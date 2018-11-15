package im.shimo.react.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

@ReactModule(name = KeyboardViewManager.REACT_CLASS)
public class KeyboardViewManager extends ViewGroupManager<KeyboardView> {
    public final static boolean DEBUG = false;
    private int navigationBarHeight;
    private int statusBarHeight;
    protected static final String REACT_CLASS = "KeyboardView";
    static KeyboardViewManager INSTANCE;
    private final static String TAG = "KeyboardViewManager";

    public KeyboardViewManager() {
        INSTANCE = this;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    ThemedReactContext mThemedReactContext;

    @Override
    public KeyboardView createViewInstance(ThemedReactContext context) {
        mThemedReactContext = context;
        if (statusBarHeight == 0) {
            navigationBarHeight = getNavigationBarHeight(context);
            statusBarHeight = getStatusBarHeight(context);
        }
        return new KeyboardView(context, navigationBarHeight, statusBarHeight);
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
        if (DEBUG) {
            Log.e(TAG, "KeyboardViewManager.setHideWhenKeyboardIsDismissed=" + hideWhenKeyboardIsDismissed);
        }
    }


    @ReactProp(name = "contentVisible")
    public void setContentVisible(KeyboardView view, boolean contentVisible) {
        view.setContentVisible(contentVisible);
        if (DEBUG) {
            Log.e(TAG, "KeyboardViewManager.setContentVisible=" + contentVisible);
        }
    }

    @ReactProp(name = "fullWhenKeyboardDisplay")
    public void setFullWhenKeyboardDisplay(KeyboardView view,boolean fullWhenKeyboardDisplay){
        view.setFullWhenKeyboardDisplay(fullWhenKeyboardDisplay);
        if (DEBUG) {
            Log.e(TAG, "KeyboardViewManager.setFullWhenKeyboardDisplay=" + fullWhenKeyboardDisplay);
        }
    }

    @ReactProp(name = "inNative")
    public void setInNative(KeyboardView view,boolean inNative){
        view.setInNative(inNative);
        if (DEBUG) {
            Log.e(TAG, "KeyboardViewManager.setInNative=" + inNative);
        }
    }

    @ReactProp(name = "keyboardPlaceholderHeight")
    public void setKeyboardPlaceholderHeight(KeyboardView view, int keyboardPlaceholderHeight) {
        view.setKeyboardPlaceholderHeight(keyboardPlaceholderHeight);
        if (DEBUG) {
            Log.e(TAG, "KeyboardViewManager.setKeyboardPlaceholderHeight=" + keyboardPlaceholderHeight);
        }
    }

    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (KeyboardView.Events event : KeyboardView.Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    public static int getTitleBarHeight(Context context) {
        TypedArray styledAttributes = context.obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return actionBarSize;
    }

    static float getNavigationSize() {
        return INSTANCE.navigationBarHeight;
    }

}
