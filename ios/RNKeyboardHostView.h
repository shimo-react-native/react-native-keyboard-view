#import <UIKit/UIKit.h>

#import "YYKeyboardManager.h"
#import <React/RCTBridge.h>
#import <React/RCTInvalidating.h>
#import <React/RCTView.h>

@interface RNKeyboardHostView : RCTView <RCTInvalidating, YYKeyboardObserver>

@property (nonatomic, assign) BOOL synchronouslyUpdateTransform;

/**
 hide contentView and coverView when keyboard is not visiable. default YES
 */
@property (nonatomic, assign) BOOL hideWhenKeyboardIsDismissed;

/**
 invoked when keyboard did hidden
 */
@property (nonatomic, copy) RCTDirectEventBlock onKeyboardHide;

/**
 invoked when keyboard will show
 */
@property (nonatomic, copy) RCTDirectEventBlock onKeyboardShow;

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
