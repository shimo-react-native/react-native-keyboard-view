package im.shimo.react.keyboard;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

@ReactModule(name = KeyboardModule.NAME)
public class KeyboardModule extends ReactContextBaseJavaModule {

    /* package */ static final String FRAGMENT_TAG =
            "im.shimo.react.keyboard.KeyboardModule";

    /* package */ static final String NAME = "RNKeyboardModule";

    KeyboardModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void closeKeyboard(final int tag, final Promise promise) {
        getReactApplicationContext().getNativeModule(UIManagerModule.class).addUIBlock(new UIBlock() {
            public void execute (NativeViewHierarchyManager manager) {
                KeyboardView view = (KeyboardView) manager.resolveView(tag);
                promise.resolve(view.close());
            }
        });
    }
}
