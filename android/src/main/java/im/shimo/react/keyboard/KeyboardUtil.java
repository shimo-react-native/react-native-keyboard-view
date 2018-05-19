package im.shimo.react.keyboard;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import static android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT;

public class KeyboardUtil {

    public static void showKeyboard(final View view) {
        if (view == null) {
            return;
        }
        view.requestFocus();
        InputMethodManager inputManager =
                (InputMethodManager) view.getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(view, SHOW_IMPLICIT);
    }

    public static void showKeyboardOnTouch(final View view) {
        if (view == null) {
            return;
        }
        if (isKeyboardActive(view)) return;
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }, 200);
    }

    public static void hideKeyboard(final View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager) view.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean isKeyboardActive(final View view) {
        if (view == null) {
            return false;
        }
        InputMethodManager imm =
                (InputMethodManager) view.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm.isActive(view);
    }


}