#import <UIKit/UIKit.h>

#import "YYKeyboardManager.h"
#import <React/RCTInvalidating.h>
#import <React/RCTView.h>
#import <React/RCTBridge.h>

@interface RNKeyboardHostView : RCTView <RCTInvalidating, YYKeyboardObserver>

@property (nonatomic, assign) BOOL synchronouslyUpdateTransform;

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
