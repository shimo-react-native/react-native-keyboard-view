package im.shimo.react.keyboard;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;

public class KeyboardCoverViewManager extends ViewGroupManager<KeyboardCoverView> {
    protected static final String REACT_CLASS = "KeyboardCoverView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public KeyboardCoverView createViewInstance(ThemedReactContext context) {
        return new KeyboardCoverView(context);
    }
}
