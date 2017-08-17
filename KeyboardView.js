import React, { Component, Children } from 'react';
import PropTypes from 'prop-types';
import { NativeModules, StyleSheet, View, requireNativeComponent, Platform, Animated, Dimensions } from 'react-native';
import Modal from 'react-native-root-modal';

const SCREEN_HEIGHT = Dimensions.get('screen').height;
const isIOS = Platform.OS === 'ios';
const isAndroid = Platform.OS === 'android';

const styles = StyleSheet.create({
    offSteam: {
        position: 'absolute',
        height: 0,
        width: 0,
        overflow: 'hidden'
    },

    cover: {
        flex: 1
    },

    hide: {
        opacity: 0
    },

    androidInputAvoid: isAndroid ? {
        bottom: SCREEN_HEIGHT,
        transform: [{ translateY: SCREEN_HEIGHT }]
    } : {}
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        renderStickyView: PropTypes.func,
        renderCoverView: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func,
        hideWhenKeyboardIsDismissed: PropTypes.bool,
        contentVisible: PropTypes.bool,
        keyboardPlaceholderHeight: PropTypes.number
    };

    static defaultProps = {
        hideWhenKeyboardIsDismissed: true,
        contentVisible: true
    };

    static dismiss = isIOS ?
      NativeModules.RNKeyboardViewManager.dismiss :
      NativeModules.KeyboardViewModule.dismiss;

    static dismissWithoutAnimation = isIOS ?
      NativeModules.RNKeyboardViewManager.dismissWithoutAnimation :
      null;

    static getInHardwareKeyboardMode = isIOS ?
      NativeModules.RNKeyboardViewManager.getInHardwareKeyboardMode :
      null;

    _shouldSetResponder() {
        return true;
    }

    _getContentView(children, visible) {
        if (!visible) {
            return null;
        }

        const hide = (isIOS && !visible) ? styles.hide : null;

        return (
          <KeyboardContentView
            style={[styles.offSteam, hide]}
            pointerEvents="box-none"
            key="contentView"
          >
              {children}
          </KeyboardContentView>
        );
    }

    _getCoverView(cover, stickyView, visible) {
        if (!visible) {
            return null;
        }

        const hide = (isIOS && !visible) ? styles.hide : null;

        return (
          <KeyboardCoverView
            style={[styles.offSteam, hide]}
            pointerEvents="box-none"
            key="coverView"
          >
              <View
                style={[styles.cover, styles.androidInputAvoid]}
                pointerEvents="box-none"
              >
                  {cover}
              </View>
              {stickyView && (
                <View style={styles.androidInputAvoid}>
                    {stickyView}
                </View>
              )}
          </KeyboardCoverView>
        );
    }

    _hasChildren(children) {
        return children && Children.count(children) > 0;
    }

    render() {
        const { children, renderStickyView, renderCoverView, transform, onHide, onShow, keyboardPlaceholderHeight,
          hideWhenKeyboardIsDismissed, contentVisible } = this.props;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCoverView && renderCoverView();
        const hasCover = this._hasChildren(cover) || this._hasChildren(stickyView);
        const hasContent = this._hasChildren(children);

        const props = {
            onKeyboardHide: onHide,
            onKeyboardShow: onShow,
            hideWhenKeyboardIsDismissed,
            keyboardPlaceholderHeight,
            contentVisible
        };

        const childViews = [
            this._getContentView(children, hasContent, hasContent),
            this._getCoverView(cover, stickyView, hasCover)
        ];

        if (isIOS) {
            return (
              <Modal style={styles.offSteam} visible={true}>
                  <KeyboardView
                    style={[styles.offSteam, styles.hide, transform && { transform }]}
                    synchronouslyUpdateTransform={!!transform}
                    {...props}
                  >
                      {childViews}
                  </KeyboardView>
              </Modal>
            );
        } else {
            return (
              <KeyboardView
                style={[styles.offSteam, styles.hide]}
                {...props}
              >
                  {childViews}
              </KeyboardView>
            );
        }
    }
}

let KeyboardView,
  KeyboardContentView,
  KeyboardCoverView;

const nativeOnlyProps = {
    hideWhenKeyboardIsDismissed: true,
    onKeyboardHide: true,
    onKeyboardShow: true,
    keyboardPlaceholderHeight: true,
    contentVisible: true
};

if (isIOS) {
    KeyboardView = requireNativeComponent('RNKeyboardView', null, {
        nativeOnly: {
            ...nativeOnlyProps,
            synchronouslyUpdateTransform: true
        }
    });

    KeyboardView = Animated.createAnimatedComponent(KeyboardView);
    KeyboardContentView = requireNativeComponent('RNKeyboardContentView');
    KeyboardCoverView = requireNativeComponent('RNKeyboardCoverView');
} else {
    KeyboardView = requireNativeComponent('KeyboardView', null, {
        nativeOnly: nativeOnlyProps
    });
    KeyboardContentView = requireNativeComponent('KeyboardContentView');
    KeyboardCoverView = requireNativeComponent('KeyboardCoverView');
}
