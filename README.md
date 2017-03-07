# react-native-keyboard-view
KeyboardView Library for react-native

## Supports:

version:
only supports: react-native >= 0.42.0

platform:
ios √
android (partial)

![Example](./preview.gif)

### Installation

* Install from npm

```bash
npm i react-native-keyboard-view --save
```

* Link native library

```bash
react-native link react-native-keyboard-view
```


### Usage

iOS:

```javascript
import KeyboardView from 'react-native-keyboard-view';

class Example extends Component {
    
    close() {
        this.refs.keyboard.close();
    };
    
    _renderStickyView() {
        return (
            <View style={{height: 40}}><Text>BUTTON</Text></View>
        );
    };
    
    _renderCover() {
        return (
            <View style={{flex: 1, backgroundColor: 'rgba(0, 0, 0, 0.25)'}} />
        );
    };
    
    render() {
        return (
            <KeyboardView
                ref="keyboard"
                onShow={(state, height) => console.log('onShow', state, height)}
                onHide={(state) => console.log('onHide', state)}
                onKeyboardChanged={(state, height) => console.log('onKeyboardChanged', state, height)}
                renderStickyView={this._renderStickyView}
                renderCover={this._renderCover}
                backgroundColor="rgba(0, 0, 0, 0.25)"
                initialState={true}>
                <View style={{flex: 1}} />
            </KeyboardView>
        );
    }
}

```

Android: (not finished yet)

```javascript
import KeyboardView from 'react-native-keyboard-view';

<KeyboardView
    ref="keyboard"
    height={300}
    backgroundColor="#fff"
    onShow={(state, height) => console.log('onShow', state, height)}
    onHide={(state) => console.log('onHide', state)}
    onKeyboardChanged={(state, height) => console.log('onKeyboardChanged', state, height)}
    renderStickyView={this._renderStickyView}>
        <ScrollView
            keyboardShouldPersistTaps="always"
            style={{flex: 1,  backgroundColor: '#fff'}}>
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
```
