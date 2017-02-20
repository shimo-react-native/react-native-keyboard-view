#import "RNKeyboardHostView.h"

#import "RCTAssert.h"
#import "RCTTouchHandler.h"
#import "RCTUIManager.h"
#import "UIView+React.h"
#import "YYKeyboardManager.h"

#import <UIKit/UIKit.h>

@implementation RNKeyboardHostView
{
    __weak RCTBridge *_bridge;
    RCTTouchHandler *_touchHandler;
    UIView *_containerView;
    UIView *_stickyView;
    BOOL _isPresented;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _touchHandler = [[RCTTouchHandler alloc] initWithBridge:bridge];
        
        NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
        [notificationCenter addObserver:self
                                                 selector:@selector(keyboardWillShow:)
                                                     name:UIKeyboardWillShowNotification
                                                   object:nil];
        [notificationCenter addObserver:self
                               selector:@selector(keyboardDidHide:)
                                   name:UIKeyboardDidHideNotification
                                 object:nil];
        [notificationCenter addObserver:self
                               selector:@selector(keyboardWillChangeFrame:)
                                   name:UIKeyboardWillChangeFrameNotification
                                 object:nil];
        
    }
    
    return self;
}

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    [subview addGestureRecognizer:_touchHandler];
    
    if (atIndex == 0) {
        _containerView = subview;
    } else if (atIndex == 1) {
        _stickyView = subview;
    }
}

- (void)removeReactSubview:(UIView *)subview
{
    [super removeReactSubview:subview];
    [subview removeGestureRecognizer:_touchHandler];
}

- (void)didUpdateReactSubviews
{
    // Do nothing, as subview (singular) is managed by `insertReactSubview:atIndex:`
}

- (void)keyboardWillShow:(NSNotification *)notification
{
    
}

- (void)keyboardDidHide:(NSNotification *)notification
{
    [_containerView removeFromSuperview];
    [_stickyView removeFromSuperview];
    _isPresented = NO;
}

- (void)keyboardWillChangeFrame:(NSNotification *)notification
{
    YYKeyboardManager *manager = [YYKeyboardManager defaultManager];
    
    if (!_isPresented) {
        [[manager keyboardWindow] addSubview:_containerView];
        [[manager keyboardWindow] addSubview:_stickyView];
        _isPresented = YES;
    }
    
    NSDictionary *userInfo = notification.userInfo;
    NSNumber *durationValue = userInfo[UIKeyboardAnimationDurationUserInfoKey];
    NSNumber *curveValue = userInfo[UIKeyboardAnimationCurveUserInfoKey];
    
    NSValue *beginFrameValue = userInfo[UIKeyboardFrameBeginUserInfoKey];
    CGRect keyboardBeginFrame = [beginFrameValue CGRectValue];
    
    
    [UIView performWithoutAnimation:^() {
        [_bridge.uiManager setFrame:keyboardBeginFrame forView:_containerView];
        [_containerView reactSetFrame:keyboardBeginFrame];

        if (_stickyView) {
            CGRect rect = CGRectMake(
                                     keyboardBeginFrame.origin.x,
                                     keyboardBeginFrame.origin.y - _stickyView.frame.size.height,
                                     keyboardBeginFrame.size.width,
                                     _stickyView.frame.size.height
                                     );
            [_bridge.uiManager setFrame:rect forView:_stickyView];
            [_stickyView reactSetFrame:rect];
        }
        
        
        
    }];
    
    
    NSValue *endFrameValue = userInfo[UIKeyboardFrameEndUserInfoKey];
    CGRect keyboardEndFrame = [endFrameValue CGRectValue];
    
    [UIView animateWithDuration:durationValue.doubleValue
                          delay:0
                        options:(curveValue.intValue << 16)
                     animations:^() {
                         [_bridge.uiManager setFrame:keyboardEndFrame forView:_containerView];
                         [_containerView reactSetFrame:keyboardEndFrame];
                         
                         if (_stickyView) {
                             CGRect rect = CGRectMake(
                                                      keyboardEndFrame.origin.x,
                                                      keyboardEndFrame.origin.y - _stickyView.frame.size.height,
                                                      keyboardBeginFrame.size.width,
                                                      _stickyView.frame.size.height
                                                      );
                             [_bridge.uiManager setFrame:rect forView:_stickyView];
                             [_stickyView reactSetFrame:rect];
                         }
                     }
                     completion:nil];
    
}

- (void)invalidate
{
    // dismiss
}

-(void)dealloc
{
    //Removing notification observers on dealloc.
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}


@end
