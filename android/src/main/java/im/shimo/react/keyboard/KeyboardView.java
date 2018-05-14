package im.shimo.react.keyboard;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.PopupWindow;

import com.facebook.drawee.BuildConfig;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.ReactShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import java.util.ArrayList;


/**
 * ContentView is layout to cover the keyboard,
 * CoverView is layout to fill the rest part on the screen.
 * <p>
 * +--------------+
 * |              |
 * |              |
 * |     cover    |
 * |     view     |
 * |              |
 * |              |
 * |--------------|
 * |   (keyboard) |
 * |    content   |
 * |     view     |
 * |              |
 * +--------------+
 */


public class KeyboardView extends ReactRootAwareViewGroup implements LifecycleEventListener, AdjustResizeWithFullScreen.OnKeyboardStatusListener {
    private final static String TAG = "KeyboardView";
    private final ThemedReactContext mThemedContext;
    private final UIManagerModule mNativeModule;
    private @Nullable
    volatile
    KeyboardCoverView mCoverView;
    private @Nullable
    volatile
    KeyboardContentView mContentView;
    private int navigationBarHeight;
    private int statusBarHeight;
    private RCTEventEmitter mEventEmitter;
    private int mKeyboardPlaceholderHeight;
    private float mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
    private boolean mContentVisible = true;
    private boolean mHideWhenKeyboardIsDismissed = true;
    // whether keyboard is shown
    private boolean mKeyboardShown = false;
    private volatile int mChildCount;
    private View.OnFocusChangeListener mOnFocusChangeListener;
    /**
     * 光标焦点
     */
    private View mEditFocusView;
    /**
     * 是否为初始化
     */
    private volatile boolean initWhenAttached;
    private PopupWindow mContentViewPopupWindow;
    private int mMinContentViewHeight = 256;
    private boolean mKeyboardShownStatus;
    private int mCoverViewBottom;
    private int mUseBottom;

    public enum Events {
        EVENT_SHOW("onKeyboardShow"),
        EVENT_HIDE("onKeyboardHide");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public KeyboardView(final ThemedReactContext context, int navigationBarHeight, int statusBarHeight) {
        super(context);
        this.mThemedContext = context;
        this.mNativeModule = mThemedContext.getNativeModule(UIManagerModule.class);
        this.navigationBarHeight = navigationBarHeight;
        this.statusBarHeight = statusBarHeight;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        context.addLifecycleEventListener(this);

        mContentViewPopupWindow = new PopupWindow();
        mContentViewPopupWindow.setAnimationStyle(R.style.DialogAnimationSlide);
        mContentViewPopupWindow.setClippingEnabled(false);
        mContentViewPopupWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        mContentViewPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mContentViewPopupWindow.setInputMethodMode(WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mContentViewPopupWindow.setAttachedInDecor(true);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            //点击PopupWindow最外层布局以及点击返回键PopupWindow不会消失
            mContentViewPopupWindow.setBackgroundDrawable(null);
        } else {
            mContentViewPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void addView(View child, int index) {
        mChildCount++;
        final ViewGroup view = getReactRootView();
        if (view == null) {
            if (child instanceof KeyboardCoverView) {
                mCoverView = (KeyboardCoverView) child;
            } else if (child instanceof KeyboardContentView) {
                mContentView = (KeyboardContentView) child;
            }
            initWhenAttached = true;
        } else {
            if (child instanceof KeyboardCoverView) {
                if (mCoverView != null) {
                    removeView(mCoverView);
                }
                mCoverView = (KeyboardCoverView) child;
                view.addView(mCoverView);
            } else if (child instanceof KeyboardContentView) {
                if (mContentView != null) {
                    removeView(mContentView);
                }
                mContentView = (KeyboardContentView) child;
                mContentViewPopupWindow.setContentView(mContentView);
                mContentViewPopupWindow.setWidth(AdjustResizeWithFullScreen.getUseRight());
                if (mContentVisible && mEditFocusView != null && mEditFocusView.isFocused()) {
                    if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                        KeyboardUtil.hideKeyboard(mEditFocusView);
                    }
                }
            }
        }
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "child = [" + child + "], index = [" + index + "]"
                    + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                    + ",mContentVisible=" + mContentVisible
                    + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
            );
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AdjustResizeWithFullScreen.assistRegisterActivity(mThemedContext.getCurrentActivity(), statusBarHeight, navigationBarHeight, this);
        if (initWhenAttached) {
            initWhenAttached = false;
            final ViewGroup view = getReactRootView();
            if (mCoverView != null) {
                if (mHideWhenKeyboardIsDismissed || (mContentView != null && mContentView.isShown())) {
                    mCoverView.setVisibility(GONE);
                } else {
                    keepCoverViewOnScreenFrom(AdjustResizeWithFullScreen.getUseBottom(), 0);
                    mCoverView.setVisibility(VISIBLE);
                }
                view.addView(mCoverView);
            }
            if (mContentView != null) {
                mContentViewPopupWindow.setContentView(mContentView);
                mContentViewPopupWindow.setWidth(AdjustResizeWithFullScreen.getUseRight());
                if (mContentVisible && mEditFocusView != null && mEditFocusView.isFocused()) {
                    if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                        KeyboardUtil.hideKeyboard(mEditFocusView);
                    }
                }
            }
        }
    }


