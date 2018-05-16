package im.shimo.react.keyboard;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.facebook.react.uimanager.DisplayMetricsHolder;

/**
 * 全屏且adjustResize无效的解决方式
 */
public class AdjustResizeWithFullScreen {
    private final static String TAG = "AdjustResizeWith";
    private final Activity mActivity;
    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    private final int mStatusBarHeight;
    private final int mNavigationBarHeight;
    private View mChildOfContent;
    private int usableHeightPrevious;
    private int usableWidthPrevious;
    private static AdjustResizeWithFullScreen mInstance;

    private OnKeyboardStatusListener mListener;
    private static int mKeyboardHeight;
    private boolean mKeyboardOpened;
    private final int KEYBOARD_MIN_HEIGHT = 200;
    private Rect mVisibleViewArea = new Rect();
    private int mHeightPixels;

    public static View getDecorView() {
        return mInstance.mActivity.getWindow().getDecorView();
    }

    public static boolean isInit() {
        return mInstance != null;
    }

    public interface OnKeyboardStatusListener {
        void onKeyboardOpened();

        void onKeyboardClosed();

        boolean onKeyboardResize(int heightOfLayout, int bottom);
    }

    public static void assistRegisterActivity(Activity activity, int statusBarHeight, int navigationBarHeight, OnKeyboardStatusListener onKeyboardStatusListener) {
        if (mInstance == null) {
            mInstance = new AdjustResizeWithFullScreen(activity, statusBarHeight, navigationBarHeight, onKeyboardStatusListener);
        } else {
            mInstance.initData(onKeyboardStatusListener);
        }
    }

    public static void assistUnRegister(Activity activity) {
        if (mInstance != null) {
            if (mInstance.mChildOfContent != null && mInstance.mOnGlobalLayoutListener != null) {
                mInstance.mChildOfContent.getViewTreeObserver().removeOnGlobalLayoutListener(mInstance.mOnGlobalLayoutListener);
                mInstance.mChildOfContent.requestLayout();
                mInstance = null;
            }
        }
    }


    private AdjustResizeWithFullScreen(Activity activity, int statusBarHeight, int navigationBarHeight, OnKeyboardStatusListener onKeyboardStatusListener) {
        mActivity = activity;
        mStatusBarHeight = statusBarHeight;
        mNavigationBarHeight = navigationBarHeight;
        mHeightPixels = DisplayMetricsHolder.getScreenDisplayMetrics().heightPixels;
        mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                AdjustResizeWithFullScreen.this.possiblyResizeChildOfContent();
            }
        };
        initData(onKeyboardStatusListener);
    }

    private void initData(OnKeyboardStatusListener onKeyboardStatusListener) {
        if (onKeyboardStatusListener != null) {
            mListener = onKeyboardStatusListener;
        }
        FrameLayout content = (FrameLayout) mActivity.findViewById(android.R.id.content);
        mChildOfContent = content.getChildAt(0);
        mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        int mChildOfContentHeight = mChildOfContent.getBottom();
        //初始化
        if (mListener != null) {
            mListener.onKeyboardResize(mChildOfContentHeight, 0);
        }
    }

    private void possiblyResizeChildOfContent() {
        computeUsableHeight();
        int usableHeightNow = mVisibleViewArea.bottom;
        int usableWidthNow = mVisibleViewArea.right;
        if (usableHeightNow != usableHeightPrevious || usableWidthNow != usableWidthPrevious) {
            final int heightDifference = mHeightPixels - usableHeightNow;
            if (heightDifference > KEYBOARD_MIN_HEIGHT) {
                if (mKeyboardHeight != heightDifference) {
                    // keyboard is now showing, or the keyboard height has changed
                    // distance - safeAreaHeight = keyboardHeight
                    mKeyboardHeight = heightDifference - (mHeightPixels - mChildOfContent.getBottom());
                    if (KeyboardViewManager.DEBUG) {
                        Log.e(TAG, "mKeyboardHeight=" + mKeyboardHeight + ",usableHeightNow=" + usableHeightNow + ",mChildOfContentHeight=" + mChildOfContent.getRootView().getHeight());
                    }
                }
                if (!mKeyboardOpened) {
                    mKeyboardOpened = true;
                    if (mListener != null) {
                        mListener.onKeyboardOpened();
                    }
                }
                if (mListener != null) {
                    mListener.onKeyboardResize(usableHeightNow, 0);
                }
            } else {
                if (mKeyboardOpened) {
                    mKeyboardOpened = false;
                    if (mListener != null) {
                        mListener.onKeyboardClosed();
                    }
                }
                if (mListener != null) {
                    mListener.onKeyboardResize(usableHeightNow, 0);
                }
            }
            usableHeightPrevious = usableHeightNow;
            usableWidthPrevious = usableWidthNow;
        }
    }

    /**
     * 可绘区域全屏，或者可绘区域+键盘高度=全屏，则视为本逻辑意义里的全屏
     *
     * @return
     */
    public static boolean isFullscreen() {
        if (mInstance == null) return false;
        return mInstance.computeUsableHeight().bottom % mInstance.mHeightPixels == 0 ||
                (mInstance.computeUsableHeight().bottom + mKeyboardHeight) % mInstance.mHeightPixels == 0;
    }

    public static int getKeyboardHeight() {
        if (mInstance != null) {
            return mKeyboardHeight;
        }
        return 0;
    }

    /**
     * 可绘区域-顶点y坐标=剩余区域
     *
     * @param y
     * @return
     */
    public static int getRemainingHeight(int y) {
        if (mInstance == null) return 0;
        return mInstance.mVisibleViewArea.bottom - y;
    }

    public static int getWindowBottom() {
        if (mInstance != null) {
            return mInstance.mHeightPixels;
        }
        return 0;
    }

    public static int getUseBottom() {
        if (mInstance == null) return 0;
        return mInstance.computeUsableHeight().bottom;
    }

    public static int getUseRight() {
        if (mInstance == null) return 0;
        return mInstance.computeUsableHeight().right;
    }

    private Rect computeUsableHeight() {
        mChildOfContent.getWindowVisibleDisplayFrame(mVisibleViewArea);
        return mVisibleViewArea;
    }


}
