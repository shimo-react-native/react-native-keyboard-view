//
//  RNKeyboardEventEmitter.m
//  RNKeyboardView
//
//  Created by Bell Zhong on 2017/8/9.
//  Copyright © 2017年 shimo. All rights reserved.
//

#import "RNKeyboardEventEmitter.h"
#import "RNKeyboardHostView.h"
#import "YYKeyboardManager.h"

static NSString *const RNKeyboardInHardwareKeyboardModeNameEventName = @"InHardwareKeyboardModeNameEvent";

@interface RNKeyboardEventEmitter () {
    BOOL _hasListeners;
}

@end

@implementation RNKeyboardEventEmitter

RCT_EXPORT_MODULE();

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (instancetype)init {
    if (self = [super init]) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleKeyboardInHardwareKeyboardModeNotification:)
                                                     name:YYKeyboardInHardwareKeyboardModeNotification
                                                   object:nil];
    }
    return self;
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (NSArray<NSString *> *)supportedEvents {
    return @[RNKeyboardInHardwareKeyboardModeNameEventName];
}

- (void)startObserving {
    _hasListeners = YES;
}

- (void)stopObserving {
    _hasListeners = NO;
}

- (void)handleKeyboardInHardwareKeyboardModeNotification:(NSNotification *)notification {
    if (_hasListeners) { // Only send events if anyone is listening
        [self sendEventWithName:RNKeyboardInHardwareKeyboardModeNameEventName
                           body:notification.object];
    }
}

@end
