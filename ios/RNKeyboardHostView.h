#import <UIKit/UIKit.h>

#import <React/RCTInvalidating.h>
#import <React/RCTView.h>
#import <React/RCTBridge.h>

@interface RNKeyboardHostView : UIView <RCTInvalidating>

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
