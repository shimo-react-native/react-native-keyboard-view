package im.shimo.react.keyboard;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

/**
 * 全屏且adjustResize无效的解决方式
 */
public class AdjustResizeWithFullScreen {
    private final Activity mActivity;
    private final ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;
    private final int mStatusBarHeight;
    private final int mNavigationBarHeight;
    private View mChildOfContent;
    private int usableHeightPrevious;
    private FrameLayout.LayoutParams frameLayoutParams;
    private static AdjustResizeWithFullScreen mInstance;
    public static volatile boolean ENABLE_ADJUSTRESIZE = true;

    private OnKeyboardStatusListener mListener;
    private static int mKeyboardHeight;
    private boolean mKeyboardOpened;
    private int mChildOfContentHeight;
    private int mDiffHeight;
    private final int KEYBOARD_MIN_HEIGHT = 200;
    private int mUseBottom;
    private boolean mOnPauseResize;

    public static int getUseBottom() {
        if (mInstance != null && mInstance.mChildOfContent != null) {
            if (mInstance.mUseBottom == 0) {
                int usableHeightSansKeyboard = mInstance.mChildOfContent.getRootView().getHeight();
                mInstance.mUseBottom = mInstance.optimizeHeight(usableHeightSansKeyboard,mInstance.computeUsableHeight());
            }
            return mInstance.mUseBottom;
        }
        return getWindowBottom();
    }

    public interface OnKeyboardStatusListener {
        void onKeyboardOpened();

        void onKeyboardClosed();

        boolean onKeyboardResize(int heightOfLayout, int bottom);
    }

    public static void assistRegisterActivity(Activity activity, int statusBarHeight, int navigationBarHeight) {
        if (mInstance == null) {
            mInstance = new AdjustResizeWithFullScreen(activity, statusBarHeight, navigationBarHeight, null);
        } else {
            mInstance.initData(null);
        }
//        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public static void assistRegisterActivity(Activity activity, int statusBarHeight, int navigationBarHeight, OnKeyboardStatusListener onKeyboardStatusListener) {
        if (mInstance == null) {
            mInstance = new AdjustResizeWithFullScreen(activity, statusBarHeight, navigationBarHeight, onKeyboardStatusListener);
        } else {
            mInstance.initData(onKeyboardStatusListener);
        }
//        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public static void assistUnRegister(Activity activity) {
        if (mInstance != null) {
            if (mInstance.mChildOfContent != null && mInstance.mOnGlobalLayoutListener != null) {
                mInstance.mChildOfContent.getViewTreeObserver().removeOnGlobalLayoutListener(mInstance.mOnGlobalLayoutListener);
                mInstance.frameLayoutParams.height = -1;
                mInstance.mChildOfContent.requestLayout();
                mInstance = null;
            }
        }
//        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    public static void onPauseResize() {
        if (mInstance != null) {
            mInstance.mOnPauseResize = true;
            if (mInstance.mChildOfContent != null && mInstance.mOnGlobalLayoutListener != null) {
                mInstance.frameLayoutParams.height = -1;
                mInstance.mDiffHeight = 0;
                mInstance.mUseBottom = 0;
                mInstance.usableHeightPrevious = 0;
                mInstance.frameLayoutParams.height = -1;
                if (mInstance.mKeyboardOpened) {
                    mInstance.mKeyboardOpened = false;
                    if (mInstance.mListener != null) {
                        mInstance.mListener.onKeyboardClosed();
                    }
                    mInstance.mChildOfContent.requestLayout();
                }
            }
        }
    }

    public static void onResumeResize() {
        if(mInstance!=null) {
            mInstance.mOnPauseResize = false;
        }
    }

    private AdjustResizeWithFullScreen(Activity activity, int statusBarHeight, int navigationBarHeight, OnKeyboardStatusListener onKeyboardStatusListener) {
        mActivity = activity;
        mStatusBarHeight = statusBarHeight;
        mNavigationBarHeight = navigationBarHeight;
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
        frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
        mChildOfContentHeight = mChildOfContent.getBottom();
        System.out.println("mChildOfContentHeight1=" + mChildOfContentHeight);
        //初始化
        if (mListener != null) {
            mListener.onKeyboardResize(mChildOfContentHeight, 0);
        }
    }

