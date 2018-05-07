package im.shimo.react.keyboard;

import android.content.res.Configuration;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

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
    private int mCoverViewHeight;
    private volatile int mChildCount;
    private View.OnFocusChangeListener mOnFocusChangeListener;
    private View mEditFocusView;
    private int mCoverViewBottom;
    private volatile boolean initWhenAttached;

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

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        AdjustResizeWithFullScreen.onPauseResize();
        super.onConfigurationChanged(newConfig);
    }

    public KeyboardView(final ThemedReactContext context, int navigationBarHeight, int statusBarHeight) {
        super(context);
        this.mThemedContext = context;
        this.mNativeModule = mThemedContext.getNativeModule(UIManagerModule.class);
        this.navigationBarHeight = navigationBarHeight;
        this.statusBarHeight = statusBarHeight;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        context.addLifecycleEventListener(this);
        System.out.println("KeyboardView.KeyboardView");
    }

    @Override
    public void addView(View child, int index) {
        mChildCount++;
        final ViewGroup view = getReactRootView();
        if (view == null) {
            if (child instanceof KeyboardCoverView) {
                mCoverView = (KeyboardCoverView) child;
                mCoverView.setVisibility(GONE);
            } else if (child instanceof KeyboardContentView) {
                mContentView = (KeyboardContentView) child;
                mContentView.setVisibility(GONE);
            }
            initWhenAttached = true;
        } else {
            if (child instanceof KeyboardCoverView) {
                if (mCoverView != null) {
                    removeView(mCoverView);
                }
                mCoverView = (KeyboardCoverView) child;
                mCoverView.setVisibility(GONE);
//                keepCoverViewFloatOnKeyboard();
                view.addView(mCoverView);
            } else if (child instanceof KeyboardContentView) {
                if (mContentView != null) {
                    removeView(mContentView);
                }
                mContentView = (KeyboardContentView) child;
                view.addView(mContentView);
                if (mContentVisible && mEditFocusView != null && mEditFocusView.isFocused()) {
//                    keepCoverViewOnScreenFrom(-1, mCoverView.getBottom());
                    mContentView.setVisibility(VISIBLE);
                    if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                        KeyboardUtil.hideKeyboard(mEditFocusView);
                    }
                } else {
                    mContentView.setVisibility(GONE);
                }
            }
        }
        System.out.println("child = [" + child + "], index = [" + index + "]"
                + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                + ",mContentVisible=" + mContentVisible
                + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
        );
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
                view.addView(mContentView);
                if (mContentVisible && mEditFocusView != null && mEditFocusView.isFocused()) {
//                    keepCoverViewOnScreenFrom(-1, mCoverView.getBottom());
                    mContentView.setVisibility(VISIBLE);
                    if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                        KeyboardUtil.hideKeyboard(mEditFocusView);
                    }
                } else {
                    mContentView.setVisibility(GONE);
                }
            }
        }
    }


    public void setHideWhenKeyboardIsDismissed(boolean hideWhenKeyboardIsDismissed) {
        mHideWhenKeyboardIsDismissed = hideWhenKeyboardIsDismissed;
    }


    public void setKeyboardPlaceholderHeight(int keyboardPlaceholderHeight) {
        //todo ,表格目前没有调试
        if (AdjustResizeWithFullScreen.getKeyboardHeight() == 0) {
            mKeyboardPlaceholderHeight = (int) (keyboardPlaceholderHeight * mScale);
        }
        if (mContentView != null && !mContentView.isShown()) {
            if (keyboardPlaceholderHeight > 0 && !mKeyboardShown) {
                //显露面板，并发送事件
                int height = AdjustResizeWithFullScreen.getKeyboardHeight();
                int useBottom = mCoverView.getBottom();
                if (height != 0) {
                    keepCoverViewOnScreenFrom(useBottom - height, height);
                    keepContentViewOnScreenFrom(useBottom - height);
                } else {
                    keepCoverViewOnScreenFrom(useBottom - mKeyboardPlaceholderHeight, mKeyboardPlaceholderHeight);
                    keepContentViewOnScreenFrom(useBottom - mKeyboardPlaceholderHeight);
                }
                mContentView.setVisibility(VISIBLE);
                receiveEvent(Events.EVENT_SHOW);
            }
        }
    }

    public void setContentVisible(boolean contentVisible) {
        mContentVisible = contentVisible;
        if (contentVisible) {
            if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
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
        System.out.println("onKeyboardOpened"
                + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                + ",mContentVisible=" + mContentVisible
                + ",mKeyboardShown=" + mKeyboardShown
                + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
        );
        if (mKeyboardShown) return;
        mKeyboardShown = true;
        if (mOnFocusChangeListener == null) {
            mOnFocusChangeListener = new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    System.out.println("v = [" + v + "], hasFocus = [" + hasFocus + "]");
                    if (hasFocus) {
                        if (mCoverView != null) {
                            mCoverView.setVisibility(VISIBLE);
                        }
                        KeyboardUtil.showKeyboard(mEditFocusView);
                    } else {
                        if (mCoverView != null && mHideWhenKeyboardIsDismissed) {
                            mCoverView.setVisibility(GONE);
                            resetCoverView();
                        }
                        if (mContentView != null) {
                            mContentView.setVisibility(GONE);
                            resetContentView();
                        }
                        if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
                            KeyboardUtil.hideKeyboard(mEditFocusView);
                        }
                    }
                }
            };
            mEditFocusView = mThemedContext.getCurrentActivity().getWindow().getDecorView().findFocus();
            if (mEditFocusView != null) {
                mEditFocusView.setOnFocusChangeListener(mOnFocusChangeListener);
            }
        }
        if (mCoverView != null) {
            mCoverView.setVisibility(VISIBLE);
            receiveEvent(Events.EVENT_SHOW);
        }
    }

    @Override
    public void onKeyboardClosed() {
        System.out.println("onKeyboardClosed"
                + ",mHideWhenKeyboardIsDismissed=" + mHideWhenKeyboardIsDismissed
                + ",mContentVisible=" + mContentVisible
                + ",mKeyboardShown=" + mKeyboardShown
                + ",mKeyboardPlaceholderHeight=" + mKeyboardPlaceholderHeight
        );
        if (!mKeyboardShown) return;
        mKeyboardShown = false;
        if (mContentView != null) {
            if (mContentVisible) {
                mContentView.setVisibility(VISIBLE);
            } else {
                if (mKeyboardPlaceholderHeight == 0) {
                    mContentView.setVisibility(GONE);
                    resetContentView();
                    receiveEvent(Events.EVENT_HIDE);
                }
            }
        }
        if (mCoverView != null) {
            if (mEditFocusView.isFocused()) {
                if (mHideWhenKeyboardIsDismissed && (mContentView == null || !mContentView.isShown())) {
                    mCoverView.setVisibility(GONE);
                    resetCoverView();
                } else {
                    mCoverView.setVisibility(VISIBLE);
                }
            } else {
                mCoverView.setVisibility(GONE);
                resetCoverView();
            }
        }
    }

    @Override
    public boolean onKeyboardResize(int heightOfLayout, int bottom) {
        if (mCoverView != null && mCoverView.isShown()) {
            keepCoverViewOnScreenFrom(heightOfLayout, bottom);
        }
        if (mContentView != null && mContentView.isShown()) {
            keepContentViewOnScreenFrom(mCoverView.getBottom());
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
        AdjustResizeWithFullScreen.onResumeResize();
    }

    @Override
    public void onHostPause() {
        AdjustResizeWithFullScreen.onPauseResize();
        if (mCoverView != null) {
            mCoverView.setVisibility(GONE);
            resetCoverView();
        }
        if (mContentView != null) {
            mContentView.setVisibility(GONE);
            resetContentView();
        }
        if (KeyboardUtil.isKeyboardActive(mEditFocusView)) {
            KeyboardUtil.hideKeyboard(mEditFocusView);
        }
    }

    @Override
    public void onHostDestroy() {
        System.out.println("KeyboardView.onHostDestroy");
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
        onDropInstance();
        System.out.println("KeyboardView.onDetachedFromWindow");
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
        System.out.println("removeView,child = [" + child + "]");
        final ViewGroup view = (ViewGroup) child.getParent();
        if (view != null) {
            child.setVisibility(GONE);
            view.removeView(child);
            if (child.equals(mCoverView)) {
                mCoverView = null;
            } else {
                mContentView = null;
            }
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
     *
     * @param top
     */
    private void keepContentViewOnScreenFrom(final int top) {
        if (mContentView != null) {
            if (mContentView.getTop() != top || mContentView.isShown()) {
                ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    UIManagerModule nativeModule = ((ReactContext) getContext()).getNativeModule(UIManagerModule.class);
                                    ReactShadowNode contentShadowNode = nativeModule.getUIImplementation().resolveShadowNode(mContentView.getId());
                                    contentShadowNode.setPosition(YogaEdge.TOP.intValue(), top);
                                    contentShadowNode.setPosition(YogaEdge.BOTTOM.intValue(), 0);
                                    contentShadowNode.setPositionType(YogaPositionType.ABSOLUTE);
                                    int realKeyboardHeight = AdjustResizeWithFullScreen.getKeyboardHeight();
                                    if (realKeyboardHeight != 0) {
                                        nativeModule.updateNodeSize(mContentView.getId(), mContentView.getMeasuredWidth(), realKeyboardHeight);
                                        contentShadowNode.setStyleHeight(realKeyboardHeight);
                                    } else {
                                        nativeModule.updateNodeSize(mContentView.getId(), mContentView.getMeasuredWidth(), mKeyboardPlaceholderHeight);
                                        contentShadowNode.setStyleHeight(mKeyboardPlaceholderHeight);
                                    }
                                    ((ReactContext) getContext()).getNativeModule(UIManagerModule.class).getUIImplementation().dispatchViewUpdates(-1);//这句话相当于全局更新
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        }
    }

    /**
     * 将面板固定在某高度，一般是mCoverView的bottom  或者是屏幕以外
     */
    private void keepCoverViewOnScreenFrom(final int height, final int bottom) {
        if (mCoverView != null) {
            if (mCoverView.getMeasuredHeight() == height && mCoverView.getBottom() == bottom)
                return;
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {

                            try {
                                UIManagerModule nativeModule = ((ReactContext) getContext()).getNativeModule(UIManagerModule.class);
                                ReactShadowNode contentShadowNode = nativeModule.getUIImplementation().resolveShadowNode(mCoverView.getId());
                                if (bottom >= 0) {
                                    contentShadowNode.setPosition(YogaEdge.BOTTOM.intValue(), bottom);
                                }
                                contentShadowNode.setPosition(YogaEdge.TOP.intValue(), 0);
                                contentShadowNode.setPositionType(YogaPositionType.ABSOLUTE);
                                if (height > -1) {
                                    contentShadowNode.setStyleHeight(height);
                                }
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class).getUIImplementation().dispatchViewUpdates(-1);//这句话相当于全局更新
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    public void resetCoverView() {
        if (mCoverView != null) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                UIManagerModule nativeModule = ((ReactContext) getContext()).getNativeModule(UIManagerModule.class);
                                ReactShadowNode contentShadowNode = nativeModule.getUIImplementation().resolveShadowNode(mCoverView.getId());
                                contentShadowNode.setPosition(YogaEdge.BOTTOM.intValue(), 0);
                                contentShadowNode.setPosition(YogaEdge.TOP.intValue(), AdjustResizeWithFullScreen.getWindowBottom());
                                contentShadowNode.setPositionType(YogaPositionType.ABSOLUTE);
                                contentShadowNode.setStyleHeight(0);
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class).getUIImplementation().dispatchViewUpdates(-1);//这句话相当于全局更新
                            } catch (Exception e) {

                            }
                        }
                    });
        }
    }

    public void resetContentView() {
        if (mContentView != null) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                UIManagerModule nativeModule = ((ReactContext) getContext()).getNativeModule(UIManagerModule.class);
                                ReactShadowNode contentShadowNode = nativeModule.getUIImplementation().resolveShadowNode(mContentView.getId());
                                contentShadowNode.setPosition(YogaEdge.TOP.intValue(), AdjustResizeWithFullScreen.getWindowBottom());
                                contentShadowNode.setPositionType(YogaPositionType.ABSOLUTE);
                                nativeModule.updateNodeSize(mContentView.getId(), mContentView.getMeasuredWidth(), 0);
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class).getUIImplementation().dispatchViewUpdates(-1);//这句话相当于全局更新
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }
}

