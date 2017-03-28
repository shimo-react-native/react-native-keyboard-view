import React, { Component, PropTypes } from 'react';
import { NativeModules, StyleSheet, View, Keyboard, Animated,
    requireNativeComponent } from 'react-native';

import Modal from 'react-native-root-modal';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute',
        width: 0,
        height: 0,
        opacity: 0
    },

    cover: {
        flex: 1
    }
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        initialState: PropTypes.bool,
        backgroundColor: PropTypes.string,
        renderStickyView: PropTypes.func,
        renderCover: PropTypes.func,
        onShow: PropTypes.func,
        onHide: PropTypes.func,
        onKeyboardChanged: PropTypes.func,
        transform: PropTypes.array
    };

    static dismiss = () => { NativeModules.RNKeyboardViewManager.closeKeyboard(); };

    constructor(props) {
        super(props);
        this.state = {
            contentVisible: props.initialState || false
        };
    }

    componentWillMount() {
        this._willShow = this._willShow.bind(this);
        this._didHide = this._didHide.bind(this);
        this._willChangeFrame = this._willChangeFrame.bind(this);
        this._onStickyViewLayout = this._onStickyViewLayout.bind(this);

        Keyboard.addListener('keyboardWillShow', this._willShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
        Keyboard.addListener('keyboardWillChangeFrame', this._willChangeFrame);
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardWillShow', this._willShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
        Keyboard.removeListener('keyboardWillChangeFrame', this._willChangeFrame);
    }


    close() {
        NativeModules.RNKeyboardViewManager.closeKeyboard();
    }

    showKeyboard() {
        this.setState({
            contentVisible: false
        }, () => {
            this._onChangeFrame();
        });
    }

    hideKeyboard() {
        this.setState({
            contentVisible: true
        }, () => {
            this._onChangeFrame();
        });
    }

    toggleKeyboard() {
        this.setState({
            contentVisible: !this.state.contentVisible
        }, () => {
            this._onChangeFrame();
        });
    }

    _active = false;
    _stickyViewHeight = 0;

    _willShow({ endCoordinates: { height } }) {
        this._active = true;
        this._lastFrameHeight = height;
        const { onShow } = this.props;
        onShow && onShow(this.state.contentVisible, height + this._stickyViewHeight);
    }

    _didHide() {
        this._active = false;
        const { onHide } = this.props;
        onHide && onHide(this.state.contentVisible, this._stickyViewHeight);
    }

    _willChangeFrame({
        endCoordinates: { height, screenY },
        startCoordinates: { height: startHeight, screenY: startScreenY}
    }) {
        // Do not trigger onKeyboardChanged callback on keyboard show and hide
        if (screenY + startHeight !== startScreenY && screenY !== startHeight + startScreenY) {
            this._lastFrameHeight = height;
            this._onChangeFrame(height);
        }
    }

    _onChangeFrame(height = this._lastFrameHeight) {
        const { onKeyboardChanged } = this.props;
        onKeyboardChanged && onKeyboardChanged(this.state.contentVisible, height + this._stickyViewHeight);
    }

    _onStickyViewLayout({ nativeEvent: { layout: { height } } }) {
        if (this._stickyViewHeight !== height) {
            this._stickyViewHeight = height;
            if (this._active) {
                this._onChangeFrame();
            }
        }
    }

    render() {
        const { backgroundColor, children, renderStickyView, renderCover, transform } = this.props;
        const { contentVisible } = this.state;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCover && renderCover();
        const KeyboardView = transform ? AnimatedKeyboardView : RNKeyboardView;

        return (
            <Modal visible={true} style={styles.keyboard}>
                <KeyboardView
                    pointerEvents="none"
                    synchronouslyUpdateTransform={!!transform}
                    style={[styles.keyboard, transform && {transform}]}>
                    <View pointerEvents="box-none">
                        <View style={styles.cover} pointerEvents="box-none">{cover}</View>
                        <View onLayout={this._onStickyViewLayout}>{stickyView}</View>
                        <View
                            style={{backgroundColor: backgroundColor || '#fff', opacity: +contentVisible}}
                            pointerEvents={contentVisible ? 'box-none' : 'none'}
                        >
                            {children}
                        </View>
                    </View>
                </KeyboardView>
            </Modal>

        );
    }
}

const RNKeyboardView = requireNativeComponent('RNKeyboardView', null, {
    nativeOnly: {
        synchronouslyUpdateTransform: true
    }
});

const AnimatedKeyboardView = Animated.createAnimatedComponent(RNKeyboardView);
