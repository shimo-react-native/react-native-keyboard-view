#import <React/RCTBridge.h>
#import <React/RCTUIManager.h>
#import "RNKeyboardViewManager.h"
#import "RNKeyboardHostView.h"
#import "RCTShadowView.h"
#import "RCTUtils.h"

@interface RNKeyboardShdowView : RCTShadowView

@end

@implementation RNKeyboardShdowView

- (void)insertReactSubview:(id<RCTComponent>)subview atIndex:(NSInteger)atIndex
{
    [super insertReactSubview:subview atIndex:atIndex];
    if ([subview isKindOfClass:[RCTShadowView class]]) {
        RCTShadowView *shadowView = (RCTShadowView *)subview;
        shadowView.size = RCTScreenSize();
        [shadowView setJustifyContent:YGJustifyFlexEnd];
    }
}

@end

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

- (RCTShadowView *)shadowView
{
    return [RNKeyboardShdowView new];
}


- (void)invalidate
{
    for (RNKeyboardHostView *hostView in _hostViews) {
        [hostView invalidate];
    }
    [_hostViews removeAllObjects];
}

RCT_EXPORT_VIEW_PROPERTY(containerHeight, CGFloat)

RCT_EXPORT_METHOD(closeKeyboard:(nonnull NSNumber *)reactTag)
{
    [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
        RNKeyboardHostView *view = viewRegistry[reactTag];
        [view closeKeyboard];
    }];
}

@end
