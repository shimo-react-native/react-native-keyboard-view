#import "RNKeyboardContentView.h"
#import <React/RCTShadowView.h>
#import <React/RCTTouchHandler.h>
#import <React/RCTUtils.h>

@interface RNKeyboardContentShadowView : RCTShadowView

@end

@implementation RNKeyboardContentShadowView

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

RCT_NOT_IMPLEMENTED(-(instancetype)initWithFrame
                    : (CGRect)frame)
RCT_NOT_IMPLEMENTED(-(instancetype)initWithCoder
                    : coder)

- (instancetype)initWithBridge:(RCTBridge *)bridge {
    if ((self = [super initWithFrame:CGRectZero])) {
        _visible = YES;
        _touchHandler = [[RCTTouchHandler alloc] initWithBridge:bridge];
        [_touchHandler attachToView:self];
    }
    return self;
}

- (RCTShadowView *)shadowView {
    return [RNKeyboardContentShadowView new];
}

#pragma mark - RCTInvalidating

- (void)invalidate {
    [_touchHandler detachFromView:self];
    _touchHandler = nil;
}

#pragma mark - Setter

- (void)setHidden:(BOOL)hidden {
    // do nothing, let `setCoverHidden` do the stuff
}

- (void)setVisible:(BOOL)visible {
    _visible = visible;
    [super setHidden:!visible];
}

@end
