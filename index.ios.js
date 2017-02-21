import React, { Component, PropTypes } from 'react';
import { NativeModules, StyleSheet, findNodeHandle, View, Keyboard,
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

    componentWillMount() {
        this._willShow = this._willShow.bind(this);
        this._didHide = this._didHide.bind(this);
        this._willChangeFrame = this._willChangeFrame.bind(this);

        Keyboard.addListener('keyboardWillShow', this._willShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
        Keyboard.addListener('keyboardWillChangeFrame', this._willChangeFrame);
    }

    _visible;
    _active;

    open() {
        this._callKeyboardService('openKeyboard');
    }

    close() {
        this._callKeyboardService('closeKeyboard');
    }

    showKeyboard() {
        this._callKeyboardService('showKeyboard');
        this._visible = true;
    }

    hideKeyboard() {
        this._callKeyboardService('hideKeyboard');
        this._visible = false;
    }

    toggleKeyboard() {
        this._callKeyboardService('toggleKeyboard');
        this._visible = !this._visible;
    }

    _callKeyboardService(method) {
        return NativeModules.RNKeyboardViewManager[method](findNodeHandle(this.refs.keyboardView));
    }

    _willShow({ endCoordinates: { height } }) {
        this._active = true;
        const { onShow } = this.props;
        onShow && onShow(false, height);
    }

    _didHide() {
        this._active = false;
        const { onHide } = this.props;
        onHide && onHide(this._visible);
    }

    _willChangeFrame({ endCoordinates: { height } }) {
        if (this._active) {
            const { onKeyboardChanged } = this.props;
            onKeyboardChanged && onKeyboardChanged(this._visible, height);
        }
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
