#import <UIKit/UIKit.h>

#import "YYKeyboardManager.h"
#import <React/RCTBridge.h>
#import <React/RCTInvalidating.h>
#import <React/RCTView.h>
#import <React/RCTEventEmitter.h>

FOUNDATION_EXTERN NSString * const RNKeyboardInHardwareKeyboardModeNotification;

@interface RNKeyboardHostView : RCTView <RCTInvalidating, YYKeyboardObserver>

@property (nonatomic, assign) BOOL synchronouslyUpdateTransform;

/**
 hide contentView and coverView when keyboard is not visiable. default YES
 */
@property (nonatomic, assign) BOOL hideWhenKeyboardIsDismissed;

/**
 invoked when keyboard or contentView did hidden
 */
@property (nonatomic, copy) RCTDirectEventBlock onKeyboardHide;

/**
 invoked when keyboard or contentView will show
 */
@property (nonatomic, copy) RCTDirectEventBlock onKeyboardShow;

/**
 contentView min height.
 
 if keyboardPlaceholderHeight > 0 && _contentView != nil,
    if keyboard is visible, add contentVie to keyboardWindow, contentViewHieght = MAX(keyboardPlaceholderHeight, keyboardVisibleHeight).
    if keyboard is not visible, add contentVie to reactRootView, contentViewHieght = keyboardPlaceholderHeight.
 */
@property (nonatomic, assign) CGFloat keyboardPlaceholderHeight;

#pragma mark - prop - readonly

/**
 whether keyboard or contentView is shown
 */
@property (nonatomic, assign, readonly) BOOL contentOrKeyboardShown;

@property (nonatomic, assign, readonly) BOOL inHardwareKeyboardMode;

#pragma mark - method

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
