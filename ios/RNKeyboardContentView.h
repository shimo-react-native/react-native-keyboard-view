#import <Foundation/Foundation.h>
#import <React/RCTBridge.h>
#import <React/RCTInvalidating.h>
#import <React/RCTView.h>
#import "RNKeyboardCoverView.h"

@interface RNKeyboardContentView : RNKeyboardCoverView <RCTInvalidating>

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
