#import "RNKeyboardContentView.h"
#import <React/RCTShadowView.h>
#import <React/RCTTouchHandler.h>
#import <React/RCTUtils.h>

@interface RNKeyboardShdowView : RCTShadowView

@end

@implementation RNKeyboardShdowView

- (void)insertReactSubview:(id<RCTComponent>)subview atIndex:(NSInteger)atIndex {
    [super insertReactSubview:subview atIndex:atIndex];
    if ([subview isKindOfClass:[RCTShadowView class]]) {
        RCTShadowView *shadowView = (RCTShadowView *)subview;
        shadowView.size = RCTScreenSize();
        [shadowView setJustifyContent:YGJustifyFlexEnd];
    }
}

@end

@implementation RNKeyboardContentView {
    RCTTouchHandler *_touchHandler;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge {
    if ((self = [super initWithFrame:CGRectZero])) {
        _touchHandler = [[RCTTouchHandler alloc] initWithBridge:bridge];
        [_touchHandler attachToView:self];
    }
    return self;
}

- (RCTShadowView *)shadowView {
    return [RNKeyboardShdowView new];
}

- (void)invalidate {
    [_touchHandler detachFromView:self];
}

@end
