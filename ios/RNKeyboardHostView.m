#import "RNKeyboardHostView.h"

#import "RCTAssert.h"
#import "RCTTouchHandler.h"
#import "RCTUIManager.h"
#import "UIView+React.h"
#import <React/RCTUtils.h>
#import <UIKit/UIKit.h>
#import "RNKeyboardCoverView.h"
#import "RNKeyboardContentView.h"
#import "RCTUtils.h"


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
}

- (void)removeReactSubview:(__kindof UIView *)subview
{
    if ([subview class] == [RNKeyboardContentView class]) {
        _contentView = nil;
    } else if ([subview class] == [RNKeyboardCoverView class]) {
        _coverView = nil;
    }
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
    if (!self.superview || _isPresented || ![_manager isKeyboardVisible]) {
        return;
    }
    dispatch_async(RCTGetUIManagerQueue(), ^{
        [_bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
            _keyboardWindow = [_manager keyboardWindow];
            [self present];
            [self layoutContents];
            [self setAdjustedContainerFrame:NO];
        }];
        [_bridge.uiManager partialBatchDidFlush];
    });
}

- (void)synchronousTransform
{
    if (_manager.keyboardVisible && _synchronouslyUpdateTransform) {
        _keyboardWindow.transform = self.transform;
        
        _coverView.transform = self.transform;
        _contentView.transform = self.transform;
        
    }
}

- (void)present
{
    [UIView performWithoutAnimation:^() {
        [self synchronousTransform];
    }];
    
    [_keyboardWindow addSubview:_contentView];
    [self.window addSubview:_coverView];
    
    _isPresented = YES;
}

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition {
    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;
    _keyboardState = toVisible;
    
    if (!fromVisible && !toVisible) {
        return;
    }
    
    _keyboardWindow = [_manager keyboardWindow];
    
    if (!fromVisible && !_isPresented) {
        [self present];
    } else if (!toVisible) {
        _isPresented = NO;
    }
    
    [self layoutContents];

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
                         }
                     }];
}

- (void)layoutContents
{
    CGRect keyboardFrame = [_manager keyboardFrame];
    CGSize screenSize = RCTScreenSize();
    if (_contentView) {
        [self setViewSize:_contentView size:screenSize];
        [self setViewSize:_contentView.subviews.firstObject size:keyboardFrame.size];
    }
    
    if (_coverView) {
        [self setViewSize:_coverView
                     size:CGSizeMake(screenSize.width, screenSize.height - CGRectGetHeight(keyboardFrame))];
    }
}

- (void)setViewSize:(__kindof UIView*)view size:(CGSize)size {
    [_bridge.uiManager setSize:size forView:view];
}

- (void)setAdjustedContainerFrame:(BOOL)direction
{
    CGSize size = [_manager keyboardFrame].size;
    CGRect contentFrame = CGRectMake(0, direction ? size.height : 0, size.width, size.height);
    [_contentView reactSetFrame:contentFrame];
    
    CGRect coverFrame = _coverView.frame;
    coverFrame.origin = contentFrame.origin;
    [_coverView reactSetFrame:coverFrame];
}

- (void)invalidate
{
    if ([_manager isKeyboardVisible]) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [UIView performWithoutAnimation:^() {
                [_manager keyboardWindow].transform = CGAffineTransformIdentity;
                
                _contentView.transform = CGAffineTransformIdentity;
                [_contentView removeFromSuperview];
                _contentView = nil;
                
                _coverView.transform = CGAffineTransformIdentity;
                [_coverView removeFromSuperview];
                _coverView = nil;
             
            }];
        });
    }

    [_manager removeObserver:self];
    _isPresented = NO;
}

@end