    /**
     * 是否为竖屏
     *
     * @return
     */
    private boolean isPortrait() {
        return mActivity.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    private int optimizeHeight(int usableHeightSansKeyboard, int usableHeightNow) {
        if (!mKeyboardOpened) {
            if (isPortrait()) {
                if (mStatusBarHeight + usableHeightNow + mNavigationBarHeight == usableHeightSansKeyboard) {
                    //有NavigationBar
                    return usableHeightNow + mStatusBarHeight;
                } else if (mStatusBarHeight + usableHeightNow == usableHeightSansKeyboard) {
                    //无圆角无NavigationBar
                    return mStatusBarHeight + usableHeightNow;
                } else if (usableHeightSansKeyboard - (mStatusBarHeight + usableHeightNow) > 0 && usableHeightSansKeyboard - (mStatusBarHeight + usableHeightNow) < mNavigationBarHeight) {
                    //有圆角无NavigationBar
                    return mStatusBarHeight + usableHeightNow;
                }
            } else {
                if (mStatusBarHeight + usableHeightNow == usableHeightSansKeyboard) {
                    return mStatusBarHeight + usableHeightNow;
                } else if (usableHeightNow == usableHeightSansKeyboard) {
                    return usableHeightNow;
                }
            }
        } else {
            if (isPortrait()) {
                if (mKeyboardHeight + mStatusBarHeight + usableHeightNow + mNavigationBarHeight == usableHeightSansKeyboard) {
                    //有NavigationBar
                    return usableHeightNow + mStatusBarHeight;
                } else if (mKeyboardHeight + mStatusBarHeight + usableHeightNow == usableHeightSansKeyboard) {
                    //无圆角无NavigationBar
                    return mStatusBarHeight + usableHeightNow;
                } else if (usableHeightSansKeyboard - (mStatusBarHeight + usableHeightNow + mKeyboardHeight) > 0 && usableHeightSansKeyboard - (mStatusBarHeight + usableHeightNow + mKeyboardHeight) < mNavigationBarHeight) {
                    //有圆角无NavigationBar
                    return mStatusBarHeight + usableHeightNow;
                }
            } else {
                if (mKeyboardHeight + mStatusBarHeight + usableHeightNow == usableHeightSansKeyboard) {
                    return mStatusBarHeight + usableHeightNow;
                } else if (mKeyboardHeight + usableHeightNow == usableHeightSansKeyboard) {
                    return usableHeightNow;
                }
            }
        }
        System.out.println("testest:elseelse≤");
        return usableHeightNow;
    }

    private void possiblyResizeChildOfContent() {
        if (!ENABLE_ADJUSTRESIZE||mOnPauseResize) {
            return;
        }
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
            System.out.println("usableHeightSansKeyboard="+usableHeightSansKeyboard);
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > mStatusBarHeight + mNavigationBarHeight) {
                if (!mKeyboardOpened) {
                    if (usableHeightPrevious - usableHeightNow > KEYBOARD_MIN_HEIGHT) {
                        mKeyboardHeight = usableHeightPrevious - usableHeightNow;
                        System.out.println("mKeyboardHeight=" + mKeyboardHeight);
                    }
                    mKeyboardOpened = true;
                    if (mListener != null) {
                        mListener.onKeyboardOpened();
                    }
                }
                int height = optimizeHeight(usableHeightSansKeyboard, usableHeightNow);
                if (frameLayoutParams.height == -1) {
                    frameLayoutParams.height = height;
                } else if(Math.abs(frameLayoutParams.height-height)==mNavigationBarHeight) {
                    if(height>frameLayoutParams.height) {
                        frameLayoutParams.height = height;
                    } else {
                        height = frameLayoutParams.height;
                    }
                }
                if (mListener != null) {
                    mListener.onKeyboardResize(height, 0);
                }
                mDiffHeight = height;
            } else {
                int tempDiff = mDiffHeight;
                if(tempDiff==0) {
                    tempDiff = optimizeHeight(usableHeightSansKeyboard, usableHeightNow);
                }
                if (mKeyboardOpened) {
                    frameLayoutParams.height = -1;
                    mKeyboardOpened = false;
                    if (mListener != null) {
                        mListener.onKeyboardClosed();
                    }
                }
                if (mListener != null) {
                    mListener.onKeyboardResize(tempDiff, 0);
                }
            }
            requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }

    public static void requestLayout() {
        if (mInstance != null && mInstance.mChildOfContent != null) {
            mInstance.mChildOfContent.requestLayout();
        }
    }

    public static int getKeyboardHeight() {
        if (mInstance != null) {
            return mKeyboardHeight;
        }
        return 0;
    }

    public static boolean isKeyboardOpened() {
        if (mInstance != null) {
            return mInstance.mKeyboardOpened;
        }
        return false;
    }

    public static int getWindowBottom() {
        if (mInstance != null) {
            return mInstance.mChildOfContent.getRootView().getHeight();
        }
        return 0;
    }

    private int computeUsableHeight() {
        Rect r = new Rect();
        mChildOfContent.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }
}
