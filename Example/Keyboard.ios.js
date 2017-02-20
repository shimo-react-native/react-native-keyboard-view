import React, { Component, PropTypes } from 'react';
import { NativeModules, StyleSheet, findNodeHandle, View,
    requireNativeComponent } from 'react-native';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute',
        width: 0,
        height: 0
    }
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        backgroundColor: PropTypes.string,
        renderStickyView: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func,
        onKeyboardChanged: PropTypes.func
    };

    constructor(props, context) {
        super(props, context);
        this.state = {
            visible: false,
            contentEnabled: false
        };
    }

    // We don't want any responder events bubbling out of the KeyboardView.
    _shouldSetResponder(): boolean {
        return true;
    }

    open() {
        if (!this.state.visible) {
            this._callKeyboardService('openKeyboard');
        }
    }

    async close() {
        if (this.state.visible) {
            this._callKeyboardService('closeKeyboard');
        }
    }

    showKeyboard() {
        if (this.state.visible) {
            this._callKeyboardService('showKeyboard');
        }
    }

    hideKeyboard() {
        if (this.state.visible) {
            this._callKeyboardService('hideKeyboard');
        }
    }

    toggleKeyboard() {
        if (this.state.visible) {
            this._callKeyboardService('toggleKeyboard');
        }
    }

    _callKeyboardService(method) {
        return NativeModules.RNKeyboardModule[method](findNodeHandle(this.refs.keyboardView));
    }

    render() {
        const { backgroundColor, children, renderStickyView } = this.props;
        const { visible } = this.state;

        return (
            <RNKeyboardView
                ref="keyboardView"
                onStartShouldSetResponder={this._shouldSetResponder}
                style={styles.keyboard}
                visible={visible}>
                <View style={{backgroundColor, opacity: 0.75}}>
                    {children}
                </View>
                <View style={{position: 'absolute', left: 0, right: 0, top: 0}}>
                    {renderStickyView && renderStickyView()}
                </View>
            </RNKeyboardView>
        );
    }
}

const RNKeyboardView = requireNativeComponent('RNKeyboardView', null, {
    nativeOnly: {
        visible: true
    }
});
