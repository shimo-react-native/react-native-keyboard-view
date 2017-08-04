#import "RNKeyboardContentViewManager.h"
#import "RNKeyboardContentView.h"

@implementation RNKeyboardContentViewManager

RCT_EXPORT_MODULE()

- (UIView *)view {
    return [[RNKeyboardContentView alloc] initWithBridge:self.bridge];
}

@end
