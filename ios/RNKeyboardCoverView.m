#import "RNKeyboardCoverView.h"
#import <React/UIView+React.h>


#import <React/RCTShadowView.h>
#import <React/RCTTouchHandler.h>
#import <React/RCTUtils.h>


@interface RNKeyboardCoverShdowView : RCTShadowView

@end

@implementation RNKeyboardCoverShdowView

- (void)insertReactSubview:(id<RCTComponent>)subview atIndex:(NSInteger)atIndex {
    [super insertReactSubview:subview atIndex:atIndex];
    if ([subview isKindOfClass:[RCTShadowView class]]) {
        RCTShadowView *shadowView = (RCTShadowView *)subview;
        shadowView.size = RCTScreenSize();
        [shadowView setJustifyContent:YGJustifyFlexEnd];
    }
}

@end

@implementation RNKeyboardCoverView{
    RCTTouchHandler *_touchHandler;
}

#pragma mark - UIView

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
    return [RNKeyboardCoverShdowView new];
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
