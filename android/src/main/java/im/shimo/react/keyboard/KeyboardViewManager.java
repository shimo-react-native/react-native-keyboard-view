package im.shimo.react.keyboard;

import android.webkit.WebView;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

@ReactModule(name = KeyboardViewManager.REACT_CLASS)
public class KeyboardViewManager extends ViewGroupManager<KeyboardView> {

    protected static final String REACT_CLASS = "KeyboardView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public KeyboardView createViewInstance(ThemedReactContext context) {
        return new KeyboardView(context);
    }

    @Override
    public LayoutShadowNode createShadowNodeInstance() {
        return new KeyboardViewShadowView();
    }

    @Override
    public void onDropViewInstance(KeyboardView view) {
        super.onDropViewInstance(view);
        view.onDropInstance();
    }

    @ReactProp(name = "hideWhenKeyboardIsDismissed")
    public void setHideWhenKeyboardIsDismissed(KeyboardView view, boolean hideWhenKeyboardIsDismissed) {
        view.setHideWhenKeyboardIsDismissed(hideWhenKeyboardIsDismissed);
    }

    @ReactProp(name = "contentVisible")
    public void setContentVisible(KeyboardView view, boolean contentVisible) {
        view.setContentVisible(contentVisible);
    }

    @ReactProp(name = "keyboardPlaceholderHeight")
    public void setKeyboardPlaceholderHeight(KeyboardView view, int keyboardPlaceholderHeight) {
        view.setKeyboardPlaceholderHeight(keyboardPlaceholderHeight);
    }

    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (KeyboardView.Events event : KeyboardView.Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }
}