    public void setHideWhenKeyboardIsDismissed(boolean hideWhenKeyboardIsDismissed) {
        mHideWhenKeyboardIsDismissed = hideWhenKeyboardIsDismissed;
    }


    public void setKeyboardPlaceholderHeight(int keyboardPlaceholderHeight) {
        if (AdjustResizeWithFullScreen.getKeyboardHeight() == 0) {
            mKeyboardPlaceholderHeight = (int) (keyboardPlaceholderHeight * mScale);
        }
        if (mContentView != null && !mContentView.isShown()) {
            if (keyboardPlaceholderHeight > 0 && !mKeyboardShown) {
                //显露面板，并发送事件
                final int height = AdjustResizeWithFullScreen.getKeyboardHeight();
                final int useBottom = mCoverView.getBottom();
                if (height != 0) {
                    keepCoverViewOnScreenFrom(useBottom - height, height);
                } else {
                    keepCoverViewOnScreenFrom(useBottom - mKeyboardPlaceholderHeight, mKeyboardPlaceholderHeight);
                }
                receiveEvent(Events.EVENT_SHOW);
            }
        } else if (mCoverView != null && !mContentVisible && !mHideWhenKeyboardIsDismissed && keyboardPlaceholderHeight == 0) {
            View viewGroup = mCoverView.getChildAt(0);
            while (!(viewGroup instanceof EditText) && ((ViewGroup) viewGroup).getChildCount() > 0) {
                viewGroup = ((ViewGroup) viewGroup).getChildAt(0);
            }
            if (viewGroup != null && viewGroup instanceof EditText) {
                //输入法弹不出来,因为ENEditText
                if (!viewGroup.isFocused()) {
                    keepCoverViewOnScreenFrom(AdjustResizeWithFullScreen.getUseBottom(), 0);
                    mCoverView.setVisibility(VISIBLE);
                }
            }
        }
    }

    public void setContentVisible(boolean contentVisible) {
        mContentVisible = contentVisible;
        if (contentVisible) {
            if (KeyboardUtil.isKeyboardActive(mEditFocusView) && mContentView != null) {
                KeyboardUtil.hideKeyboard(this);
            }
        } else {
            if (mEditFocusView != null) {
                if (mEditFocusView.isFocused()) {
                    KeyboardUtil.showKeyboard(mEditFocusView);
                } else {
                    mKeyboardShown = true;
                    onKeyboardClosed();
                }
            }
        }
    }


