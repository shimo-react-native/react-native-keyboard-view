import React, { Component, PropTypes } from 'react';
import { NativeModules, StyleSheet, findNodeHandle, View, Keyboard,
    requireNativeComponent } from 'react-native';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute',
        width: 0,
        height: 0
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
        onKeyboardChanged: PropTypes.func
    };

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

        Keyboard.addListener('keyboardWillShow', this._willShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
        Keyboard.addListener('keyboardWillChangeFrame', this._willChangeFrame);
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardWillShow', this._willShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
        Keyboard.removeListener('keyboardWillChangeFrame', this._willChangeFrame);
    }

    _active;

    open() {
        this._callKeyboardService('openKeyboard');
    }

    close() {
        this._callKeyboardService('closeKeyboard');
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

    _callKeyboardService(method) {
        return NativeModules.RNKeyboardViewManager[method](findNodeHandle(this.refs.keyboardView));
    }

    _willShow({ endCoordinates: { height } }) {
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

    _willChangeFrame({ endCoordinates: { height } }) {
        if (this._active) {
            this._lastFrameHeight = height;
            const { onKeyboardChanged } = this.props;
            onKeyboardChanged && onKeyboardChanged(this._visible, height);
            this._onChangeFrame(height);
        }
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
                style={styles.keyboard}>
                <View pointerEvents="box-none">
                    <View style={styles.cover} pointerEvents="box-none">{cover}</View>
                    <View>{stickyView}</View>
                    <View
                        style={{backgroundColor: backgroundColor || '#fff', opacity: +contentVisible}}
                        pointerEvents={contentVisible ? 'box-none' : 'none'}
                    >
                        {children}
                    </View>
                </View>
            </RNKeyboardView>
        );
    }
}

const RNKeyboardView = requireNativeComponent('RNKeyboardView');
