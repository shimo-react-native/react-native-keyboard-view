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
    UIView *_stickyView;
    BOOL _isPresented;
    BOOL _isContainerVisible;
    RCTTouchHandler *_stickyGestureHandler;
    RCTTouchHandler *_containerGestureHandler;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _stickyGestureHandler = [[RCTTouchHandler alloc] initWithBridge:_bridge];
        _containerGestureHandler = [[RCTTouchHandler alloc] initWithBridge:_bridge];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                               selector:@selector(keyboardDidHide:)
                                   name:UIKeyboardDidHideNotification
                                 object:nil];
        _isContainerVisible = NO;
        [[YYKeyboardManager defaultManager] addObserver:self];
    }
    
    return self;
}

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    if (atIndex == 0) {
        _stickyView = subview;
        [_stickyView addGestureRecognizer:_stickyGestureHandler];
    } else if (atIndex == 1) {
        _containerView = subview;
        [_containerView addGestureRecognizer:_containerGestureHandler];
    }
}

- (void)removeReactSubview:(UIView *)subview
{
    if (subview == _containerView) {
        _containerView = nil;
        [subview removeGestureRecognizer:_containerView];
    } else if (subview == _stickyView) {
        _stickyView = nil;
        [subview removeGestureRecognizer:_stickyGestureHandler];
    }
    
    [super removeReactSubview:subview];
    [subview removeFromSuperview];
}

- (void)didUpdateReactSubviews
{
    // Do nothing, as subview (singular) is managed by `insertReactSubview:atIndex:`
}

- (void)keyboardDidHide:(NSNotification *)notification
{
    [_containerView removeFromSuperview];
    [_stickyView removeFromSuperview];
    _isPresented = NO;
}

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition {
    YYKeyboardManager *manager = [YYKeyboardManager defaultManager];
    UIView *keyboardWindow = [manager keyboardWindow];
    
    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;
    
    if (!fromVisible && !_isPresented) {
        _containerView.hidden = !_isContainerVisible;
        [keyboardWindow addSubview:_containerView];
        [keyboardWindow addSubview:_stickyView];
        _isPresented = YES;
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
        if (_containerView) {
            CGRect containerBeginFrame = [self calculateContainerViewFrame:fromFrame];
            [_bridge.uiManager setFrame:containerBeginFrame forView:_containerView];
            [_containerView reactSetFrame:containerBeginFrame];
        }
        
        if (_stickyView) {
            CGRect stickyBeginFrame = [self calculateStickyViewFrame:fromFrame];
            [_bridge.uiManager setFrame:stickyBeginFrame forView:_stickyView];
            [_stickyView reactSetFrame:stickyBeginFrame];
        }
    }];
    
    [UIView animateWithDuration:duration
                          delay:0
                        options:options
                     animations:^() {
                         if (_containerView) {
                             CGRect containerEndFrame = [self calculateContainerViewFrame:toFrame];
                             [_bridge.uiManager setFrame:containerEndFrame forView:_containerView];
                             [_containerView reactSetFrame:containerEndFrame];
                         }
                         
                         if (_stickyView) {
                             CGRect stickyEndFrame = [self calculateStickyViewFrame:toFrame];
                             [_bridge.uiManager setFrame:stickyEndFrame forView:_stickyView];
                             [_stickyView reactSetFrame:stickyEndFrame];
                         }
                     }
                     completion:nil];
}

- (void)openKeyboard
{
    if (!_isPresented) {
        YYKeyboardManager *manager = [YYKeyboardManager defaultManager];
        UIView *keyboardWindow = [manager keyboardWindow];
        [keyboardWindow addSubview:_containerView];
        [keyboardWindow addSubview:_stickyView];
        
        UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 20, 20)];
        [keyboardWindow addSubview:view];
        
        _isPresented = YES;
        
        
        CGSize screenSize = RCTScreenSize();
        CGSize keyboardSize = CGSizeMake(screenSize.width, _containerHeight);
        CGRect fromFrame = {
            .origin = CGPointMake(0, screenSize.height),
            .size = keyboardSize
        };
        CGRect toFrame = {
            .origin = CGPointMake(0, screenSize.height - _containerHeight),
            .size = keyboardSize
        };

        [self changeWithTransition:0.25
                           options:7
                         fromFrame:fromFrame
                           toFrame:toFrame];

    }
}

- (void)closeKeyboard
{
    [self.window endEditing:YES];
}

- (void)showKeyboard
{
    if (_isContainerVisible) {
        [self toggleKeyboard];
    }
}

- (void)hideKeyboard
{
    if (!_isContainerVisible) {
        [self toggleKeyboard];
    }
}

- (void)toggleKeyboard
{
    BOOL toggle = !_isContainerVisible;
    _isContainerVisible = toggle;
    _containerView.hidden = !toggle;
}

- (CGRect)calculateStickyViewFrame:(CGRect)keyboardFrame
{
    return CGRectMake(
                      keyboardFrame.origin.x,
                      keyboardFrame.origin.y - (_stickyViewInside ? 0.0 : _stickyView.frame.size.height),
                      keyboardFrame.size.width,
                      _stickyView.frame.size.height
                      );
}

- (CGRect)calculateContainerViewFrame:(CGRect)keyboardFrame
{
    float stickyViewOffset = _stickyViewInside ? _stickyView.frame.size.height : 0.0;
    return _stickyViewInside ? CGRectMake(
                                          keyboardFrame.origin.x,
                                          keyboardFrame.origin.y + stickyViewOffset,
                                          keyboardFrame.size.width,
                                          keyboardFrame.size.height - stickyViewOffset
                                          ) : keyboardFrame;
}

-(void)dealloc
{
    //Removing all observers on dealloc.
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [[YYKeyboardManager defaultManager] removeObserver:self];
}


@end
