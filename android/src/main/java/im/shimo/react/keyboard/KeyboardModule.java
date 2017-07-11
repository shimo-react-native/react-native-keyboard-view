package im.shimo.react.keyboard;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

@ReactModule(name = KeyboardModule.NAME)
public class KeyboardModule extends ReactContextBaseJavaModule {
    /* package */ static final String NAME = "KeyboardViewModule";

    private InputMethodManager mInputMethodManager;

    KeyboardModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mInputMethodManager = (InputMethodManager) reactContext.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void dismiss() {
        getReactApplicationContext().getNativeModule(UIManagerModule.class).addUIBlock(new UIBlock() {
            public void execute (NativeViewHierarchyManager nativeViewHierarchyManager) {
                Activity activiy = getCurrentActivity();

                if (activiy != null) {
                    View focus = activiy.getWindow().getCurrentFocus();

                    if (focus != null) {
                        focus.clearFocus();
                        mInputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }

                }
            }
        });

    }
}
