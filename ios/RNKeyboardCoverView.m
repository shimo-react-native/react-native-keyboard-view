#import "RNKeyboardCoverView.h"
#import <React/UIView+React.h>

@implementation RNKeyboardCoverView {
    BOOL _isCoverHidden;
}

#pragma mark - UIView

- (void)setHidden:(BOOL)hidden {
    if (_isCoverHidden == hidden) {
        [super setHidden:hidden];
    }
}

#pragma mark - Setter

- (void)setCoverHidden:(BOOL)hidden {
    _isCoverHidden = hidden;
    [self setHidden:hidden];
}


@end
