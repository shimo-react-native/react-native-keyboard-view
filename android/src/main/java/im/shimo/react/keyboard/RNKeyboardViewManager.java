package im.shimo.react.keyboard;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

@ReactModule(name = RNKeyboardViewManager.REACT_CLASS)
public class RNKeyboardViewManager extends ViewGroupManager<RNKeyboardView> {

    protected static final String REACT_CLASS = "RNKeyboardView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RNKeyboardView createViewInstance(ThemedReactContext context) {
        return new RNKeyboardView(context);
    }

    @ReactProp(name = "height")
    public void setHeight(RNKeyboardView view, float height) {
        view.setHeight(height);
    }

    @ReactProp(name = "visible")
    public void setVisible(RNKeyboardView view, boolean visible) {
        view.setVisible(visible);

    }

    @Override
    public void onDropViewInstance(RNKeyboardView view) {
        view.onDropInstance();
    }
}
