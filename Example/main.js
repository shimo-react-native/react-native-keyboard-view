import React, { Component } from 'react';
import {
    AppRegistry,
    StyleSheet,
    Text,
    View,
    TextInput,
    TouchableHighlight,
    ScrollView
} from 'react-native';

import KeyboardView from 'react-native-keyboard-view';

const KEYBOARD_VIEW = 'KEYBOARD_VIEW';

class Keyboard extends Component {

    _open() {
        this.refs[KEYBOARD_VIEW].open();
    }

    _close() {
        this.refs[KEYBOARD_VIEW].close();
    }

    _toggle() {
        this.refs[KEYBOARD_VIEW].toggleKeyboard();
    }

    _hide() {
        this.refs[KEYBOARD_VIEW].hideKeyboard();
    }

    _show() {
        this.refs[KEYBOARD_VIEW].showKeyboard();
    }

    _blur() {
        this.refs.input.blur();
    }

    _renderStickyView() {
        return (
            <View style={styles.stickyView}>
                <TouchableHighlight
                    style={styles.stickyViewButton}
                    onPress={() => {}}
                    underlayColor="#ccc"
                >
                    <Text style={styles.buttonText}>1</Text>
                </TouchableHighlight>
                <TouchableHighlight
                    style={styles.stickyViewButton}
                    onPress={() => {}}
                    underlayColor="#ccc"
                >
                    <Text style={styles.buttonText}>2</Text>
                </TouchableHighlight>
            </View>
        );
    }

    render() {
        return (
            <View style={styles.container}>
                <View style={styles.actions}>
                    <TouchableHighlight
                        style={styles.button}
                        onPress={this._open.bind(this)}
                        underlayColor="#ccc"
                    >
                        <View style={styles.buttonContent}>
                            <Text style={styles.buttonText}>OPEN</Text>
                        </View>
                    </TouchableHighlight>
                    <TouchableHighlight
                        style={styles.button}
                        onPress={this._close.bind(this)}
                        underlayColor="#ccc"
                    >
                        <View style={styles.buttonContent}>
                            <Text style={styles.buttonText}>CLOSE</Text>
                        </View>
                    </TouchableHighlight>
                    <TouchableHighlight
                        style={styles.button}
                        onPress={this._toggle.bind(this)}
                        underlayColor="#ccc"
                    >
                        <View style={styles.buttonContent}>
                            <Text style={styles.buttonText}>TOGGLE KEYBOARD</Text>
                        </View>
                    </TouchableHighlight>
                    <TouchableHighlight
                        style={styles.button}
                        onPress={this._hide.bind(this)}
                        underlayColor="#ccc"
                    >
                        <View style={styles.buttonContent}>
                            <Text style={styles.buttonText}>HIDE KEYBOARD</Text>
                        </View>
                    </TouchableHighlight>
                    <TouchableHighlight
                        style={styles.button}
                        onPress={this._show.bind(this)}
                        underlayColor="#ccc"
                    >
                        <View style={styles.buttonContent}>
                            <Text style={styles.buttonText}>SHOW KEYBOARD</Text>
                        </View>
                    </TouchableHighlight>
                    <TouchableHighlight
                        style={styles.button}
                        onPress={this._blur.bind(this)}
                        underlayColor="#ccc"
                    >
                        <View style={styles.buttonContent}>
                            <Text style={styles.buttonText}>BLUR</Text>
                        </View>
                    </TouchableHighlight>
                </View>
                <TextInput
                    ref="input"
                    underlineColorAndroid="transparent"
                    placeholder="empty"
                    style={styles.input}
                />
                <KeyboardView
                    ref={KEYBOARD_VIEW}
                    height={300}
                    stickyViewHeight={40}
                    renderStickyView={this._renderStickyView}>
                        <ScrollView style={{flex: 1}}>
                            <View style={styles.keyboard}>
                                <Text style={styles.keyboardText}>KEYBOARD REPLACEMENT</Text>
                            </View>
                            <TouchableHighlight
                                style={styles.button}
                                onPress={this._blur.bind(this)}
                                underlayColor="#ccc"
                            >
                                <View style={styles.buttonContent}>
                                    <Text style={styles.buttonText}>BLUR</Text>
                                </View>
                            </TouchableHighlight>
                        </ScrollView>
                </KeyboardView>
            </View>
        );
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        backgroundColor: '#F5FCFF',
    },

    actions: {
        justifyContent: 'space-around',
        flexDirection: 'row',
        flexWrap: 'wrap'
    },

    button: {
        borderRadius: 5,
        backgroundColor: '#aaa',
        paddingVertical: 5,
        paddingHorizontal: 10,
        height: 30,
        marginHorizontal: 10,
        marginBottom: 10
    },
    buttonContent: {
        alignItems: 'center',
        justifyContent: 'center'
    },
    buttonText: {
        textAlign: 'center',
        color: '#eee',
        marginBottom: 5,
    },
    input: {
        width: 150,
        height: 30,
        paddingHorizontal: 5,
        marginBottom: 5,
        borderWidth: StyleSheet.hairlineWidth,
        alignSelf: 'center'
    },

    keyboard: {
        height: 300,
        alignItems: 'center',
        justifyContent: 'center'
    },

    keyboardText: {
        fontSize: 24,
        fontWeight: 'bold'
    },

    stickyView: {
        height: 40,
        backgroundColor: 'red',
        flexDirection: 'row'
    },

    stickyViewButton: {
        height: 40,
        marginHorizontal: 10,
        paddingHorizontal: 20,
        backgroundColor: '#aaa',
        justifyContent: 'center'
    }
});

AppRegistry.registerComponent('keyboard', () => Keyboard);
