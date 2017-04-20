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
    __weak UIView *_contentView;
    __weak UIView *_stickyView;
    __weak YYKeyboardManager *_manager;
    __weak UIWindow *_keyboardWindow;
    BOOL _isPresented;
    RCTTouchHandler *_containerTouchHandler;
    RCTTouchHandler *_stickyViewTouchHandler;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _containerTouchHandler = [[RCTTouchHandler alloc] initWithBridge:_bridge];
        _stickyViewTouchHandler = [[RCTTouchHandler alloc] initWithBridge:_bridge];
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

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    if (atIndex == 0) {
        RCTAssert(_contentView == nil, @"KeyboardView ContainerView is already existed.");
        [_containerTouchHandler attachToView:subview];
        _contentView = subview;
    } else if (atIndex == 1) {
        RCTAssert(_stickyView == nil, @"KeyboardView StickyView is already existed.");
        [_stickyViewTouchHandler attachToView:subview];
        _stickyView = subview;
    }
}

- (void)removeReactSubview:(UIView *)subview
{
    if (subview == _contentView) {
        [_containerTouchHandler detachFromView:subview];
        _contentView = nil;
    } else if (subview == _stickyView) {
        [_stickyViewTouchHandler detachFromView:subview];
        _stickyView = nil;
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

- (void)synchronousTransform
{
    if (_manager.keyboardVisible && _synchronouslyUpdateTransform) {
        _keyboardWindow.transform = _stickyView.transform = self.transform;
    }
}

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition {
    if (!_contentView || !_stickyView) {
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
        [_keyboardWindow addSubview:_contentView];
        [self.window addSubview:_stickyView];
        _isPresented = YES;
    } else if (!toVisible) {
        _isPresented = NO;
    }

    // Set content height.
    if (toVisible) {
        [_bridge.uiManager setSize:toFrame.size forView:_contentView.subviews.firstObject];
        [_bridge.uiManager setSize:toFrame.size forView:_stickyView.subviews.lastObject];
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
                             [_contentView removeFromSuperview];
                             [_stickyView removeFromSuperview];
                         }
                     }];
}

- (CGFloat)getStickyViewHeight
{
    return CGRectGetHeight(_stickyView.subviews[1].bounds);
}

- (void)setAdjustedContainerFrame:(CGRect)frame direction:(BOOL)direction
{
    frame.origin.y = direction ? frame.size.height : 0;
    [_contentView reactSetFrame:frame];
    [_stickyView reactSetFrame:frame];
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
                [[UIApplication sharedApplication].keyWindow endEditing:YES];
                [_manager keyboardWindow].transform = _stickyView.transform = CGAffineTransformIdentity;

                [_contentView removeFromSuperview];
                [_containerTouchHandler detachFromView:_contentView];
                _contentView = nil;

                [_stickyView removeFromSuperview];
                [_stickyViewTouchHandler detachFromView:_stickyView];
                _stickyView = nil;
            }];
        });
    }

    [_manager removeObserver:self];
    _isPresented = NO;
}

@end
