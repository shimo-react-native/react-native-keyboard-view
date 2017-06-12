package im.shimo.react.keyboard;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

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
        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