    @Override
    public void onKeyboardOpened() {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "onKeyboardOpened"
                    + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                    + ",mContentVisible=" + mContentVisible
                    + ",mKeyboardShown=" + mKeyboardShown
                    + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
            );
        }
        if (mKeyboardShown) return;
        mKeyboardShown = true;
        if (mOnFocusChangeListener == null) {
            mOnFocusChangeListener = new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        if (mCoverView != null) {
                            mCoverView.setVisibility(VISIBLE);
                        }
                        KeyboardUtil.showKeyboard(mEditFocusView);
                    } else {
                        if (mCoverView != null && mCoverView.isShown() && mHideWhenKeyboardIsDismissed) {
                            mCoverView.setVisibility(GONE);
                            if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                                KeyboardUtil.hideKeyboard(mEditFocusView);
                            }
                        }
                        if (mContentView != null && mContentView.isShown() && !mCoverView.isShown()) {
                            receiveEvent(Events.EVENT_HIDE);
                        }

                    }
                }
            };
            mEditFocusView = mThemedContext.getCurrentActivity().getWindow().getDecorView().findFocus();
            if (mEditFocusView != null) {
                mEditFocusView.setOnFocusChangeListener(mOnFocusChangeListener);
            }
        }
        if (mContentView != null && mContentView.isShown()) {
            receiveEvent(Events.EVENT_HIDE);
        }
        if (mCoverView != null) {
            mCoverView.setVisibility(VISIBLE);
            receiveEvent(Events.EVENT_SHOW);
        }
    }

    @Override
    public void onKeyboardClosed() {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "onKeyboardClosed"
                    + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                    + ",mContentVisible=" + mContentVisible
                    + ",mKeyboardShown=" + mKeyboardShown
                    + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
            );
        }
        if (!mKeyboardShown) return;
        mKeyboardShown = false;
        if (mContentView != null) {
            if (mContentVisible) {
            } else {
                if (mKeyboardPlaceholderHeight == 0) {
                    receiveEvent(Events.EVENT_HIDE);
                }
            }
        } else {
            receiveEvent(Events.EVENT_HIDE);
        }
        if (mCoverView != null) {
            if (mEditFocusView.isFocused()) {
                if (!mContentVisible && mHideWhenKeyboardIsDismissed && (mContentView == null || !mContentView.isShown())) {
                    mCoverView.setVisibility(GONE);
                } else {
                    if (mContentView == null) {
                        if (mHideWhenKeyboardIsDismissed) {
                            mCoverView.setVisibility(GONE);
                        } else {
                            mCoverView.setVisibility(VISIBLE);
                        }
                    } else {
                        mCoverView.setVisibility(VISIBLE);
                    }
                }
            } else {
                if (!mHideWhenKeyboardIsDismissed) {
                    mCoverView.setVisibility(VISIBLE);
                } else {
                    mCoverView.setVisibility(GONE);
                }
            }
        }
    }

    @Override
    public boolean onKeyboardResize(int heightOfLayout, int bottom) {
        if (mCoverView != null && AdjustResizeWithFullScreen.isInit()) {
            if (mCoverView.isShown()) {
                int diff = AdjustResizeWithFullScreen.getWindowBottom() - heightOfLayout;
                if (mContentVisible && diff <= navigationBarHeight + statusBarHeight) {
                    int coverViewBottom = mCoverView.getBottom();
                    if (!AdjustResizeWithFullScreen.isFullscreen() && coverViewBottom + AdjustResizeWithFullScreen.getKeyboardHeight()
                            == AdjustResizeWithFullScreen.getWindowBottom()) {
                        coverViewBottom -= diff;
                    }
                    keepCoverViewOnScreenFrom(coverViewBottom, bottom);
                    return true;
                } else {
                    keepCoverViewOnScreenFrom(heightOfLayout, bottom);
                    return true;
                }
            }
            if (mKeyboardShown) {
                keepCoverViewOnScreenFrom(heightOfLayout, bottom);
            }
        }
        return true;
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

    @Override
    public void onHostResume() {
        if (mKeyboardShownStatus) {
            mKeyboardShownStatus = false;
            if (mEditFocusView != null) {
                mEditFocusView.setFocusable(true);
                mEditFocusView.requestFocus();
            }
        } else {
            if (mCoverView == null || !mCoverView.isShown()) return;
            int diff = mUseBottom - AdjustResizeWithFullScreen.getUseBottom();
            if (diff != 0) {
                keepCoverViewOnScreenFrom(mCoverViewBottom - diff, 0);
            } else {
                keepCoverViewOnScreenFrom(mCoverViewBottom, 0);
            }
        }
    }

    @Override
    public void onHostPause() {
        if (mEditFocusView != null && (KeyboardUtil.isKeyboardActive(mEditFocusView))) {
            mKeyboardShownStatus = true;
        } else {
            if (mCoverView != null) {
                mCoverViewBottom = mCoverView.getBottom();
                mUseBottom = AdjustResizeWithFullScreen.getUseBottom();
            }
        }
    }

    @Override
    public void onHostDestroy() {
        onDropInstance();
    }


    public void onDropInstance() {
        ((ReactContext) getContext()).removeLifecycleEventListener(this);
        AdjustResizeWithFullScreen.assistUnRegister(mThemedContext.getCurrentActivity());
        detachViewFromRoot();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        receiveEvent(Events.EVENT_HIDE);
        onDropInstance();
    }

    private void detachViewFromRoot() {
        if (mEditFocusView != null) {
            mEditFocusView.setOnFocusChangeListener(null);
        }
        if (mCoverView != null) {
            removeView(mCoverView);
        }
        if (mContentView != null) {
            removeView(mContentView);
        }
        mContentView = null;
        mCoverView = null;
    }

    @Override
    public void removeView(final View child) {
        if (child == null) return;
        ViewParent viewParent = child.getParent();
        if (viewParent != null) {
            if (child.equals(mCoverView)) {
                mCoverView = null;
                ((ViewGroup) viewParent).removeView(child);
                if (!mContentVisible) {
                    receiveEvent(Events.EVENT_HIDE);
                }
            } else {
                if (mCoverView != null && mCoverView.isShown()
                        && mHideWhenKeyboardIsDismissed
                        && mEditFocusView != null && mEditFocusView.isFocused()
                        && KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                    KeyboardUtil.showKeyboard(mEditFocusView);
                }
                mContentViewPopupWindow.dismiss();
                ViewGroup parent = (ViewGroup) mContentView.getParent();
                if (parent != null) {
                    parent.removeView(mContentView);
                }
                mContentView = null;
                receiveEvent(Events.EVENT_HIDE);
            }
            child.setVisibility(GONE);
            mChildCount--;
        }
    }

    @Override
    public void removeViewAt(int index) {
        if (index == 0 && mContentView != null) {
            removeView(mContentView);
        } else {
            removeView(mCoverView);
        }
    }

    @Override
    public int getChildCount() {
        return mChildCount;
    }

    @Override
    public View getChildAt(int index) {
        if (index == 0 && mContentView != null) {
            return mContentView;
        } else {
            return mCoverView;
        }
    }

    private void receiveEvent(Events event) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("keyboardShown", mKeyboardShown);
        mEventEmitter.receiveEvent(getId(), event.toString(), map);
    }


    /**
     * 将面板固定在某高度，一般是mCoverView的bottom  或者是屏幕以外
     */
    private void keepCoverViewOnScreenFrom(final int height, final int bottom) {
        if (mCoverView != null) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mCoverView.getMeasuredHeight() == height && mCoverView.getBottom() == bottom)
                                return;
                            try {
                                ReactShadowNode coverShadowNode = mNativeModule.getUIImplementation().resolveShadowNode(mCoverView.getId());
                                if (bottom >= 0) {
                                    coverShadowNode.setPosition(YogaEdge.BOTTOM.intValue(), bottom);
                                }
                                coverShadowNode.setPosition(YogaEdge.TOP.intValue(), 0);
                                coverShadowNode.setPositionType(YogaPositionType.ABSOLUTE);
                                final int useRight = AdjustResizeWithFullScreen.getUseRight();
                                if (height > -1) {
                                    coverShadowNode.setStyleHeight(height);
                                    mNativeModule.updateNodeSize(mCoverView.getId(), useRight, height);
                                }
                                mNativeModule.getUIImplementation().dispatchViewUpdates(-1);//这句话相当于全局更新
                                mCoverView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mContentVisible) {
                                            if (height > -1) {
                                                keepContentViewOnScreenFrom(height);
                                            } else {
                                                keepContentViewOnScreenFrom(mCoverView.getBottom());
                                            }
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    /**
     * 将面板固定在某高度，一般是mCoverView的bottom  或者是屏幕以外
     *
     * @param top
     */
    private void keepContentViewOnScreenFrom(int top) {
        if (mContentView != null) {
            if (mContentViewPopupWindow.getContentView() == null) {
                mContentViewPopupWindow.setContentView(mContentView);
                mContentViewPopupWindow.setWidth(AdjustResizeWithFullScreen.getUseRight());
            }
            if (mKeyboardShown) {
                if (top != AdjustResizeWithFullScreen.getUseBottom()) {
                    top = AdjustResizeWithFullScreen.getUseBottom();
                }
            }
            final int tempHeight = getContentViewHeight(top);
            final int useRight = AdjustResizeWithFullScreen.getUseRight();
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mNativeModule.updateNodeSize(mContentView.getId(), useRight, tempHeight);
                        }
                    });
            if (mContentViewPopupWindow.isShowing()) {
                mContentViewPopupWindow.update(0, top, useRight, tempHeight);
            } else {
                if (mContentViewPopupWindow.getHeight() != tempHeight) {
                    mContentViewPopupWindow.setHeight(tempHeight);
                }
                if (mContentViewPopupWindow.getWidth() != useRight) {
                    mContentViewPopupWindow.setWidth(useRight);
                }
                mContentViewPopupWindow.showAtLocation(AdjustResizeWithFullScreen.getDecorView(), Gravity.NO_GRAVITY, 0, top);
            }
        }
    }

    private int getContentViewHeight(int top) {
        int realKeyboardHeight = AdjustResizeWithFullScreen.getRemainingHeight(top);
        int keyboardHeight = AdjustResizeWithFullScreen.getKeyboardHeight();
        if (realKeyboardHeight == 0 || realKeyboardHeight < keyboardHeight) {
            realKeyboardHeight = keyboardHeight;
            if (realKeyboardHeight == 0) {
                if (mKeyboardPlaceholderHeight != 0) {
                    realKeyboardHeight = mKeyboardPlaceholderHeight;
                } else {
                    realKeyboardHeight = mMinContentViewHeight;
                }
            }
        }
        return realKeyboardHeight;
    }

}

