#import <Foundation/Foundation.h>
#import <React/RCTBridge.h>
#import <React/RCTInvalidating.h>
#import <React/RCTView.h>

@interface RNKeyboardContentView : RCTView <RCTInvalidating>

@property (nonatomic, assign) BOOL visible;

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
