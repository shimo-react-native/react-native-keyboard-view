#import <React/RCTBridge.h>
#import <React/RCTUIManager.h>
#import "RNKeyboardViewManager.h"
#import "RNKeyboardHostView.h"
#import "RCTShadowView.h"
#import "RCTUtils.h"
#import "YYKeyboardManager.h"

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

RCT_EXPORT_VIEW_PROPERTY(synchronouslyUpdateTransform, BOOL)

RCT_EXPORT_METHOD(dismiss)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication].keyWindow endEditing:YES];
    });
}


RCT_EXPORT_METHOD(dismissWithoutAnimation)
{
    if ([YYKeyboardManager defaultManager].isKeyboardVisible) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [UIView animateWithDuration:0 animations:^{
                [[UIApplication sharedApplication].keyWindow endEditing:YES];
            }];
        });
    }
}

@end
