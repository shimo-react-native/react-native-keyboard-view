

#import "RNKeyboardHostView.h"

#import <React/RCTAssert.h>
#import <React/RCTRootView.h>
#import <React/RCTShadowView.h>
#import <React/RCTTouchHandler.h>
#import <React/RCTUIManager.h>
#import <React/RCTUtils.h>
#import <React/UIView+React.h>
#import <UIKit/UIKit.h>

#import "RNKeyboardContentView.h"
#import "RNKeyboardCoverView.h"

@interface RNKeyboardHostView ()

@property (nonatomic, weak) UIView *rootView;

@end

@implementation RNKeyboardHostView {
    __weak RCTBridge *_bridge;
    __weak RNKeyboardContentView *_contentView;
    __weak RNKeyboardCoverView *_coverView;
    __weak YYKeyboardManager *_manager;
    BOOL _keyboardState;
    BOOL _isPresented;
    BOOL attachedToSuper;
}

- (instancetype)initWithBridge:(RCTBridge *)bridge {
    if ((self = [super initWithFrame:CGRectZero])) {
        _bridge = bridge;
        _hideWhenKeyboardIsDismissed = YES;
        _manager = [YYKeyboardManager defaultManager];
        [_manager addObserver:self];
    }
    return self;
}

#pragma mark - UIView

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (!self.superview && _isPresented) {
        _isPresented = NO;
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (!_coverView.superview && self.window) {
        [self autoAddSubview:_coverView onSuperview:self.rootView];
        
        if (_hideWhenKeyboardIsDismissed && !(_hideWhenKeyboardIsDismissed && !_isPresented && [_manager isKeyboardVisible])) {
            [_coverView setCoverHidden:YES];
        }
    }
    
    if (!_contentView.superview && self.window) {
        [self autoAddSubview:_contentView onSuperview:[_manager keyboardWindow]];
    }
}

#pragma mark - React

- (void)insertReactSubview:(__kindof UIView *)subview atIndex:(NSInteger)atIndex {
    if ([subview class] == [RNKeyboardContentView class]) {
        RCTAssert(_contentView == nil, @"KeyboardView ContainerView is already existed.");
        _contentView = subview;
        [self autoAddSubview:_contentView onSuperview:[_manager keyboardWindow]];
    } else if ([subview class] == [RNKeyboardCoverView class]) {
        RCTAssert(_coverView == nil, @"KeyboardView StickyView is already existed.");
        _coverView = subview;
        
        [self autoAddSubview:_coverView onSuperview:self.rootView];
        if (_hideWhenKeyboardIsDismissed && !(_hideWhenKeyboardIsDismissed && !_isPresented && [_manager isKeyboardVisible])) {
            [_coverView setCoverHidden:YES];
        }
    }

    
    [super insertReactSubview:subview atIndex:atIndex];
    [self layoutContents];
}

- (void)removeReactSubview:(__kindof UIView *)subview {
    if ([subview class] == [RNKeyboardContentView class]) {
        [_contentView removeFromSuperview];
        _contentView = nil;
    } else if ([subview class] == [RNKeyboardCoverView class]) {
        [_coverView removeFromSuperview];
        _coverView = nil;
    }

    [super removeReactSubview:subview];
}

- (void)didUpdateReactSubviews {
    // Do nothing, as subviews are managed by `insertReactSubview:atIndex:`
}

- (void)didSetProps:(NSArray<NSString *> *)changedProps {
    if ([changedProps containsObject:@"transform"]) {
        [self synchronousTransform];
    }
}

#pragma mark - RCTInvalidating

- (void)invalidate {
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIView performWithoutAnimation:^() {
            [_manager keyboardWindow].transform = CGAffineTransformIdentity;
        }];
        [_contentView removeFromSuperview];
        _contentView = nil;
        [_coverView removeFromSuperview];
        _coverView = nil;
        _isPresented = NO;
        [_manager removeObserver:self];
    });
}

#pragma mark - YYKeyboardObserver

