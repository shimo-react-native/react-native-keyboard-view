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
    BOOL _keyboardState;
    BOOL _isPresented;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _manager = [YYKeyboardManager defaultManager];
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
    
    [super insertReactSubview:subview atIndex:atIndex];
    
    if (_contentView && _coverView) {
        [_manager addObserver:self];
        
        if (!_isPresented && [_manager isKeyboardVisible]) {
            [self presendAndLayoutContents];
        }
        
    }
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
    if (!self.superview && _isPresented) {
        [_manager removeObserver:self];
        _isPresented = NO;
    }
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
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIView performWithoutAnimation:^() {
            [self synchronousTransform];
        }];
        
        [_keyboardWindow addSubview:_contentView];
        [self.window addSubview:_coverView];
    });
}

- (void)presendAndLayoutContents
{
    dispatch_async(RCTGetUIManagerQueue(), ^{
        _keyboardWindow = [_manager keyboardWindow];
        [self present];
        [self layoutContents];
        [_bridge.uiManager setNeedsLayout];
    });
}

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition
{

    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;
    
    _keyboardState = toVisible;

    if ((!fromVisible && !toVisible) ||(!_contentView || !_coverView)) {
        return;
    }

    _keyboardWindow = [_manager keyboardWindow];
    _keyboardWindow.transform = self.transform;
    if (!fromVisible && !_isPresented) {
        [self present];
    }

    if (toVisible) {
        [self layoutContents];
    }

    if (!fromVisible && !_isPresented) {
        [UIView performWithoutAnimation:^() {
            [self setAdjustedContainerFrame:YES];
        }];
        _isPresented = YES;
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
                             _isPresented = NO;
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
        _contentShadowView.size = screenSize;
        // _contentShadowView.yogaNode must not be null
        if (_contentShadowView.yogaNode) {
            YGNodeStyleSetPadding(_contentShadowView.yogaNode, YGEdgeTop, coverHeight);
        }
        
        RCTShadowView *_coverShadowView = [self getShadowView:_coverView];
        _coverShadowView.size = CGSizeMake(screenSize.width, coverHeight);
        [_bridge.uiManager setNeedsLayout];
    });
}

- (void)setAdjustedContainerFrame:(BOOL)direction
{
    float keyboardHeight = CGRectGetHeight([_manager keyboardFrame]);
    float offset = direction ? keyboardHeight : 0;
    
    CGRect contentFrame = _contentView.frame;
    contentFrame.origin.y = offset;
    [_contentView reactSetFrame:contentFrame];
    
    CGRect coverFrame = _coverView.frame;
    coverFrame.origin.y = offset;
    [_coverView reactSetFrame:coverFrame];
}

- (RCTShadowView *)getShadowView:(UIView *)view
{
    NSMutableDictionary<NSNumber *, RCTShadowView *> *shadowViewRegistry = [_bridge.uiManager valueForKey:@"shadowViewRegistry"];
    NSNumber *reactTag = view.reactTag;
    return shadowViewRegistry[reactTag];
}

- (void)invalidate
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIView performWithoutAnimation:^() {
            _keyboardWindow.transform = CGAffineTransformIdentity;
        }];
        [_contentView removeFromSuperview];
        [_coverView removeFromSuperview];
        _isPresented = NO;
        [_manager removeObserver:self];
    });
}

@end
