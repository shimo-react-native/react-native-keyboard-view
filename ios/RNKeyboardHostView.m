#import "RNKeyboardHostView.h"

#import "RCTAssert.h"
#import "RCTTouchHandler.h"
#import "RCTUIManager.h"
#import "UIView+React.h"
#import <React/RCTUtils.h>
#import <UIKit/UIKit.h>

@implementation RNKeyboardHostView
{
    __weak RCTBridge *_bridge;
    __weak UIView *_containerView;
    __weak YYKeyboardManager *_manager;
    __weak UIWindow *_keyboardWindow;
    BOOL _isPresented;
    RCTTouchHandler *_touchHandler;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _touchHandler = [[RCTTouchHandler alloc] initWithBridge:_bridge];
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

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    RCTAssert(_containerView == nil, @"Keyboard view can only have one subview");
    [super insertReactSubview:subview atIndex:atIndex];
    [subview addGestureRecognizer:_touchHandler];
    _containerView = subview;
    [_manager addObserver:self];
}

- (void)removeReactSubview:(UIView *)subview
{
    RCTAssert(subview == _containerView, @"Cannot remove view other than keyboard view");
    [super removeReactSubview:subview];
    [subview removeGestureRecognizer:_touchHandler];
    _containerView = nil;
}

- (void)didUpdateReactSubviews
{
    // Do nothing, as subview (singular) is managed by `insertReactSubview:atIndex:`
}

- (void)didSetProps:(NSArray<NSString *> *)changedProps
{
    if ([changedProps containsObject:@"transform"]) {
        [self synchronousTransform];
    }
}

- (void)synchronousTransform
{
    if (_manager.keyboardVisible && _synchronouslyUpdateTransform) {
        _keyboardWindow.transform = self.transform;
    }
}

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition {
    if (!_containerView.subviews.count) {
        return;
    }

    _keyboardWindow = [_manager keyboardWindow];
    UIView *keyboardView = [_manager keyboardView];
    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;
    CGRect toFrame = [_manager convertRect:transition.toFrame toView:nil];


    if (!fromVisible && !toVisible) {
        return;
    }

    if (!fromVisible && !_isPresented) {
        [UIView performWithoutAnimation:^() {
            [self synchronousTransform];
        }];
        [_keyboardWindow addSubview:_containerView];
        _isPresented = YES;
    } else if (!toVisible) {
        _isPresented = NO;
    }

    // Set content height.
    if (toVisible) {
        [_bridge.uiManager setSize:toFrame.size forView:_containerView.subviews.lastObject];
    }

    toFrame.size.height += [self getStickyViewHeight];

    [UIView performWithoutAnimation:^() {
        if (!fromVisible) {
            [self setAdjustedKeyboardFrame:keyboardView.frame direction:YES];
            [self setAdjustedContainerFrame:toFrame direction:YES];
        }
    }];

    [UIView animateWithDuration:transition.animationDuration
                          delay:0
                        options:transition.animationCurve
                     animations:^() {
                         if (!fromVisible) {
                             [self setAdjustedContainerFrame:toFrame direction:NO];
                             [self setAdjustedKeyboardFrame:keyboardView.frame direction:NO];
                         } else if (!toVisible) {
                             [self setAdjustedContainerFrame:toFrame direction:YES];
                             [self setAdjustedKeyboardFrame:keyboardView.frame direction:YES];
                         }
                     }
                     completion:^(BOOL finished) {
                         if (finished && !toVisible) {
                             [_containerView removeFromSuperview];
                         }
                     }];
}

- (CGFloat)getStickyViewHeight
{
    return CGRectGetHeight(_containerView.subviews[1].bounds);
}

- (void)setAdjustedContainerFrame:(CGRect)frame direction:(BOOL)direction
{
    frame.origin.y = direction ? frame.size.height : 0;
    [_containerView reactSetFrame:frame];
}

- (void)setAdjustedKeyboardFrame:(CGRect)frame direction:(BOOL)direction
{
    CGFloat offset = [self getStickyViewHeight];
    UIView *keyboardView = [_manager keyboardView];

    if (offset) {
        frame.origin.y = frame.origin.y + (direction ? offset : -offset);
    }

    [keyboardView setFrame:frame];

}

-(void)invalidate
{
    if ([_manager isKeyboardVisible]) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [UIView performWithoutAnimation:^() {
                [self closeKeyboard];
                [_manager keyboardWindow].transform = CGAffineTransformIdentity;
                [_containerView removeFromSuperview];
            }];
        });
    }

    [_manager removeObserver:self];
    _isPresented = NO;
}

-(void)closeKeyboard
{
    [[UIApplication sharedApplication].keyWindow endEditing:YES];
}

@end
