#import "RNKeyboardViewManager.h"
#import "RNKeyboardHostView.h"
#import "YYKeyboardManager.h"
#import <React/RCTBridge.h>
#import <React/RCTShadowView.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>

@implementation RNKeyboardViewManager {
    NSHashTable *_hostViews;
    RNKeyboardHostView *_keyboardHostView;
}

RCT_EXPORT_MODULE()

- (UIView *)view {
    _keyboardHostView = [[RNKeyboardHostView alloc] initWithBridge:self.bridge];

    if (!_hostViews) {
        _hostViews = [NSHashTable weakObjectsHashTable];
    }
    [_hostViews addObject:_keyboardHostView];

    return _keyboardHostView;
}

RCT_EXPORT_VIEW_PROPERTY(synchronouslyUpdateTransform, BOOL)
RCT_EXPORT_VIEW_PROPERTY(hideWhenKeyboardIsDismissed, BOOL)
RCT_EXPORT_VIEW_PROPERTY(contentVisible, BOOL)
RCT_EXPORT_VIEW_PROPERTY(keyboardPlaceholderHeight, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(onKeyboardHide, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onKeyboardShow, RCTDirectEventBlock)

RCT_EXPORT_METHOD(dismiss) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIApplication sharedApplication].keyWindow endEditing:YES];
    });
}

RCT_EXPORT_METHOD(dismissWithoutAnimation) {
    if ([YYKeyboardManager defaultManager].isKeyboardVisible) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [UIView performWithoutAnimation:^{
                [[UIApplication sharedApplication].keyWindow endEditing:YES];
            }];
        });
    }
}

RCT_REMAP_METHOD(getInHardwareKeyboardMode,
                 getInHardwareKeyboardModeWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@(_keyboardHostView.inHardwareKeyboardMode));
}

@end
