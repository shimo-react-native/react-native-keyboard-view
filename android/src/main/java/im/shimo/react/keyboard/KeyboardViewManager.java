package im.shimo.react.keyboard;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

@ReactModule(name = KeyboardViewManager.REACT_CLASS)
public class KeyboardViewManager extends ViewGroupManager<KeyboardView> {

    protected static final String REACT_CLASS = "RNKeyboardView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public KeyboardView createViewInstance(ThemedReactContext context) {
        return new KeyboardView(context);
    }

    @ReactProp(name = "height")
    public void setHeight(KeyboardView view, float height) {
        view.setHeight(height);
    }

    @ReactProp(name = "visible")
    public void setVisible(KeyboardView view, boolean visible) {
        view.setVisible(visible);

    }

    @Override
    public void onDropViewInstance(KeyboardView view) {
        view.onDropInstance();
    }
}
