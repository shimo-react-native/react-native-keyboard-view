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
        position: 'absolute',
        backgroundColor: 'transparent',
        justifyContent: 'flex-end',
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
        height: PropTypes.number.isRequired,
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
            contentEnabled: false,
            height: 0
        };
        this._translateY = new Animated.Value(0);
    }

    componentWillMount() {
        this._didShow = this._didShow.bind(this);
        this._didHide = this._didHide.bind(this);
        this._back = this._back.bind(this);
        this._onContentLayout = this._onContentLayout.bind(this);
        Keyboard.addListener('keyboardDidShow', this._didShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
        BackAndroid.addEventListener('hardwareBackPress', this._back);
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardDidShow', this._didShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
        BackAndroid.removeEventListener('hardwareBackPress', this._back);
    }

    _willHideKeyboardManually = false;
    _translateY = null;
    _closing = false;

    _back() {
        if (this.state.visible && !this._closing) {
            this.close();
            return true;
        }

        return false;
    }

    async _didShow({ endCoordinates: { height } }) {
        this._willHideKeyboardManually = false;
        const { onKeyboardChanged, onShow } = this.props;

        if (!this.state.visible) {
            onShow && onShow(false, height);

            this.setState({
                visible: true
            });
            this._translateY.setValue(this.state.height - height);
        } else {
            onKeyboardChanged && onKeyboardChanged(false, height);

            if (this.props.height !== height) {
                Animated.timing(this._translateY, {
                    toValue: this.state.height - height,
                    useNativeDriver: true,
                    duration: 120
                }).start();
            }
        }
    }

    _didHide() {
        const { onKeyboardChanged, onHide } = this.props;

        if (this._willHideKeyboardManually) {
            this._willHideKeyboardManually = false;
            Animated.timing(this._translateY, {
                toValue: 0,
                useNativeDriver: true,
                duration: 120
            }).start();

            onKeyboardChanged && onKeyboardChanged(true, this.props.height);
        } else {
            onHide && onHide(false);
            this.setState({
                visible: false
            }, () => {

                if (this._closing) {
                    this._translateY.setValue(this.state.height);
                    this._closing = false;
                }
            });
        }
    }

    _onContentLayout({ nativeEvent: { layout: { height, x } } }) {
        this.setState({ height });
        if (!this.state.visible) {
            this._translateY.setValue(height);
        }
    }

    // We don't want any responder events bubbling out of the KeyboardView.
    _shouldSetResponder(): boolean {
        return true;
    }

    open() {
        if (!this.state.visible) {
            const { onShow } = this.props;
            onShow && onShow(true, this.props.height);

            this.setState({
                visible: true
            });
            this._translateY.stopAnimation();
            Animated.timing(this._translateY, {
                toValue: 0,
                duration: 160,
                useNativeDriver: true
            }).start();
        }
    }

    async close() {
        if (this.state.visible) {
            const { onHide } = this.props;
            this._closing = true;
            if (!await this._callKeyboardService('closeKeyboard')) {
                Animated.timing(this._translateY, {
                    toValue: this.state.height,
                    duration: 160,
                    useNativeDriver: true,
                    easing: Easing.inOut(Easing.ease)
                }).start(() => {
                    this.setState({
                        visible: false
                    });
                    this._closing = false;
                    onHide && onHide(true);
                });
            }
        }
    }

    showKeyboard() {
        if (this.state.visible) {
            this._callKeyboardService('showKeyboard');
        }
    }

    hideKeyboard() {
        if (this.state.visible) {
            this._willHideKeyboardManually = true;
            this._callKeyboardService('hideKeyboard');
        }
    }

    toggleKeyboard() {
        if (this.state.visible) {
            this._willHideKeyboardManually = true;
            this._callKeyboardService('toggleKeyboard');
        }
    }

    _callKeyboardService(method) {
        return NativeModules.RNKeyboardModule[method](findNodeHandle(this.refs.keyboardView));
    }

    render() {
        const { backgroundColor, children, renderStickyView } = this.props;
        const { visible, height } = this.state;

        return (
            <RNKeyboardView
                ref="keyboardView"
                onStartShouldSetResponder={this._shouldSetResponder}
                style={styles.keyboard}
                visible={visible}>
                <Animated.View style={[styles.container, {transform: [{translateY: this._translateY}]}]}>
                    {renderStickyView && renderStickyView()}
                    <View style={{backgroundColor, height}}/>
                    <View style={styles.content} onLayout={this._onContentLayout}>
                        {children}
                    </View>
                </Animated.View>
            </RNKeyboardView>
        );
    }
}

const RNKeyboardView = requireNativeComponent('RNKeyboardView', null, {
    nativeOnly: {
        height: true,
        visible: true
    }
});
