import React, { Component, PropTypes } from 'react';
import { NativeModules, Keyboard, StyleSheet, BackAndroid, Easing, findNodeHandle, View,
    requireNativeComponent, Animated } from 'react-native';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute',
        height: 0,
        width: 0
    },
    container: {
        backgroundColor: 'transparent'
    },

    content: {
        position: 'absolute',
        right: 0,
        bottom: 0,
        left: 0
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

    constructor(props) {
        super(props);
        this.state = {
            contentVisible: props.initialState || false
        };
    }

    componentWillMount() {
        this._didShow = this._didShow.bind(this);
        this._didHide = this._didHide.bind(this);
        Keyboard.addListener('keyboardDidShow', this._didShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardDidShow', this._didShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
    }

    _active = false;

    _didShow({ endCoordinates: { height } }) {
        this._active = true;
        this._lastFrameHeight = height;
        const { onShow } = this.props;
        onShow && onShow(false, height);
    }

    _didHide() {
        this._active = false;
        const { onHide } = this.props;
        onHide && onHide(this._visible);
    }

    close() {
        NativeModules.RNKeyboardModule.closeKeyboard(findNodeHandle(this.refs.keyboardView));
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

    _onChangeFrame(height = this._lastFrameHeight) {
        const { onKeyboardChanged } = this.props;
        onKeyboardChanged && onKeyboardChanged(this.state.contentVisible, height);
    }

    render() {
        const { backgroundColor, children, renderStickyView, renderCover } = this.props;
        const { contentVisible } = this.state;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCover && renderCover();

        return (
            <RNKeyboardView
                ref="keyboardView"
                contentVisible={contentVisible}
                style={styles.keyboard}>
                <View pointerEvents="box-none" >
                    <View style={styles.cover} pointerEvents="box-none">{cover}</View>
                    <View>{stickyView}</View>
                    <View

                        style={{backgroundColor: backgroundColor || '#fff'}}
                    >
                        {children}
                    </View>
                </View>
            </RNKeyboardView>
        );
    }
}

const RNKeyboardView = requireNativeComponent('RNKeyboardView', null, {
    nativeOnly: {
        contentVisible: true
    }
});