- (void)keyboardChangedWithTransition:(YYKeyboardTransition)transition {
    BOOL fromVisible = transition.fromVisible;
    BOOL toVisible = transition.toVisible;

    if (fromVisible != toVisible && toVisible) {
        if (_onKeyboardShow) {
            _onKeyboardShow(nil);
        }
    }

    _keyboardState = toVisible;
    
    if ((!fromVisible && !toVisible) || (!_contentView && !_coverView)) {
        return;
    }

    if (!fromVisible && !_isPresented) {
        [self synchronousTransform];
    }

    if (toVisible) {
        [self layoutContents];
        [_coverView setCoverHidden:NO];
        [UIView performWithoutAnimation:^() {
            [_coverView setAlpha:1];
        }];
    }

    if (!fromVisible && !_isPresented) {
        [UIView performWithoutAnimation:^() {
            [self setAdjustedContainerFrame:YES];
        }];
        _isPresented = YES;
    }

    [UIView animateWithDuration:transition.animationDuration
        delay:0
        options:transition.animationOption
        animations:^() {
            if (!fromVisible) {
                [self setAdjustedContainerFrame:NO];
            } else if (!toVisible) {
                [self setAdjustedContainerFrame:YES];
            }
            if (!toVisible && _hideWhenKeyboardIsDismissed) {
                [_coverView setAlpha:0];
            }
        }
        completion:^(BOOL finished) {
            if (finished && !toVisible && !_keyboardState) { // keyboard is not visible
                _isPresented = NO;
                if (_hideWhenKeyboardIsDismissed) {
                    [_coverView setCoverHidden:YES];
                    [_coverView setAlpha:1];
                    if (_onKeyboardHide) {
                        _onKeyboardHide(nil);
                    }
                }
            }
        }];
}

#pragma mark - Layout

- (void)layoutContents {
    CGRect keyboardFrame = [_manager keyboardFrame];
    CGSize screenSize = RCTScreenSize();
    float coverHeight = screenSize.height - CGRectGetHeight(keyboardFrame);
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self synchronousTransform];
    });
    
    dispatch_async(RCTGetUIManagerQueue(), ^{
        if (_contentView) {
            RCTShadowView *_contentShadowView = [self getShadowView:_contentView];
            _contentShadowView.size = screenSize;
            // _contentShadowView.yogaNode must not be null
            if (_contentShadowView.yogaNode) {
                YGNodeStyleSetPadding(_contentShadowView.yogaNode, YGEdgeTop, coverHeight);
            }
        }
        if (_coverView) {
            RCTShadowView *_coverShadowView = [self getShadowView:_coverView];
            _coverShadowView.size = CGSizeMake(screenSize.width, coverHeight);
        }
        [_bridge.uiManager setNeedsLayout];
    });
}

#pragma mark - Private

- (void)synchronousTransform {
    [UIView performWithoutAnimation:^() {
        if (_manager.keyboardVisible && _synchronouslyUpdateTransform) {
            [_manager keyboardWindow].transform = self.transform;
            _coverView.transform = self.transform;
        }
    }];
}

- (void)setAdjustedContainerFrame:(BOOL)direction {
    float keyboardHeight = CGRectGetHeight([_manager keyboardFrame]);
    float offset = direction ? keyboardHeight : 0;

    if (_contentView) {
        CGRect contentFrame = _contentView.frame;
        contentFrame.origin.y = offset;
        [_contentView reactSetFrame:contentFrame];
    }

    if (_coverView) {
        CGRect coverFrame = _coverView.frame;
        coverFrame.origin.y = offset;
        [_coverView reactSetFrame:coverFrame];
    }
}

- (RCTShadowView *)getShadowView:(UIView *)view {
    NSMutableDictionary<NSNumber *, RCTShadowView *> *shadowViewRegistry = [_bridge.uiManager valueForKey:@"shadowViewRegistry"];
    NSNumber *reactTag = view.reactTag;
    return shadowViewRegistry[reactTag];
}

- (void)autoAddSubview:(UIView *)subview onSuperview:(UIView *)superview {
    if (!subview || !superview) {
        return;
    }
    UIView *originSuperview = [subview superview];
    if (!originSuperview) {
        [superview addSubview:subview];
        return;
    }
    if (originSuperview != superview) {
        [subview removeFromSuperview];
        [superview addSubview:subview];
    }
}

#pragma mark - Getter

- (UIView *)rootView {
    if (!_rootView) {
        UIView *rootview = self;
        while (![rootview isReactRootView] && rootview != nil) {
            rootview = [rootview superview];
        }
        _rootView = rootview;
    }
    return _rootView;
}

#pragma mark - Setter

- (void)setSynchronouslyUpdateTransform:(BOOL)synchronouslyUpdateTransform {
    synchronouslyUpdateTransform = YES;
    if (_synchronouslyUpdateTransform == synchronouslyUpdateTransform) {
        return;
    }

    if (synchronouslyUpdateTransform) {
        [self synchronousTransform];
    }
    _synchronouslyUpdateTransform = synchronouslyUpdateTransform;
}

@end
