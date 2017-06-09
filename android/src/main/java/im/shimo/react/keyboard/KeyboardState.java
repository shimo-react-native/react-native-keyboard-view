package im.shimo.react.keyboard;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.PixelUtil;

public class KeyboardState {
    private Rect mVisibleViewArea;
    private int mMinKeyboardHeightDetected = (int) PixelUtil.toPixelFromDIP(60);
    private int mKeyboardHeight = 0;
    private boolean mKeyboardStatus = false;
    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;
    private boolean mToggleKeyboardManually = false;

    public KeyboardState(final View rootView, final OnKeyboardChangeListener listener) {
        ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        mVisibleViewArea = new Rect();

        mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mToggleKeyboardManually) {
                    mToggleKeyboardManually = false;
                    return;
                }

                rootView.getWindowVisibleDisplayFrame(mVisibleViewArea);
                mKeyboardHeight = DisplayMetricsHolder.getWindowDisplayMetrics().heightPixels - mVisibleViewArea.bottom;
                mKeyboardStatus = mKeyboardHeight > mMinKeyboardHeightDetected;

                if (mKeyboardStatus) {
                    listener.onKeyboardShown(mKeyboardHeight);
                } else {
                    listener.onKeyboardClosed();
                }

            }
        };

        mLayoutListener.onGlobalLayout();
        viewTreeObserver.addOnGlobalLayoutListener(mLayoutListener);
    }

    public int getKeyboardHeight() {
        return mKeyboardHeight;

    }

    public boolean getKeyboardStatus() {
        return mKeyboardStatus;
    }

    public interface OnKeyboardChangeListener {
        void onKeyboardShown(int keyboardSize);
        void onKeyboardClosed();
    }

    public void destroy(final View rootView) {
        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
    }

    public void willToggleKeyboardManually() {
        mToggleKeyboardManually = true;
    }
}
