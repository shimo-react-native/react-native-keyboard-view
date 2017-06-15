import React, { Component, PropTypes, Children } from 'react';
import { NativeModules, Keyboard, StyleSheet, View, requireNativeComponent, Platform, Animated } from 'react-native';
import Modal from 'react-native-root-modal'

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

    contentView: {
        justifyContent: 'flex-end'
    }
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        renderStickyView: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func
    };

    static dismiss = Platform.OS === 'ios' ?
        NativeModules.RNKeyboardViewManager.dismiss :
        NativeModules.KeyboardViewModule.dismiss;

    static dismissWithoutAnimation = Platform.OS === 'ios' ?
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

    _getContentView(children) {
        if (Platform.OS === 'ios') {
            return (
                <KeyboardContentView
                    style={styles.contentView}
                    pointerEvents="box-none"
                >
                    <View>{children}</View>
                </KeyboardContentView>
            );
        } else {
            return (
                <KeyboardContentView style={styles.offSteam}>{children}</KeyboardContentView>
            );
        }
    }

    _getCoverView(cover, stickyView) {
        return (
            <KeyboardCoverView
                style={styles.offSteam}
                pointerEvents="box-none"
            >
                <View style={styles.cover} pointerEvents="box-none">{cover}</View>
                <View>{stickyView}</View>
            </KeyboardCoverView>
        );
    }

    render() {
        const { children, renderStickyView, renderCover, transform } = this.props;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCover && renderCover();
        const hasCover = (!!Children.count(cover) || !!Children.count(stickyView));
        const hasContent = Children.count(children) > 0;

        if (!hasContent && !hasCover) {
            return null;
        }

        const KeyboardComponent = (Platform.OS === 'ios' && transform) ? AnimatedKeyboardView : KeyboardView;
        const keyboard = (
            <KeyboardComponent
                style={[styles.offSteam, transform && { transform }]}
                synchronouslyUpdateTransform={!!transform}
            >
                {hasContent && this._getContentView(children)}
                {hasCover && this._getCoverView(cover, stickyView)}
            </KeyboardComponent>
        );

        return Platform.OS === 'ios' ? (
            <Modal style={styles.offSteam} visible={true}>
                {keyboard}
            </Modal>
        ) : keyboard;

    }
}

let KeyboardView,
    KeyboardContentView,
    KeyboardCoverView;

if (Platform.OS === 'ios') {
    KeyboardView = requireNativeComponent('RNKeyboardView', null, {
        nativeOnly: {
            synchronouslyUpdateTransform: true
        }
    });
    KeyboardContentView = requireNativeComponent('RNKeyboardContentView');
    KeyboardCoverView = requireNativeComponent('RNKeyboardCoverView');
} else {
    KeyboardView = requireNativeComponent('KeyboardView');
    KeyboardContentView = requireNativeComponent('KeyboardContentView');
    KeyboardCoverView = requireNativeComponent('KeyboardCoverView');
}

const AnimatedKeyboardView = Animated.createAnimatedComponent(KeyboardView);
