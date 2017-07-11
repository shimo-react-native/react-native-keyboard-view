package im.shimo.react.keyboard;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;

public class KeyboardContentViewManager extends ViewGroupManager<KeyboardContentView> {
    protected static final String REACT_CLASS = "KeyboardContentView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public KeyboardContentView createViewInstance(ThemedReactContext context) {
        return new KeyboardContentView(context);
    }
}
