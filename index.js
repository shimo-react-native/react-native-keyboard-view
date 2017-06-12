import React, { Component, PropTypes, Children } from 'react';
import { NativeModules, Keyboard, StyleSheet, View, requireNativeComponent, Platform } from 'react-native';

const styles = StyleSheet.create({
    offSteam: {
        position: 'absolute',
        height: 0,
        width: 0
    },

    cover: {
        flex: 1
    }
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        renderStickyView: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func
    };

    static dismiss = NativeModules.KeyboardViewModule.dismiss;

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

    render() {
        const { children, renderStickyView, renderCover, transform } = this.props;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCover && renderCover();
        const hasCover = !!Children.count(cover) || !!Children.count(stickyView);
        const hasContent = Children.count(children) > 0;

        if (!hasContent && !hasCover) {
            return null;
        }

        return (
            <KeyboardView
                style={[styles.offSteam, transform && { transform }]}
                synchronouslyUpdateTransform={!!transform}
            >
                {hasContent && (
                    <KeyboardContentView style={styles.offSteam}>{children}</KeyboardContentView>
                )}
                {hasCover && (
                    <KeyboardCoverView style={styles.offSteam}>
                        <View style={styles.cover} pointerEvents="box-none">{cover}</View>
                        <View>{stickyView}</View>
                    </KeyboardCoverView>
                )}
            </KeyboardView>
        );
    }
}

const KeyboardView = requireNativeComponent('KeyboardView', null, Platform.OS === 'ios' ? {
    nativeOnly: {
        synchronouslyUpdateTransform: true
    }
} : null);
const KeyboardContentView = requireNativeComponent('KeyboardContentView');
const KeyboardCoverView = requireNativeComponent('KeyboardCoverView');
