#import "RNKeyboardShadowView.h"
#import "RCTUtils.h"

@implementation RNKeyboardShadowView
{
    id<RCTComponent> _containerShadowView;
    id<RCTComponent> _stickyShadowView;
}

- (void)setFrame:(CGRect)frame
{
    [(RCTShadowView *)_containerShadowView setFrame:frame];
    [(RCTShadowView *)_stickyShadowView setFrame:CGRectMake(
                                                            frame.origin.x,
                                                            frame.origin.y - frame.size.height - 40,
                                                            20,
                                                            20)];
}

- (void)insertReactSubview:(id<RCTComponent>)subview atIndex:(NSInteger)atIndex
{
    [super insertReactSubview:subview atIndex:atIndex];
    if (atIndex == 0) {
        _containerShadowView = subview;
    } else if (atIndex == 1) {
        _stickyShadowView = subview;
    }
}

- (void)removeReactSubview:(RCTShadowView *)subview
{
    
}

@end
