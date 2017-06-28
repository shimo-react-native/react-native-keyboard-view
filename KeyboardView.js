import React, { Component, PropTypes, Children } from 'react';
import { NativeModules, Keyboard, StyleSheet, View, requireNativeComponent, Platform, Animated } from 'react-native';
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
        onShow: PropTypes.func,
        onHide: PropTypes.func
    };

    static dismiss = isIOS ?
      NativeModules.RNKeyboardViewManager.dismiss :
      NativeModules.KeyboardViewModule.dismiss;

    static dismissWithoutAnimation = isIOS ?
      NativeModules.RNKeyboardViewManager.dismissWithoutAnimation :
      null;

    componentWillMount() {
        Keyboard.addListener('keyboardDidShow', this._didShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardDidShow', this._didShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
    }

    _didShow = () => {
        const { onShow } = this.props;
        onShow && onShow();
    };

    _didHide = () => {
        const { onHide } = this.props;
        onHide && onHide();
    };


    _shouldSetResponder() {
        return true;
    }

    _getContentView(children, visible) {
        if (!isIOS && !visible) {
            return null;
        }

        const hide = (isIOS && !visible) ? styles.hide : null;

        return (
          <KeyboardContentView
            style={[styles.offSteam, hide]}
            pointerEvents="box-none"
          >
              {children}
          </KeyboardContentView>
        );
    }

    _getCoverView(cover, stickyView, visible) {
        if (!isIOS && !visible) {
            return null;
        }

        const hide = (isIOS && !visible) ? styles.hide : null;

        return (
          <KeyboardCoverView
            style={[styles.offSteam, hide]}
            pointerEvents="box-none"
            collapsable={false}
          >
              <View
                style={styles.cover}
                pointerEvents="box-none"
                collapsable={false}
              >
                  {cover}
              </View>
              <View collapsable={false} >
                  {stickyView}
              </View>
          </KeyboardCoverView>
        );
    }

    _hasChildren(children) {
        return children && Children.count(children) > 0;
    }

    render() {
        const { children, renderStickyView, renderCover, transform } = this.props;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCover && renderCover();
        const hasCover = this._hasChildren(cover) || this._hasChildren(stickyView);
        const hasContent = this._hasChildren(children);

        if (!hasContent && !hasCover) {
            return null;
        }

        if (isIOS) {
            return (
              <Modal style={styles.offSteam} visible={true}>
                  <KeyboardView
                    style={[styles.offSteam, transform && { transform }]}
                    synchronouslyUpdateTransform={!!transform}
                  >
                      {this._getContentView(children, hasContent)}
                      {this._getCoverView(cover, stickyView, hasCover)}
                  </KeyboardView>
              </Modal>
            );
        } else {
            return (
              <KeyboardView
                style={styles.offSteam}
              >
                  {this._getContentView(children, hasContent)}
                  {this._getCoverView(cover, stickyView, hasCover)}
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
            synchronouslyUpdateTransform: true
        }
    });

    KeyboardView = Animated.createAnimatedComponent(KeyboardView);
    KeyboardContentView = requireNativeComponent('RNKeyboardContentView');
    KeyboardCoverView = requireNativeComponent('RNKeyboardCoverView');
} else {
    KeyboardView = requireNativeComponent('KeyboardView');
    KeyboardContentView = requireNativeComponent('KeyboardContentView');
    KeyboardCoverView = requireNativeComponent('KeyboardCoverView');
}
