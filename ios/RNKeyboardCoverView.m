#import "RNKeyboardCoverView.h"
#import <React/UIView+React.h>

@implementation RNKeyboardCoverView

#pragma mark - UIView

- (void)setHidden:(BOOL)hidden {
    // do nothing, let `setCoverHidden` do the stuff
}

#pragma mark - Setter

- (void)setCoverHidden:(BOOL)coverHidden {
    _coverHidden = coverHidden;
    [super setHidden:coverHidden];
}


@end
