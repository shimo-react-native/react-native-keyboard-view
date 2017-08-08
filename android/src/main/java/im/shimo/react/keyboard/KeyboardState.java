package im.shimo.react.keyboard;

import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;

import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.PixelUtil;

import java.util.ArrayList;

class KeyboardState {
    private Rect mVisibleViewArea;
    private int mMinKeyboardHeightDetected = (int) PixelUtil.toPixelFromDIP(60);
    private Rect mKeyboardFrame;
    private boolean mKeyboardShowing = false;
    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;
    private ArrayList<OnKeyboardChangeListener> mOnKeyboardChangeListeners;
    private DisplayMetrics mWindowDisplayMetrics;

    KeyboardState(final View rootView) {
        ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        mWindowDisplayMetrics = DisplayMetricsHolder.getWindowDisplayMetrics();
        mVisibleViewArea = new Rect();
        mOnKeyboardChangeListeners = new ArrayList<>();

        mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getWindowVisibleDisplayFrame(mVisibleViewArea);

                Rect keyboardFrame = new Rect(0, mVisibleViewArea.bottom, mWindowDisplayMetrics.widthPixels, mWindowDisplayMetrics.heightPixels);


                if (keyboardFrame.equals(mKeyboardFrame)) {
                    return;
                } else {
                    mKeyboardFrame = keyboardFrame;
                }


                mKeyboardShowing = keyboardFrame.height() > mMinKeyboardHeightDetected;

                for (int i = 0; i < mOnKeyboardChangeListeners.size(); i++) {
                    OnKeyboardChangeListener listener = mOnKeyboardChangeListeners.get(i);
                    if (mKeyboardShowing) {
                        listener.onKeyboardShown(keyboardFrame);
                    } else {
                        listener.onKeyboardClosed();
                    }
                }

            }
        };

        mLayoutListener.onGlobalLayout();
        viewTreeObserver.addOnGlobalLayoutListener(mLayoutListener);
    }

    void addOnKeyboardChangeListener(OnKeyboardChangeListener listener) {
        mOnKeyboardChangeListeners.add(listener);
    }

    void removeOnKeyboardChangeListener(OnKeyboardChangeListener listener) {
        mOnKeyboardChangeListeners.remove(listener);
    }

    Rect getKeyboardFrame() {
        return mKeyboardFrame;
    }

    Rect getVisibleViewArea() {
        return mVisibleViewArea;
    }

    boolean isKeyboardShowing() {
        return mKeyboardShowing;
    }

    interface OnKeyboardChangeListener {
        void onKeyboardShown(Rect keyboardFrame);
        void onKeyboardClosed();
    }

}
