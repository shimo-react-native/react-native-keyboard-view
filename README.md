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

* iOS (React Native > 0.60)

```bash
cd ios && pod install
```

* Manual link steps (React Native < 0.60)

```bash
react-native link react-native-keyboard-view
```


### Usage


```javascript
import KeyboardView from 'react-native-keyboard-view';

class Example extends Component {
    
    close() {
        KeyboardView.dismiss();
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
                onShow={() => console.log('onShow')}
                onHide={() => console.log('onHide')}
                renderStickyView={this._renderStickyView}
                renderCover={this._renderCover}
            >
                <View style={{flex: 1}} />
            </KeyboardView>
        );
    }
}

```

