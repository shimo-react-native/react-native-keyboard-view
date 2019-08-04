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
        top: 0,
        left: 0,
        right: 0,
        height: 0,
        overflow: 'hidden',
        opacity: isIOS ? 0 : 1
    },

    cover: {
        flex: 1
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
        keyboardPlaceholderHeight: PropTypes.number,
        stickyViewStyle: PropTypes.object,
    };

    static defaultProps = {
        hideWhenKeyboardIsDismissed: true,
        contentVisible: true,
        stickyViewStyle: {}
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

        return (
          <KeyboardContentView
            style={styles.offSteam}
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

        return (
          <KeyboardCoverView
            style={styles.offSteam}
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
                <View style={this.props.stickyViewStyle}>
                    <View style={styles.androidInputAvoid}>
                        {stickyView}
                    </View>
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
                    style={[styles.offSteam, transform && { transform }]}
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
                style={[styles.offSteam]}
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
