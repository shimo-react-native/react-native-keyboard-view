package im.shimo.react.keyboard;
import android.app.Activity;

@ReactModule(name = RNPromptModule.NAME)
public class RNKeyboardModule extends ReactContextBaseJavaModule {

    /* package */ static final String FRAGMENT_TAG =
            "im.shimo.react.keyboard.RNKeyboardModule";

    /* package */ static final String NAME = "RNKeyboardService";

    public RNKeyboardModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
