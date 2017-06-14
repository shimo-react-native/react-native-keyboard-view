#import "RNKeyboardCoverViewManager.h"
#import "RNKeyboardCoverView.h"

@implementation RNKeyboardCoverViewManager

RCT_EXPORT_MODULE()

- (UIView *)view
{
    return [[RNKeyboardCoverView alloc] initWithBridge:self.bridge];
}

@end
