package im.shimo.react.keyboard;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/**
 * Created by song on 2018/4/12.
 */

class KeyboardStateVivoX21OS extends AbstractKeyboardState {
    /**
     * Vivo官方文档说明的圆角高度
     */
    private int mRadioReact;

    public KeyboardStateVivoX21OS(View rootView, int navigationBarHeight, int statusBarHeight) {
        super(rootView, navigationBarHeight, statusBarHeight);
    }

    /**
     * 是否显示了NavigationBar
     *
     * @param context
     * @param viewHeight rootView的高度
     * @return
     */
    protected void isRomNavigationBarShow(Context context, int viewHeight, int viewWidth) {
        if (mRadioReact == 0) {
            mRadioReact = mRootView.getResources().getDimensionPixelSize(R.dimen.vivo_r_height);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            //获取真实的屏幕高度
            display.getRealSize(realSize);
            //以下屏幕高度减去圆角高度才能计算出可布局高度
            if (viewHeight != 0) {
                if (viewHeight == realSize.y - mRadioReact) {
                    mRomNavigationBarShow = false;
                    if (viewWidth == realSize.x - mRadioReact) {
                        mNavigationbarShow = false;
                    } else if (viewWidth < realSize.x - mRadioReact) {
                        mNavigationbarShow = true;
                    }
                } else if (viewHeight < realSize.y - mRadioReact) {
                    mRomNavigationBarShow = true;
                    mNavigationbarShow = true;
                }
            }
            display.getSize(size);
            mRealNavigationBarShow = (realSize.y != (size.y + mRadioReact));
        } else {
            boolean menu = ViewConfiguration.get(context).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            if (menu || back) {
                mRomNavigationBarShow = mRealNavigationBarShow = mNavigationbarShow = false;
            } else {
                mRomNavigationBarShow = mRealNavigationBarShow = mNavigationbarShow = true;
            }
        }
    }

    @Override
    protected Rect dealKeyBoardFrame(Rect keyboardFrame, int navigationBarHeight, int statusBarHeight) {
        if (mKeyboardShowing) {
            //兼容非原生厂商rom隐藏navigation
            if (!mRomNavigationBarShow) {
                keyboardFrame.top += mRadioReact;
            } else {
                //rootView高度=屏幕绘制高度-NavigationBar高度
                if (mRealNavigationBarShow) {
                    //系统NavigationBar显示
                    //说明键盘弹起时，高度需要增加NavigationBar高度
                    // ，因为键盘弹起时，NavigationBar会跟着键盘一起出现
                    keyboardFrame.top += navigationBarHeight;
                } else {
                    //系统NavigationBar高度不显示
                    //说明键盘弹起时不需要增加任何高度
                }
            }
        } else {
            if (!mRomNavigationBarShow) {
                keyboardFrame.top += mRadioReact;
            } else {

            }
        }
        return keyboardFrame;
    }

    @Override
    public int checkExtraHeight(int navigationBarHeight) {
        int temp = 0;
        if (isRealNavigationBarShow()) {
            if (!isRomNavigationBarShow()) {
                temp = mRadioReact;
            } else {
                temp = navigationBarHeight;
            }
        }
        return temp;
    }
}
