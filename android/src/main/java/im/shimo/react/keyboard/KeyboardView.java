package im.shimo.react.keyboard;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.PopupWindow;

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
    private volatile int mChildCount;
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
    private int mUseBottom;
    private int mUseRight;
    private ValueAnimator translationSlide;
    // whether keyboard is shown
    private boolean mKeyboardShown = false;
    private volatile int mVisibility = -1;
    private int mOrientation = -1;
    private boolean isOrientationChange;

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
        mContentViewPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

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
                mChildCount++;
            } else if (child instanceof KeyboardContentView) {
                if (mContentView != null) {
                    removeView(mContentView);
                }
                mContentView = (KeyboardContentView) child;
                mContentViewPopupWindow.setContentView(mContentView);
                mContentViewPopupWindow.setWidth(AdjustResizeWithFullScreen.getUseRight());
            }
        }
        if (KeyboardViewManager.DEBUG) {
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
        if (KeyboardViewManager.DEBUG) {
            Log.e(TAG, "onAttachedToWindow,mOrientation=" + mOrientation);
        }
        if (mOrientation == -1) {
            mOrientation = getResources().getConfiguration().orientation;
        }
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
                mChildCount++;
            }
            if (mContentView != null) {
                mContentViewPopupWindow.setContentView(mContentView);
                mContentViewPopupWindow.setWidth(AdjustResizeWithFullScreen.getUseRight());
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
        if (mContentView != null && mCoverView != null) {
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
            keepCoverViewOnScreenFrom(AdjustResizeWithFullScreen.getUseBottom(), 0);
            mCoverView.setVisibility(VISIBLE);
        }
    }

    public void setContentVisible(boolean contentVisible) {
        mContentVisible = contentVisible;
        if (contentVisible) {
            if (mCoverView == null) return;
            mCoverView.setVisibility(VISIBLE);
            keepCoverViewOnScreenFrom(mPreCoverHeight, mPreCoverBottom);
        } else {
            if (mEditFocusView != null) {
                if (mEditFocusView.isFocused()) {
                    if (!mKeyboardShown) {
                        if (mCoverView != null) {
                            mCoverView.setVisibility(GONE);
                            //设置到屏幕外
                            keepCoverViewOnScreenFrom(mPreCoverHeight, AdjustResizeWithFullScreen.getUseBottom());
                            if (mContentView != null) {
                                //删除
                                removeContentView();
                            }
                        }
                    }
                } else {
                    mKeyboardShown = true;
                    onKeyboardClosed();
                }
            } else {
                if (!mKeyboardShown) {
                        if (mCoverView != null) {
                            mCoverView.setVisibility(GONE);
                            //设置到屏幕外
                            keepCoverViewOnScreenFrom(mPreCoverHeight, AdjustResizeWithFullScreen.getUseBottom());
                            if (mContentView != null) {
                                //删除
                                removeContentView();
                            }
                        }
                    }
            }
        }
    }


    @Override
    public void onKeyboardOpened() {
        if (KeyboardViewManager.DEBUG) {
            Log.e(TAG, "onKeyboardOpened"
                    + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                    + ",mContentVisible=" + mContentVisible
                    + ",mKeyboardShown=" + mKeyboardShown
                    + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
            );
        }
        if (mKeyboardShown) return;
        mKeyboardShown = true;
        if (mEditFocusView == null) {
            View view = mThemedContext.getCurrentActivity().getWindow().getDecorView().findFocus();
            if (view instanceof EditText || view instanceof WebView) {
                mEditFocusView = view;
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
        if (KeyboardViewManager.DEBUG) {
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
            if (mEditFocusView != null && mEditFocusView.isFocused()) {
                if (mHideWhenKeyboardIsDismissed) {
                    mCoverView.setVisibility(GONE);
                    mContentViewPopupWindow.dismiss();
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
        if (KeyboardViewManager.DEBUG) {
            Log.e(TAG, "onKeyboardResize,heightOfLayout=" + heightOfLayout);
            Log.e(TAG, "onKeyboardResize,mCoverView.isShown()=" + mCoverView.isShown());
        }
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

    }

    @Override
    public void onHostPause() {

    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (mVisibility != visibility) {
            if (KeyboardViewManager.DEBUG) {
                Log.e(TAG, "onWindowVisibilityChanged,mVisibility=" + mVisibility + ",visibility=" + visibility
                        + ",mUseBottom=" + mUseBottom + ",mKeyboardShownStatus=" + mKeyboardShownStatus);
            }
            if (visibility == VISIBLE) {
                if (mUseBottom == 0 && !mKeyboardShownStatus) {
                    //没有变化，无需进入逻辑
                    return;
                }
                int orientation = getResources().getConfiguration().orientation;
                final boolean isOchanged = isOrientationChange = mOrientation != orientation;
                if (isOchanged) {
                    mOrientation = orientation;
                    mKeyboardShownStatus = false;
                    mVisibility = visibility;
                    return;
                }
                if (mKeyboardShownStatus) {
                    mKeyboardShownStatus = false;
                    if (mEditFocusView != null) {
                        mEditFocusView.setFocusable(true);
                        mEditFocusView.requestFocus();
                    }
                } else {
                    if (mCoverView == null || !mCoverView.isShown() || mPreCoverHeight == 0) {
                        mVisibility = visibility;
                        return;
                    }
                    int diff = mUseBottom - AdjustResizeWithFullScreen.getUseBottom();
                    int diffR = mUseRight - getRootView().getWidth();//AdjustResizeWithFullScreen.getUseRight();
                    boolean isChanged = diff != 0 || diffR != 0 || isOchanged;
                    if (isChanged) {
                        keepCoverViewOnScreenFrom(mPreCoverHeight - diff, 0);
                    } else {
                        keepCoverViewOnScreenFrom(mPreCoverHeight, 0);
                    }
                }
                mVisibility = visibility;
            } else if (visibility == GONE) {
                if (mEditFocusView != null && (KeyboardUtil.isKeyboardActive(mEditFocusView)) || mKeyboardShown) {
                    mKeyboardShownStatus = true;
                } else {
                    if (mCoverView != null) {
                        mUseBottom = AdjustResizeWithFullScreen.getUseBottom();
                        mUseRight = getRootView().getWidth();//AdjustResizeWithFullScreen.getUseRight();
                    }
                }
                mVisibility = visibility;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        receiveEvent(Events.EVENT_HIDE);
        onDropInstance();
    }

    @Override
    public void onHostDestroy() {
        ((ReactContext) getContext()).removeLifecycleEventListener(this);
        onDropInstance();
    }


    public void onDropInstance() {
        if (mCoverView != null) {
            removeView(mCoverView);
        }
        if (mContentView != null) {
            removeView(mContentView);
        }
        AdjustResizeWithFullScreen.assistUnRegister();
//        mContentView = null;
//        mCoverView = null;
        mEditFocusView = null;
//        mContentViewPopupWindow.dismiss();
        mContentViewPopupWindow.setContentView(null);
        mVisibility = -1;
        mKeyboardShown = mKeyboardShownStatus = false;
        mOrientation = -1;
        mContentVisible = false;
        mKeyboardPlaceholderHeight = 0;
        if (translationSlide != null) {
            translationSlide = null;
        }
    }

    @Override
    public void removeView(final View child) {
        if (child == null) return;
        ViewParent viewParent = child.getParent();
        if (viewParent != null) {
            if (KeyboardViewManager.DEBUG) {
                Log.e(TAG, "removeView,child=" + child);
            }
            if (child.equals(mCoverView)) {
                removeCoverView(child, (ViewGroup) viewParent);
            } else {
                removeContentView();
            }
            child.setVisibility(GONE);
        }
    }

    private void removeCoverView(View child, ViewGroup viewParent) {
        mCoverView = null;
        viewParent.removeView(child);
        mChildCount--;
        if (!mContentVisible) {
            receiveEvent(Events.EVENT_HIDE);
        }
        mPreCoverBottom = mPreCoverHeight = mPreCoverWidth = 0;
    }

    private void removeContentView() {
        mContentViewPopupWindow.dismiss();
        ViewGroup parent = (ViewGroup) mContentView.getParent();
        if (parent != null) {
            parent.removeView(mContentView);
        }
        mContentView = null;
        receiveEvent(Events.EVENT_HIDE);
        mPreContentWidth = mPreContentHeight = mPreContentTop = 0;
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

    //防止多次重绘界面
    private int mPreCoverHeight = 0;
    private int mPreCoverBottom = 0;
    private int mPreCoverWidth = 0;

    /**
     * 确定CoverView的位置，以及随着coverView变化而变化的contentView的位置
     */
    private void keepCoverViewOnScreenFrom(final int height, final int bottom) {
        if (mCoverView != null) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            final int useRight = getReactRootView().getWidth();//AdjustResizeWithFullScreen.getUseRight();
                            //maybe its null in this thread
                            if (!isOrientationChange && mPreCoverBottom == bottom && mPreCoverHeight == height && mPreCoverWidth == useRight || mCoverView == null) {
                                postContentView();
                                return;
                            }
                            if (KeyboardViewManager.DEBUG) {
                                Log.e(TAG, "keepCoverViewOnScreenFrom,height" + height + ",bottom=" + bottom + ",useRight=" + useRight);
                            }
                            mPreCoverBottom = bottom;
                            mPreCoverHeight = height;
                            mPreCoverWidth = useRight;
                            try {
                                ReactShadowNode coverShadowNode = mNativeModule.getUIImplementation().resolveShadowNode(mCoverView.getId());
                                if (bottom >= 0) {
                                    coverShadowNode.setPosition(YogaEdge.BOTTOM.intValue(), bottom);
                                }
                                coverShadowNode.setPosition(YogaEdge.TOP.intValue(), 0);
                                coverShadowNode.setPositionType(YogaPositionType.ABSOLUTE);
                                if (height > -1) {
                                    coverShadowNode.setStyleHeight(height);
                                    mNativeModule.updateNodeSize(mCoverView.getId(), useRight, height);
                                }
                                mNativeModule.getUIImplementation().dispatchViewUpdates(-1);//这句话相当于全局更新
                                postContentView();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        private void postContentView() {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mContentVisible) {
                                        if (height > -1) {
                                            keepContentViewOnScreenFrom(height);
                                        } else {
                                            try {
                                                final int coverBottom = mCoverView == null ? -99 : mCoverView.getBottom();
                                                if (coverBottom == -99) return;
                                                keepContentViewOnScreenFrom(coverBottom);
                                            } catch (Exception e) {
                                                //maybe its null in this thread
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    });
            if (mPreCoverBottom == bottom && mPreCoverHeight == height) {
                return;
            }
            if (translationSlide != null) {
                if (translationSlide.isRunning() || translationSlide.isStarted()) {
                    translationSlide.cancel();
                }
            }
            translationSlide = ObjectAnimator.ofFloat(mCoverView, "alpha", 0, 1);
            translationSlide.start();
        }
    }

    //防止多次重绘界面
    private int mPreContentHeight = 0;
    private int mPreContentTop = 0;
    private int mPreContentWidth = 0;

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
            final int useRight = getReactRootView().getWidth();//AdjustResizeWithFullScreen.getUseRight();
            if (KeyboardViewManager.DEBUG) {
                Log.e(TAG, "keepContentViewOnScreenFrom,height" + tempHeight + ",top=" + top + ",useRight=" + useRight);
            }
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mContentView != null) {
                                //maybe its null in this thread
                                mNativeModule.updateNodeSize(mContentView.getId(), useRight, tempHeight);
                            }
                        }
                    });
            if (mContentViewPopupWindow.isShowing()) {
                boolean isOrientChanged = isOrientationChange;
                if (!isOrientChanged) {
                    isOrientChanged = mOrientation == getResources().getConfiguration().orientation;
                }

                if (!isOrientChanged && mPreContentHeight == tempHeight && mPreContentTop == top && mPreContentWidth == useRight) {
                    return;
                }
                if (isOrientChanged) {
                    isOrientationChange = false;
                    mOrientation = getResources().getConfiguration().orientation;
                }
                mContentViewPopupWindow.update(AdjustResizeWithFullScreen.getUseLeft(), top, useRight, tempHeight);
            } else {
                if (mContentViewPopupWindow.getHeight() != tempHeight) {
                    mContentViewPopupWindow.setHeight(tempHeight);
                }
                if (mContentViewPopupWindow.getWidth() != useRight) {
                    mContentViewPopupWindow.setWidth(useRight);
                }
                try {
                    final View decorView = AdjustResizeWithFullScreen.getDecorView();
                    if(decorView!=null) {
                        mContentViewPopupWindow.showAtLocation(decorView, Gravity.NO_GRAVITY, AdjustResizeWithFullScreen.getUseLeft(), top);
                    }
                } catch (Exception e) {
                    //mybe its non in asynchronization
                    e.printStackTrace();
                }
            }
            mPreContentHeight = tempHeight;
            mPreContentTop = top;
            mPreContentWidth = useRight;
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


