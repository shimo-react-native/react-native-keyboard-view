package im.shimo.react.keyboard;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.facebook.react.uimanager.DisplayMetricsHolder;

import java.util.ArrayList;

/**
 * Created by song on 2018/1/17.
 */

public abstract class AbstractKeyboardState {
    /**
     * 布局可控RECT
     */
    protected Rect mVisibleViewArea;
    /**
     * 预测键盘Rect，或者NavigationBar高度
     */
    protected Rect mKeyboardFrame;
    /**
     * 键盘是否显示
     */
    protected boolean mKeyboardShowing = false;
    protected ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;
    protected ArrayList<OnKeyboardChangeListener> mOnKeyboardChangeListeners;
    protected DisplayMetrics mWindowDisplayMetrics;
    /**
     * 相对于第三方rom厂商来说NavigationBar是否显示状态
     */
    protected boolean mIsRomNavigationBarShow;
    /**
     * 系统真正的NavigationBar是否显示状态
     */
    protected boolean mIsRealNavigationBarShow;
    /**
     * 是否为初始化状态
     */
    protected boolean mIsFirst;
    /**
     * 高度变化之前的高度,用于拦截无效的变化
     */
    protected int usableHeightPrevious;

    protected View mRootView;
    /**
     * 忽略横竖屏,是否显示NavigationBar
     */
    protected boolean mIsNavigationbarShow;

    /**
     * 是否显示了NavigationBar
     *
     * @param context
     * @param viewHeight rootView的高度
     * @return
     */
    protected void isRomNavigationBarShow(Context context, int viewHeight, int viewWidth) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            //获取真实的屏幕高度
            display.getRealSize(realSize);
            if (viewHeight != 0) {
                if (viewHeight == realSize.y) {
                    mIsRomNavigationBarShow = false;
                    if (viewWidth == realSize.x) {
                        mIsNavigationbarShow = false;
                    } else if (viewWidth < realSize.x) {
                        mIsNavigationbarShow = true;
                    }
                } else if (viewHeight < realSize.y) {
                    mIsRomNavigationBarShow = true;
                    mIsNavigationbarShow = true;
                }
            }
            display.getSize(size);
            mIsRealNavigationBarShow = (realSize.y != size.y);
        } else {
            boolean menu = ViewConfiguration.get(context).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            if (menu || back) {
                mIsRomNavigationBarShow = mIsRealNavigationBarShow = false;
            } else {
                mIsRomNavigationBarShow = mIsRealNavigationBarShow = true;
            }
        }
    }


    AbstractKeyboardState(final View rootView, final int navigationBarHeight, final int statusBarHeight) {
        mRootView = rootView;
        ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        mWindowDisplayMetrics = DisplayMetricsHolder.getScreenDisplayMetrics();
        mVisibleViewArea = new Rect();
        mOnKeyboardChangeListeners = new ArrayList<>();
        mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                onGlobalLayoutChanged(navigationBarHeight, statusBarHeight);
            }
        };

        mLayoutListener.onGlobalLayout();
        viewTreeObserver.addOnGlobalLayoutListener(mLayoutListener);
    }

    void onGlobalLayoutChanged(int navigationBarHeight, int statusBarHeight) {
        mIsFirst = false;
        if (mVisibleViewArea.bottom == 0) {
            mIsFirst = true;
        }
        mRootView.getWindowVisibleDisplayFrame(mVisibleViewArea);
        if (mIsFirst) {
            //初始化状态
            isRomNavigationBarShow(mRootView.getContext(), mVisibleViewArea.bottom, mVisibleViewArea.right);
        }
        //现在的高度值
        int usableHeightNow = mVisibleViewArea.bottom - mVisibleViewArea.top;
        //当前可见高度和上一次可见高度不一致 布局变动
        if (usableHeightNow != usableHeightPrevious) {
            usableHeightPrevious = usableHeightNow;
            Rect keyboardFrame = new Rect(0, mVisibleViewArea.bottom, mWindowDisplayMetrics.widthPixels, mWindowDisplayMetrics.heightPixels);
            if (keyboardFrame.equals(mKeyboardFrame)) {
                return;
            }
            //以下代码执行顺序不可改变
            if (mIsRomNavigationBarShow) {
                //需要探测是否超出了NavigationBar的高度
                mKeyboardShowing = keyboardFrame.height() > navigationBarHeight;
            } else {
                //需要探测是否超出了一定的高度，这里直接给statuBar的height
                mKeyboardShowing = keyboardFrame.height() > statusBarHeight;
            }
            keyboardFrame = dealKeyBoardFrame(keyboardFrame, navigationBarHeight, statusBarHeight);
            mKeyboardFrame = keyboardFrame;
            onDoListener(keyboardFrame);
        }
    }

    private void onDoListener(Rect keyboardFrame) {
        for (int i = 0; i < mOnKeyboardChangeListeners.size(); i++) {
            OnKeyboardChangeListener listener = mOnKeyboardChangeListeners.get(i);
            if (mKeyboardShowing) {
                listener.onKeyboardShown(keyboardFrame);
            } else {
                listener.onKeyboardClosed();
            }
        }
    }

    protected abstract Rect dealKeyBoardFrame(Rect keyboardFrame, int navigationBarHeight, int statusBarHeight);

    public abstract int checkExtraHeight(int navigationBarHeight);

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

    boolean isRealNavigationBarShow() {
        return mIsRealNavigationBarShow;
    }

    boolean isRomNavigationBarShow() {
        return mIsRomNavigationBarShow;
    }

    /**
     * 忽略横竖屏,是否显示NavigationBar
     */
    boolean isNavigationBarShow() {
        return mIsNavigationbarShow;
    }

    boolean isKeyboardShowing() {
        return mKeyboardShowing;
    }

    boolean isInitDataCompelete() {
        return !mIsFirst;
    }

    interface OnKeyboardChangeListener {
        void onKeyboardShown(Rect keyboardFrame);

        void onKeyboardClosed();
    }

}
