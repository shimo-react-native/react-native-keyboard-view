#import "RNKeyboardCoverView.h"
#import <React/UIView+React.h>

@implementation RNKeyboardCoverView

#pragma mark - UIView

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        _visible = YES;
    }
    return self;
}

- (void)setHidden:(BOOL)hidden {
    // do nothing, let `setCoverHidden` do the stuff
}

#pragma mark - Setter

- (void)setVisible:(BOOL)visible {
    _visible = visible;
    [super setHidden:!visible];
}

@end
