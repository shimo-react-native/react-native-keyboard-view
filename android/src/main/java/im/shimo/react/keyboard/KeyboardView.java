package im.shimo.react.keyboard;


import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;


public class KeyboardView extends ViewGroup implements LifecycleEventListener {

    private KeyboardRootViewGroup mHostView;
    private PopupWindow mWindow;
    private InputMethodManager mInputMethodManager;
    private boolean mContentVisible;

    private KeyboardState mKeyboardState;
    private static ArrayList<KeyboardView> mKeyboardViews;


    public KeyboardView(Context context) {
        super(context);
        ((ReactContext) context).addLifecycleEventListener(this);

        if (mKeyboardViews == null) {
            mKeyboardViews = new ArrayList<>();
        }

        mHostView = new KeyboardRootViewGroup(context, new Callback() {
            @Override
            public void invoke(Object... args) {
                onTouchEvent((MotionEvent)args[0]);
            }
        });

        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        mWindow = new PopupWindow(mHostView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mWindow.setAnimationStyle(R.style.DialogAnimationSlide);
        mWindow.setOutsideTouchable(true);


        mWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);

        mKeyboardViews.add(this);

        mKeyboardState = new KeyboardState(getRootView(), new KeyboardState.OnKeyboardChangeListener() {
            @Override
            public void onKeyboardShown(int keyboardSize) {
                showPopupWindow();
                mHostView.setContentHeight(keyboardSize);
            }

            @Override
            public void onKeyboardClosed() {
                dismissPopupWindow();
            }
        });
    }

    public static void dismiss() {
        if (mKeyboardViews != null) {
            for (int i = 0; i < mKeyboardViews.size(); i++) {
                KeyboardView keyboardView = mKeyboardViews.get(i);
                keyboardView.closeKeyboard();
                keyboardView.dismissPopupWindow();
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing as we are laid out by UIManager
    }

    @Override
    public void addView(View child, int index) {
        mHostView.addView(child, index);
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
        dismissPopupWindow();
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
        mKeyboardState.destroy(getRootView());
    }

    @Override
    public void onHostResume() {
        // do nothing
    }

    @Override
    public void onHostPause() {
        // do nothing
    }

    @Override
    public void onHostDestroy() {
        // Drop the instance if the host is destroyed which will dismiss the PopupWindow
        onDropInstance();
    }

    protected void showPopupWindow() {
        if (!mWindow.isShowing()) {
            mWindow.showAtLocation(getRootView(), Gravity.FILL, 0, 0);
        }
    }

    protected void dismissPopupWindow() {
        mWindow.dismiss();
        mContentVisible = false;
    }

    public void openKeyboard() {
        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void closeKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
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
        getReactRootView().dispatchTouchEvent(event);

        return false;
    }

    public void setContentVisible(boolean contentVisible) {
        if (mContentVisible != contentVisible) {
            mContentVisible = contentVisible;

            if (mWindow.isShowing()) {
                mHostView.post(new Runnable() {
                    @Override
                    public void run() {
                        mKeyboardState.willToggleKeyboardManually();
                        if (mContentVisible) {
                            closeKeyboard();
                        } else {
                            openKeyboard();
                        }
                    }
                });
            }
        }
    }
}
