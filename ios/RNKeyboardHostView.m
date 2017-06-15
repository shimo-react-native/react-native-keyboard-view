#import "RNKeyboardHostView.h"

#import <React/RCTAssert.h>
#import <React/RCTTouchHandler.h>
#import <React/RCTUIManager.h>
#import <React/UIView+React.h>
#import <React/RCTUtils.h>
#import <UIKit/UIKit.h>
#import <React/RCTUtils.h>
#import <React/RCTShadowView.h>

#import "RNKeyboardCoverView.h"
#import "RNKeyboardContentView.h"

@implementation RNKeyboardHostView
{
    __weak RCTBridge *_bridge;
    __weak RNKeyboardContentView *_contentView;
    __weak RNKeyboardCoverView *_coverView;
    __weak YYKeyboardManager *_manager;
    __weak UIWindow *_keyboardWindow;
    BOOL _hasPresented;
    BOOL _keyboardState;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _manager = [YYKeyboardManager defaultManager];
        [_manager addObserver:self];
    }
    return self;
}

- (void)setSynchronouslyUpdateTransform:(BOOL)synchronouslyUpdateTransform
{
    if (_synchronouslyUpdateTransform == synchronouslyUpdateTransform) {
        return;
    }

    if (synchronouslyUpdateTransform) {
        [self synchronousTransform];
    }
    _synchronouslyUpdateTransform = synchronouslyUpdateTransform;
}

- (void)insertReactSubview:(__kindof UIView *)subview atIndex:(NSInteger)atIndex
{
    if ([subview class] == [RNKeyboardContentView class]) {
        RCTAssert(_contentView == nil, @"KeyboardView ContainerView is already existed.");
        _contentView = subview;
    } else if ([subview class] == [RNKeyboardCoverView class]) {
        RCTAssert(_coverView == nil, @"KeyboardView StickyView is already existed.");
        _coverView = subview;
    }
    if (_hasPresented) {
        [self refreshLayout];
    }
    [super insertReactSubview:subview atIndex:atIndex];
}

- (void)removeReactSubview:(__kindof UIView *)subview
{
    if ([subview class] == [RNKeyboardContentView class]) {
        [_contentView removeFromSuperview];
        _contentView = nil;
    } else if ([subview class] == [RNKeyboardCoverView class]) {
        [_coverView removeFromSuperview];
        _coverView = nil;
    }

    if (!_contentView && !_coverView) {
        [self destroy];
    }

    [super removeReactSubview:subview];
}

- (void)didUpdateReactSubviews
{
    // Do nothing, as subviews are managed by `insertReactSubview:atIndex:`
}

- (void)didSetProps:(NSArray<NSString *> *)changedProps
{
    if ([changedProps containsObject:@"transform"]) {
        [self synchronousTransform];
    }
}

- (void)didMoveToSuperview
{
    if (!self.superview && _hasPresented) {
        [self destroy];
    }if (self.superview && !_hasPresented && [_manager isKeyboardVisible]) {
        [self refreshLayout];
    }
}

- (void)refreshLayout
{
    dispatch_async(RCTGetUIManagerQueue(), ^{
        [_bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
            _keyboardWindow = [_manager keyboardWindow];
            [self present];
            [self layoutContents];
            [self setAdjustedContainerFrame:NO];
        }];
    });
}

- (void)synchronousTransform
{
    if (_manager.keyboardVisible && _synchronouslyUpdateTransform) {
        _keyboardWindow.transform = self.transform;
        _coverView.transform = self.transform;
    }
}

- (void)present
{
    [UIView performWithoutAnimation:^() {
        [self synchronousTransform];
    }];

    [_keyboardWindow addSubview:_contentView];
    [self.window addSubview:_coverView];

    _hasPresented = YES;
}

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition
{

    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;
    _keyboardState = toVisible;

    if ((!fromVisible && !toVisible) || (!_coverView && !_contentView)) {
        return;
    }

    _keyboardWindow = [_manager keyboardWindow];
    _keyboardWindow.transform = self.transform;

    if (!fromVisible && !_hasPresented) {
        [self present];
    }

    if (toVisible) {
        [self layoutContents];
    }

    if (!fromVisible) {
        [UIView performWithoutAnimation:^() {
            [self setAdjustedContainerFrame:YES];
        }];
    }

    [UIView animateWithDuration:transition.animationDuration
                          delay:0
                        options:transition.animationOption
                     animations:^() {
                         if (!fromVisible) {
                             [self setAdjustedContainerFrame:NO];
                         } else if (!toVisible) {
                             [self setAdjustedContainerFrame:YES];
                         }
                     }
                     completion:^(BOOL finished) {
                         if (finished && !toVisible && !_keyboardState) {
                             [_contentView removeFromSuperview];
                             [_coverView removeFromSuperview];
                             _hasPresented = NO;
                         }
                     }];
}

- (void)layoutContents
{
    CGRect keyboardFrame = [_manager keyboardFrame];
    CGSize screenSize = RCTScreenSize();
    float coverHeight = screenSize.height - CGRectGetHeight(keyboardFrame);

    dispatch_async(RCTGetUIManagerQueue(), ^{
        RCTShadowView *_contentShadowView = [self getShadowView:_contentView];
        YGValue top = { .value = coverHeight, .unit = YGUnitPoint };
        _contentShadowView.top = top;
        _contentShadowView.size = keyboardFrame.size;

        RCTShadowView *_coverShadowView = [self getShadowView:_coverView];
        _coverShadowView.size = CGSizeMake(screenSize.width, coverHeight);

        [_bridge.uiManager setNeedsLayout];
    });

}

- (RCTShadowView *)getShadowView:(UIView *)view
{
    NSMutableDictionary<NSNumber *, RCTShadowView *> *shadowViewRegistry = [_bridge.uiManager valueForKey:@"shadowViewRegistry"];
    NSNumber *reactTag = view.reactTag;
    return shadowViewRegistry[reactTag];
}

- (void)setAdjustedContainerFrame:(BOOL)direction
{
    CGSize size = [_manager keyboardFrame].size;

    CGRect contentFrame = _contentView.frame;
    contentFrame.origin.y += direction ? size.height : 0;
    [_contentView reactSetFrame:contentFrame];

    CGRect coverFrame = _coverView.frame;
    coverFrame.origin.y = direction ? size.height : 0;
    [_coverView reactSetFrame:coverFrame];
}

- (void)invalidate
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIView performWithoutAnimation:^() {
            _keyboardWindow.transform = CGAffineTransformIdentity;
        }];
        [_contentView removeFromSuperview];
        _contentView = nil;
        [_coverView removeFromSuperview];
        _coverView = nil;
    });

    [self destroy];
}

- (void)destroy
{
    _hasPresented = NO;
    [_manager removeObserver:self];
}

@end
