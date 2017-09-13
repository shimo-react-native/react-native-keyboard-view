#import "UIViewController+RNKeyboard.h"
#import <objc/runtime.h>

NSNotificationName const RNViewControllerWillPresentNotification = @"RNViewControllerWillPresentNotification";
NSNotificationName const RNViewControllerDidPresentNotification = @"RNViewControllerDidPresentNotification";
NSNotificationName const RNViewControllerWillDismissNotification = @"RNViewControllerWillDismissNotification";
NSNotificationName const RNViewControllerDidDismissNotification = @"RNViewControllerDidDismissNotification";

@implementation UIViewController (RNKeyboard)

- (void)rnk_presentViewController:(UIViewController *)viewControllerToPresent animated: (BOOL)flag completion:(void (^ __nullable)(void))completion {
    [[NSNotificationCenter defaultCenter] postNotificationName:RNViewControllerWillPresentNotification
                                                        object:@{
                                                                 @"animated":@(flag),
                                                                 @"target":viewControllerToPresent,
                                                                 @"host":self}];
    [self rnk_presentViewController:viewControllerToPresent animated:flag completion: ^() {
        if (completion) {
            completion();
        }
        [[NSNotificationCenter defaultCenter] postNotificationName:RNViewControllerDidPresentNotification
                                                            object:@{
                                                                     @"animated":@(flag),
                                                                     @"target":viewControllerToPresent,
                                                                     @"host":self}];
    }];
}

- (void)rnk_dismissViewControllerAnimated:(BOOL)flag completion: (void (^ __nullable)(void))completion {
    [[NSNotificationCenter defaultCenter] postNotificationName:RNViewControllerWillDismissNotification
                                                        object:@{
                                                                 @"animated":@(flag),
                                                                 @"host":self}];
    [self rnk_dismissViewControllerAnimated:flag completion: ^() {
        if (completion) {
            completion();
        }
        [[NSNotificationCenter defaultCenter] postNotificationName:RNViewControllerDidDismissNotification
                                                            object:@{
                                                                     @"animated":@(flag),
                                                                     @"host":self}];
    }];
}

+ (void)load {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        Method originPresentMethod = class_getInstanceMethod(self, @selector(presentViewController:animated:completion:));
        Method presentMethod = class_getInstanceMethod(self, @selector(rnk_presentViewController:animated:completion:));
        method_exchangeImplementations(originPresentMethod, presentMethod);
        
    
        Method originDismissMethod = class_getInstanceMethod(self, @selector(dismissViewControllerAnimated:completion:));
        Method dismissMethod = class_getInstanceMethod(self, @selector(rnk_dismissViewControllerAnimated:completion:));
        method_exchangeImplementations(originDismissMethod, dismissMethod);
    });
}

@end
