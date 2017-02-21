import React, { Component, PropTypes } from 'react';
import { NativeModules, StyleSheet, findNodeHandle, View,
    requireNativeComponent } from 'react-native';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute',
        width: 0,
        height: 0
    },

    stickyView: {
        position: 'absolute',
        left: 0,
        right: 0,
        top: 0
    }
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        height: PropTypes.number.isRequired,
        stickyViewInside: PropTypes.bool,
        backgroundColor: PropTypes.string,
        renderStickyView: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func,
        onKeyboardChanged: PropTypes.func
    };

    open() {
        this._callKeyboardService('openKeyboard');
    }

    close() {
        this._callKeyboardService('closeKeyboard');
    }

    showKeyboard() {
        this._callKeyboardService('showKeyboard');
    }

    hideKeyboard() {
        this._callKeyboardService('hideKeyboard');
    }

    toggleKeyboard() {
        this._callKeyboardService('toggleKeyboard');
    }

    _callKeyboardService(method) {
        return NativeModules.RNKeyboardViewManager[method](findNodeHandle(this.refs.keyboardView));
    }

    render() {
        const { backgroundColor, children, renderStickyView, height, stickyViewInside } = this.props;

        return (
            <RNKeyboardView
                ref="keyboardView"
                style={styles.keyboard}
                containerHeight={height}
                stickyViewInside={stickyViewInside}>
                <View style={styles.stickyView}>
                    {renderStickyView && renderStickyView()}
                </View>
                <View style={{backgroundColor: backgroundColor || '#fff'}}>
                    {children}
                </View>
            </RNKeyboardView>
        );
    }
}

const RNKeyboardView = requireNativeComponent('RNKeyboardView', null, {
    nativeOnly: {
        stickyViewInside: true,
        containerHeight: true
    }
});
