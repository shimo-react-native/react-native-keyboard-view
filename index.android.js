import React, { Component, PropTypes } from 'react';
import { NativeModules, Keyboard, StyleSheet, BackHandler, findNodeHandle, View,
    requireNativeComponent } from 'react-native';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute',
        height: 0,
        width: 0
    },

    // Add a transparent background to keep the component structure in native side.
    // Android will automaticly composite empty group view.
    container: {
        backgroundColor: 'transparent'
    },

    cover: {
        flex: 1
    },

    hide: {
        display: 'none'
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
            contentVisible: false
        };
    }

    static dismiss = () => { NativeModules.RNKeyboardViewManager.dismiss(); };

    componentWillMount() {
        this._didShow = this._didShow.bind(this);
        this._didHide = this._didHide.bind(this);
        this._back = this._back.bind(this);

        Keyboard.addListener('keyboardDidShow', this._didShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
        BackHandler.addEventListener('hardwareBackPress', this._back);
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardDidShow', this._didShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
        BackHandler.removeEventListener('hardwareBackPress', this._back);
    }

    _active = false;
    _willToggleKeyboardManually = false;

    _back() {
        if (this.state.contentVisible) {
            this.close();
            return true;
        }

        return false;
    }

    _didShow({ endCoordinates: { height } }) {
        if (!this._willToggleKeyboardManually) {
            this._active = true;
            this._lastFrameHeight = height;
            const { onShow } = this.props;
            onShow && onShow(false, height);
        } else {
            this._willToggleKeyboardManually = false;
        }
    }

    _didHide() {
        if (!this._willToggleKeyboardManually) {
            this._active = false;
            const { onHide } = this.props;
            onHide && onHide(this.state.onKeyboardChanged);
            this.setState({contentVisible: false});
        } else {
            this._willToggleKeyboardManually = false;
        }
    }

    async close() {
        if (!await NativeModules.RNKeyboardModule.closeKeyboard(findNodeHandle(this.refs.keyboardView))) {
            this._didHide();
        }
    }

    showKeyboard() {
        this._changeContentVisible(false);
    }

    hideKeyboard() {
        this._changeContentVisible(true);
    }

    toggleKeyboard() {
        this._changeContentVisible(!this.state.contentVisible);
    }

    _changeContentVisible(contentVisible) {
        this._willToggleKeyboardManually = true;
        this.setState({
            contentVisible
        }, () => {
            this._onChangeFrame();
        });
    }

    _onChangeFrame(height = this._lastFrameHeight) {
        const { onKeyboardChanged } = this.props;
        onKeyboardChanged && onKeyboardChanged(this.state.contentVisible, height);
    }

    render() {
        const { backgroundColor, children, renderStickyView, renderCover, visible } = this.props;
        const { contentVisible } = this.state;
        const stickyView = renderStickyView && renderStickyView();
        const cover = renderCover && renderCover();

        return (
            <RNKeyboardView
                ref="keyboardView"
                contentVisible={contentVisible}
                style={styles.keyboard}>
                <View pointerEvents="box-none" style={[styles.container, !visible && styles.hide]}>
                    <View style={[styles.container, styles.cover]} pointerEvents="box-none">{cover}</View>
                    <View>{stickyView}</View>
                    <View style={{backgroundColor: backgroundColor || '#fff'}}>
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
