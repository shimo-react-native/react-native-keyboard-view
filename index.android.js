import React, { Component, PropTypes, Children } from 'react';
import { NativeModules, Keyboard, StyleSheet, BackAndroid, Easing, findNodeHandle, View,
    requireNativeComponent, Animated } from 'react-native';

const styles = StyleSheet.create({
    keyboard: {
        position: 'absolute'
    },
    container: {
        position: 'absolute',
        backgroundColor: '#fff',
        flexDirection: 'column-reverse'
    },

    content: {
        flex: 1
    }
});

export default class extends Component {
    static displayName = 'KeyboardView';

    static propTypes = {
        height: PropTypes.number.isRequired,
        backgroundColor: PropTypes.string,
        renderStickyView: PropTypes.func,
        stickyViewHeight: PropTypes.number
    };

    constructor(props, context) {
        super(props, context);
        this.state = {
            visible: false,
            contentEnabled: false,
            height: props.height
        };
        this._animatedTranslateValue = new Animated.Value(0);
        this._updateInterpolates(props);
    }

    componentWillMount() {
        this._didShow = this._didShow.bind(this);
        this._didHide = this._didHide.bind(this);
        this._back = this._back.bind(this);
        Keyboard.addListener('keyboardDidShow', this._didShow);
        Keyboard.addListener('keyboardDidHide', this._didHide);
        BackAndroid.addEventListener('hardwareBackPress', this._back);
    }


    componentWillReceiveProps(nextProps) {
        if (nextProps.height !== this.props.height) {
            this.setState({
                height: nextProps.height
            });
        }
    }

    componentWillUpdate(nextProps, nextState) {
        if (nextState.height !== this.state.height) {
            this._updateInterpolates(nextProps);
        }
    }

    componentWillUnmount() {
        Keyboard.removeListener('keyboardDidShow', this._didShow);
        Keyboard.removeListener('keyboardDidHide', this._didHide);
        BackAndroid.removeEventListener('hardwareBackPress', this._back);
    }

    _animatedTranslateValue = null;
    _translateY = null;
    _willHideKeyboardManually = false;
    _closing = false;

    _updateInterpolates(props) {
        this._translateY = this._animatedTranslateValue.interpolate({
            inputRange: [0, 1],
            outputRange: [props.height, 0]
        });
    }

    _back() {
        if (this.state.visible && !this._closing) {
            this.close();
            return true;
        }

        return false;
    }

    async _didShow({ endCoordinates: { height } }) {
        this._willHideKeyboardManually = false;

        if (!this.state.visible) {
            await this._setAsVisible();
            this._animatedTranslateValue.setValue(height / this.props.height);
        } else if (this.props.height !== height) {
            Animated.timing(this._animatedTranslateValue, {
                toValue: height / this.props.height,
                useNativeDriver: true,
                duration: 120
            }).start(() => this.setState({ height }));
        }
    }

    _didHide() {
        if (this._willHideKeyboardManually) {
            this._willHideKeyboardManually = false;
            this.setState({
                height: this.props.height
            }, () => {
                Animated.timing(this._animatedTranslateValue, {
                    toValue: 1,
                    useNativeDriver: true,
                    duration: 120
                }).start();
            });
        } else {
            this.setState({
                visible: false
            });
        }
    }

    // We don't want any responder events bubbling out of the KeyboardView.
    _shouldSetResponder(): boolean {
        return true;
    }

    _setAsVisible() {
        return new Promise((resolve) => {
            this.setState({
                visible: true
            }, resolve);
        });
    }

    open() {
        if (!this.state.visible) {
            this._setAsVisible();
            this._animatedTranslateValue.stopAnimation();
            Animated.timing(this._animatedTranslateValue, {
                toValue: 1,
                duration: 160,
                useNativeDriver: true
            }).start();
        }
    }

    async close() {
        if (this.state.visible) {
            if (!await this._callKeyboardService('closeKeyboard')) {
                Animated.timing(this._animatedTranslateValue, {
                    toValue: 0,
                    duration: 160,
                    useNativeDriver: true,
                    easing: Easing.inOut(Easing.ease)
                }).start(() => {
                    this.setState({
                        visible: false
                    });
                });
            } else {
                this._animatedTranslateValue.setValue(0);
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
        const { backgroundColor, children, renderStickyView, stickyViewHeight } = this.props;
        const { visible, height } = this.state;

        let content = children;

        if (Children.count(children)) {
            content = (
                <Animated.View
                    style={[
                            styles.container,
                            backgroundColor && { backgroundColor },
                            {transform: [{ translateY: this._translateY }]}
                        ]}
                >
                    <View style={[styles.content]}>
                        {children}
                    </View>
                    {renderStickyView && renderStickyView()}
                </Animated.View>
            );
        }

        return (
            <RNKeyboardView
                ref="keyboardView"
                onStartShouldSetResponder={this._shouldSetResponder}
                style={styles.keyboard}
                visible={visible}
                height={height + (renderStickyView ? stickyViewHeight : 0)}>
                {content}
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
