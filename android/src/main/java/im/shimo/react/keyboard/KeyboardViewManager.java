package im.shimo.react.keyboard;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;

@ReactModule(name = KeyboardViewManager.REACT_CLASS)
public class KeyboardViewManager extends ViewGroupManager<KeyboardView> {

    protected static final String REACT_CLASS = "KeyboardView";
    private KeyboardState mKeyboardState;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public KeyboardView createViewInstance(ThemedReactContext context) {
        if (mKeyboardState == null && context.getCurrentActivity() != null) {
            mKeyboardState = new KeyboardState(context.getCurrentActivity().findViewById(android.R.id.content));
        }

        return new KeyboardView(context, mKeyboardState);
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
}
