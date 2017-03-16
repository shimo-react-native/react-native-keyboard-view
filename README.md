# react-native-keyboard-view
KeyboardView Library for react-native

## Supports:

only supports: react-native >= 0.42.0

platform:

ios √  
android √   

![Example](https://github.com/shimohq/react-native-keyboard-view/raw/master/preview.gif)

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

