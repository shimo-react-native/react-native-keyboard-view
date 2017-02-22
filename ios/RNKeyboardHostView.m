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
    UIView *_containerView;
    BOOL _isPresented;
    RCTTouchHandler *_touchHandler;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _touchHandler = [[RCTTouchHandler alloc] initWithBridge:_bridge];
    }

    return self;
}


- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    RCTAssert(_containerView == nil, @"Keyboard view can only have one subview");
    [super insertReactSubview:subview atIndex:atIndex];
    [subview addGestureRecognizer:_touchHandler];
    _containerView = subview;

    [[YYKeyboardManager defaultManager] addObserver:self];
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

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition {
    YYKeyboardManager *manager = [YYKeyboardManager defaultManager];
    UIView *keyboardWindow = [manager keyboardWindow];

    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;

    if (!fromVisible && !_isPresented) {
        [keyboardWindow addSubview:_containerView];
        _isPresented = YES;
    } else if (!toVisible) {
        _isPresented = NO;
    }

    CGRect fromFrame = [manager convertRect:transition.fromFrame toView:nil];
    CGRect toFrame =  [manager convertRect:transition.toFrame toView:nil];

    NSTimeInterval animationDuration = transition.animationDuration;
    UIViewAnimationCurve curve = transition.animationCurve;

    [self changeWithTransition:transition.animationDuration
                       options:transition.animationCurve
                     fromFrame:fromFrame
                       toFrame:toFrame];
}

- (void)changeWithTransition:(NSTimeInterval)duration
                     options:(UIViewAnimationOptions)options
                   fromFrame:(CGRect)fromFrame
                     toFrame:(CGRect)toFrame
{
    [UIView performWithoutAnimation:^() {
        CGRect beginFrame = [self calculateContainerFrame:fromFrame];
        [self setFrameInSafeThread:beginFrame];
        [_containerView reactSetFrame:beginFrame];
    }];

    [UIView animateWithDuration:duration
                          delay:0
                        options:options
                     animations:^() {
                        CGRect endFrame = [self calculateContainerFrame:toFrame];
                         [self setFrameInSafeThread:endFrame];
                         [_containerView reactSetFrame:endFrame];
                     }
                     completion:nil];
}

// Ensure _containerView is not unmounted before setFrame.
// TODO: Check whether _containerView is unmounted or not in a more proper way
-(void)setFrameInSafeThread:(CGRect)frame
{
    [_bridge.uiManager rootViewForReactTag:_containerView.reactTag withCompletion:^(UIView *view) {
        if (view) {
            [_bridge.uiManager setFrame:frame forView:_containerView];
        }
    }];
}

-(CGRect)calculateContainerFrame:(CGRect)frame
{
    if (_stickyViewInside) {
        return frame;
    } else {
        // Assume the first subview is the sticky view,
        // and assign its height as offset.
        float offset = _containerView.subviews[0].frame.size.height;
        CGRect calculatedFrame = {
            .origin = CGPointMake(frame.origin.x, frame.origin.y - offset),
            .size = CGSizeMake(frame.size.width, frame.size.height + offset)
        };
        return calculatedFrame;
    }

}

-(void)invalidate
{
    //Removing all observers.
    [[YYKeyboardManager defaultManager] removeObserver:self];
    _isPresented = NO;
}

-(void)openKeyboard
{

}

-(void)closeKeyboard
{
    [self.window endEditing:YES];
}

@end
