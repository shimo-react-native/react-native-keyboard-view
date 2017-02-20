#import "RNKeyboardViewManager.h"
#import "RNKeyboardShadowView.h"
#import "RNKeyboardHostView.h"

@implementation RNKeyboardViewManager
{
      NSHashTable *_hostViews;
}


RCT_EXPORT_MODULE()

- (UIView *)view
{
    RNKeyboardHostView *view = [[RNKeyboardHostView alloc] initWithBridge:self.bridge];
    
    if (!_hostViews) {
        _hostViews = [NSHashTable weakObjectsHashTable];
    }
    [_hostViews addObject:view];
    
    return view;
}


//- (RCTShadowView *)shadowView
//{
//    return [[RNKeyboardShadowView new] init];
//}

- (void)invalidate
{
    for (RNKeyboardHostView *hostView in _hostViews) {
        [hostView invalidate];
    }
    [_hostViews removeAllObjects];
}

@end
