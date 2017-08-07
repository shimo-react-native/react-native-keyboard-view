import React, { Component, Children } from 'react';
import PropTypes from 'prop-types';
import { NativeModules, StyleSheet, View, requireNativeComponent, Platform, Animated } from 'react-native';
import Modal from 'react-native-root-modal';

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
    }
});

const isIOS = Platform.OS === 'ios';

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        renderStickyView: PropTypes.func,
        renderCoverView: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func,
        hideWhenKeyboardIsDismissed: PropTypes.bool
    };

    static defaultProps = {
        hideWhenKeyboardIsDismissed: true
    };

    static dismiss = isIOS ?
      NativeModules.RNKeyboardViewManager.dismiss :
      NativeModules.KeyboardViewModule.dismiss;

    static dismissWithoutAnimation = isIOS ?
      NativeModules.RNKeyboardViewManager.dismissWithoutAnimation :
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
                style={styles.cover}
                pointerEvents="box-none"
              >
                  {cover}
              </View>
              {stickyView && (
                <View>
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
        const { children, renderStickyView, renderCoverView, transform, onHide, onShow,
          hideWhenKeyboardIsDismissed } = this.props;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCoverView && renderCoverView();
        const hasCover = this._hasChildren(cover) || this._hasChildren(stickyView);
        const hasContent = this._hasChildren(children);

        const props = {
            onKeyboardHide: onHide,
            onKeyboardShow: onShow,
            hideWhenKeyboardIsDismissed: hideWhenKeyboardIsDismissed
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

if (isIOS) {
    KeyboardView = requireNativeComponent('RNKeyboardView', null, {
        nativeOnly: {
            synchronouslyUpdateTransform: true,
            hideWhenKeyboardIsDismissed: true,
            onKeyboardHide: true,
            onKeyboardShow: true
        }
    });

    KeyboardView = Animated.createAnimatedComponent(KeyboardView);
    KeyboardContentView = requireNativeComponent('RNKeyboardContentView');
    KeyboardCoverView = requireNativeComponent('RNKeyboardCoverView');
} else {
    KeyboardView = requireNativeComponent('KeyboardView', null, {
        nativeOnly: {
            hideWhenKeyboardIsDismissed: true,
            onKeyboardHide: true,
            onKeyboardShow: true
        }
    });
    KeyboardContentView = requireNativeComponent('KeyboardContentView');
    KeyboardCoverView = requireNativeComponent('KeyboardCoverView');
}
