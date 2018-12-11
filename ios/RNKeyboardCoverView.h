#import <React/RCTView.h>
#import <React/RCTBridge.h>
#import <React/RCTInvalidating.h>
#import <React/RCTView.h>

@interface RNKeyboardCoverView : RCTView <RCTInvalidating>

@property (nonatomic, assign) BOOL visible;

- (instancetype)initWithBridge:(RCTBridge *)bridge NS_DESIGNATED_INITIALIZER;

@end
