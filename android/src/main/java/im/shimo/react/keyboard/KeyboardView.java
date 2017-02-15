package im.shimo.react.keyboard;


import java.util.ArrayList;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.JSTouchDispatcher;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;


public class KeyboardView extends ViewGroup implements LifecycleEventListener {

    private KeyboardRootViewGroup mHostView;
    private PopupWindow mWindow;
    private InputMethodManager mInputMethodManager;
    private Rect mVisibleViewArea;
    private int mMinKeyboardHeightDetected;
    private float mScale;
    private boolean mVisible;
    private @Nullable ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;

    public KeyboardView(Context context) {
        super(context);
        ((ReactContext) context).addLifecycleEventListener(this);
        mHostView = new KeyboardRootViewGroup(context, this);
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mVisibleViewArea = new Rect();
        mMinKeyboardHeightDetected = (int) PixelUtil.toPixelFromDIP(60);
        mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density;

        mWindow = new PopupWindow(mHostView);
        mWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        mWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing as we are laid out by UIManager
    }

    @Override
    public void addView(View child, int index) {
        mHostView.addView(child, index);
        showPopupWindow();
    }

    @Override
    public int getChildCount() {
        return mHostView.getChildCount();
    }

    @Override
    public View getChildAt(int index) {
        return mHostView.getChildAt(index);
    }

    @Override
    public void removeView(View child) {
        mHostView.removeView(child);
    }

    @Override
    public void removeViewAt(int index) {
        removeView(getChildAt(index));
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> outChildren) {
        // Explicitly override this to prevent accessibility events being passed down to children
        // Those will be handled by the mHostView which lives in the PopupWindow
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Explicitly override this to prevent accessibility events being passed down to children
        // Those will be handled by the mHostView which lives in the PopupWindow
        return false;
    }

    public void onDropInstance() {
        ((ReactContext) getContext()).removeLifecycleEventListener(this);
        dismissPopupWindow();
    }

    public void setHeight(float height) {
        mWindow.setHeight((int) (height * mScale));
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
        if (visible) {
            showPopupWindow();
        } else {
            dismissPopupWindow();
        }
    }

    @Override
    public void onHostResume() {
        // We show the PopupWindow again when the host resumes
        showPopupWindow();
    }

    @Override
    public void onHostPause() {
        // We dismiss the PopupWindow and reconstitute it onHostResume
        dismissPopupWindow();
    }

    @Override
    public void onHostDestroy() {
        // Drop the instance if the host is destroyed which will dismiss the PopupWindow
        onDropInstance();
    }

    protected void showPopupWindow() {
        if (!mWindow.isShowing() && mHostView.hasContent()) {
            if (mLayoutListener == null) {
                mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (checkKeyboardStatus()) {
                            mWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
                            mWindow.update();
                            hideContent();
                        } else {
                            mWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
                            mWindow.update();
                            showContent();
                        }

                    }
                };
            }

            getRootView().getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);

            if (checkKeyboardStatus()) {
                hideContent();
            } else {
                showContent();
            }

            if (mVisible) {
                mWindow.showAtLocation(getRootView(), Gravity.BOTTOM, 0, 0);
            }
        }
    }

    protected void dismissPopupWindow() {
        mWindow.dismiss();

        if (mLayoutListener != null) {
            getRootView().getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
            mLayoutListener = null;
        }
    }

    protected boolean checkKeyboardStatus() {
        getRootView().getWindowVisibleDisplayFrame(mVisibleViewArea);
        return DisplayMetricsHolder.getWindowDisplayMetrics().heightPixels - mVisibleViewArea.bottom > mMinKeyboardHeightDetected;
    }

    public void openKeyboard() {
        if (!checkKeyboardStatus()) {
            mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }

    public void closeKeyboard() {
        if (checkKeyboardStatus()) {
            mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }

    public void toggleKeyboard() {
        mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    public boolean close() {
        if (checkKeyboardStatus()) {
            mInputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            return true;
        }

        return false;
    }

    private void hideContent() {
        mHostView.setContentVisible(false);
    }

    private void showContent() {
        mHostView.setContentVisible(true);
    }

    private @Nullable ReactRootView mReactRootView;

    private ReactRootView getReactRootView() {
        if (mReactRootView == null) {
            ViewParent parent = getParent();

            while (parent != null && !(parent instanceof ReactRootView)) {
                parent = parent.getParent();
            }

            mReactRootView = (ReactRootView) parent;
        }

        return mReactRootView;
    }

    private @Nullable JSTouchDispatcher mJSTouchDispatcher;

    private int[] getOffset() {
        int[] rootOffset = new int[2];
        getReactRootView().getLocationInWindow(rootOffset);

        int[] windowPosition = new int[2];
        mWindow.getContentView().getLocationOnScreen(windowPosition);

        int[] offset = new int[2];
        offset[0] = windowPosition[0] - rootOffset[0];
        offset[1] = windowPosition[1] - rootOffset[1];

        return offset;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int[] offset = getOffset();
        event.setLocation(event.getX() + offset[0], event.getY() + offset[1]);
        getJSTouchDispatcher().handleTouchEvent(event, getEventDispatcher());
        return false;
    }

    private JSTouchDispatcher getJSTouchDispatcher() {
        if (mJSTouchDispatcher == null) {
            mJSTouchDispatcher = new JSTouchDispatcher(getReactRootView());
        }

        return mJSTouchDispatcher;
    }

    private EventDispatcher getEventDispatcher() {
        ReactContext reactContext = (ReactContext) getContext();
        return reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
    }

}
