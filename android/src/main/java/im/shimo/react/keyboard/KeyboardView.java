package im.shimo.react.keyboard;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.PopupWindow;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.RCTEventEmitter;

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


public class KeyboardView extends ReactRootAwareViewGroup implements LifecycleEventListener {
    private @Nullable
    PopupWindow mPopupWindow;
    private @Nullable
    AbstractKeyboardState mKeyboardState;
    private @Nullable
    View mContentView;
    private @Nullable
    View mCoverView;
    private int navigationBarHeight;
    private int statusBarHeight;
    private int mChildCount = 0;
    private AbstractKeyboardState.OnKeyboardChangeListener mOnKeyboardChangeListener;
    private OnAttachStateChangeListener mOnAttachStateChangeListener;
    private ActivityEventListener mActivityEventListener;
    private boolean mHideWhenKeyboardIsDismissed = true;
    private RCTEventEmitter mEventEmitter;
    private int mKeyboardPlaceholderHeight;
    private @Nullable
    Rect mKeyboardPlaceholderFrame;
    private float mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
    private boolean mContentVisible = true;
    private ObjectAnimator translationSlide;

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

    public KeyboardView(ThemedReactContext context, int navigationBarHeight, int statusBarHeight) {
        super(context);
        this.navigationBarHeight = navigationBarHeight;
        this.statusBarHeight = statusBarHeight;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        context.addLifecycleEventListener(this);
        mOnAttachStateChangeListener = new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                resizeCover();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                onDropInstance();
            }
        };
        addOnAttachStateChangeListener(mOnAttachStateChangeListener);
        bindKeyboardState();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing as we are laid out by UIManager
    }


    @Override
    public void addView(View child, int index) {
        if (child instanceof KeyboardContentView) {
            if (mContentView != null) {
                removeView(mContentView);
            }

            mContentView = child;
            mChildCount++;

            if (mKeyboardPlaceholderHeight > 0) {
                showKeyboardPlaceHolder(mKeyboardPlaceholderHeight);
            } else {
                showOrUpdatePopupWindow();
            }
        } else if (child instanceof KeyboardCoverView) {
            if (mCoverView != null) {
                removeView(mCoverView);
            }

            mCoverView = child;
            mChildCount++;

            if (mKeyboardPlaceholderHeight > 0) {
                showKeyboardPlaceHolder(mKeyboardPlaceholderHeight);
            } else {
                resizeCover();
            }
        }


        checkKeyboardState();
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

    @Override
    public void removeView(View child) {
        if (child instanceof KeyboardContentView) {
            dismissPopupWindow();
            mContentView = null;
            mChildCount--;

            if (mKeyboardPlaceholderFrame != null) {
                mKeyboardPlaceholderFrame = null;
                resizeCover();
            }

        } else if (child instanceof KeyboardCoverView) {
            removeCoverFromSuper();
            mCoverView = null;
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

    public void onDropInstance() {
        if (mKeyboardState != null) {
            ((ReactContext) getContext()).removeLifecycleEventListener(this);
            dismissPopupWindow();
            removeCoverFromSuper();
            unbindKeyboardState();
            removeOnAttachStateChangeListener(mOnAttachStateChangeListener);
            mOnAttachStateChangeListener = null;
        }
    }

    @Override
    public void onHostResume() {
        if (mKeyboardState != null) {
            showOrUpdatePopupWindow();
            resizeCover();
        }
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        onDropInstance();
    }

    private void bindKeyboardState() {
        if (mKeyboardState != null) {
            return;
        }
        final ReactContext context = (ReactContext) getContext();
        Activity activity = ((ReactContext) getContext()).getCurrentActivity();

        if (activity != null) {
            mOnKeyboardChangeListener = new AbstractKeyboardState.OnKeyboardChangeListener() {
                @Override
                public void onKeyboardShown(Rect keyboardFrame) {
                    showOrUpdatePopupWindow(keyboardFrame);
                    resizeCover();

                    if (mKeyboardPlaceholderHeight == 0) {
                        receiveEvent(Events.EVENT_SHOW);
                    }
                }

                @Override
                public void onKeyboardClosed() {
                    if (mKeyboardPlaceholderHeight == 0) {
                        hidePopupWindow();
                        receiveEvent(Events.EVENT_HIDE);
                    } else {
                        showKeyboardPlaceHolder(mKeyboardPlaceholderHeight);
                    }

                    resizeCover();
                }
            };
            mKeyboardState = KeyboardStateFactory.create(activity.findViewById(android.R.id.content), navigationBarHeight, statusBarHeight);
            mKeyboardState.addOnKeyboardChangeListener(mOnKeyboardChangeListener);
            checkKeyboardState();
        } else if (mActivityEventListener == null) {
            mActivityEventListener = new ActivityEventListener() {
                @Override
                public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

                }

                @Override
                public void onNewIntent(Intent intent) {
                    if (context.getCurrentActivity() != null) {
                        unbindKeyboardState();
                        bindKeyboardState();
                    }
                }
            };
            context.addActivityEventListener(mActivityEventListener);
        }
    }

    private void unbindKeyboardState() {
        if (mKeyboardState != null) {
            mKeyboardState.removeOnKeyboardChangeListener(mOnKeyboardChangeListener);
            mKeyboardState = null;
        } else if (mActivityEventListener != null) {
            ((ReactContext) getContext()).removeActivityEventListener(mActivityEventListener);
            mActivityEventListener = null;
        }
    }

    private void checkKeyboardState() {
        if (mKeyboardState != null && mKeyboardState.isKeyboardShowing()) {
            mOnKeyboardChangeListener.onKeyboardShown(mKeyboardState.getKeyboardFrame());
        }
    }

    private void showOrUpdatePopupWindow() {
        if (mKeyboardState != null) {
            if (mKeyboardPlaceholderHeight > 0 && !mKeyboardState.isKeyboardShowing() && mKeyboardPlaceholderFrame != null) {
                showOrUpdatePopupWindow(mKeyboardPlaceholderFrame);
            } else {
                showOrUpdatePopupWindow(mKeyboardState.getKeyboardFrame());
            }
        }
    }

    private void showOrUpdatePopupWindow(final Rect keyboardFrame) {
        if (!mContentVisible) {
            hidePopupWindow();
        } else if (mContentView != null) {
            int extraHeight = dealWithPopwindow(keyboardFrame);
            updateNodeSizeOnQueueThread(keyboardFrame, extraHeight);
        }
    }

    private int dealWithPopwindow(Rect keyboardFrame) {
        final int extraHeight = checkExtraHeight();
        if (mPopupWindow == null) {
            mPopupWindow = new PopupWindow(mContentView, keyboardFrame.width(), keyboardFrame.height() + extraHeight);
            mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPopupWindow.setAnimationStyle(R.style.DialogAnimationSlide);
            mPopupWindow.setClippingEnabled(false);
            showAtLocationPopwindow(keyboardFrame, extraHeight);
        } else {
            updatePopwindow(keyboardFrame, extraHeight);
            if (translationSlide != null) {
                if (translationSlide.isRunning() || translationSlide.isStarted()) {
                    translationSlide.cancel();
                }
            }
            translationSlide = ObjectAnimator.ofFloat(mCoverView, "alpha", 0, 1);
            translationSlide.start();
        }
        return extraHeight;
    }

    private void updateNodeSizeOnQueueThread(final Rect keyboardFrame, final int extraHeight) {
        ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mContentView != null) {
                            if (mKeyboardState != null && mKeyboardState.isKeyboardShowing()) {
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                        .updateNodeSize(mContentView.getId(), keyboardFrame.width(), keyboardFrame.height());
                            } else {
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                        .updateNodeSize(mContentView.getId(), keyboardFrame.width(), keyboardFrame.height() + extraHeight);
                            }
                        }
                    }
                });
    }

    private void showAtLocationPopwindow(Rect keyboardFrame, int extraHeight) {
        dealWithShowAtLoacationWhenShowNavigationbar(keyboardFrame, extraHeight);
    }

    private void dealWithShowAtLoacationWhenShowNavigationbar(Rect keyboardFrame, int extraHeight) {
        mPopupWindow.showAtLocation(getRootView(), Gravity.NO_GRAVITY, 0, keyboardFrame.top - extraHeight);
    }

    private void updatePopwindow(Rect keyboardFrame, int extraHeight) {
        mPopupWindow.update(0, keyboardFrame.top - extraHeight, keyboardFrame.width(), keyboardFrame.height() + extraHeight);
    }

    private int checkExtraHeight() {
        if (mKeyboardState != null) {
            return mKeyboardState.checkExtraHeight(navigationBarHeight);
        }
        return 0;
    }

    /**
     * 预留方法，以后使用
     *
     * @param keyboardFrame
     */
    private void dealWithShowAtLoacationWhenHideNavigationbar(Rect keyboardFrame) {
        mPopupWindow.showAtLocation(getRootView(), Gravity.NO_GRAVITY, 0, keyboardFrame.top);
    }

    /**
     * 预留方法，以后使用
     *
     * @param keyboardFrame
     */
    private void dealWithUpdateWhenHideNavigationbar(Rect keyboardFrame) {
        mPopupWindow.update(0, keyboardFrame.top, keyboardFrame.width(), keyboardFrame.height());
    }

    private void resizeCover() {
        if (mKeyboardState != null && mCoverView != null) {
            if (mHideWhenKeyboardIsDismissed && mKeyboardPlaceholderFrame == null && !mKeyboardState.isKeyboardShowing()) {
                mCoverView.setVisibility(GONE);
                return;
            } else if (mCoverView.getVisibility() != VISIBLE) {
                mCoverView.setVisibility(VISIBLE);
            }
            if (mKeyboardPlaceholderFrame != null && !mKeyboardState.isKeyboardShowing()) {
                resizeCover(mKeyboardPlaceholderFrame);
            } else {
                resizeCover(mKeyboardState.getKeyboardFrame());
            }
        }
    }

    private void resizeCover(Rect keyboardFrame) {
        ReactRootView reactRootView = getReactRootView();

        if (mKeyboardState == null || mCoverView == null || reactRootView == null) {
            return;
        }

        if (mCoverView.getParent() == null) {
            reactRootView.addView(mCoverView);
        }

        int rootHeight = reactRootView.getHeight();
        if (!mKeyboardState.isKeyboardShowing() && mKeyboardPlaceholderHeight > 0) {
            //说明键盘没有弹起，但是popwindow是显示状态
            final int extraHeight = checkExtraHeight();
            rootHeight -= extraHeight;
        }

        final ReactContext context = (ReactContext) getContext();
        final int coverViewWidth = keyboardFrame.width();
        int coverViewHeightTemp = rootHeight - keyboardFrame.height();
        if (!mKeyboardState.isKeyboardShowing() && mKeyboardPlaceholderHeight == 0) {
            if (mKeyboardState.isRealNavigationBarShow()) {
                //说明键盘没有弹起，popwindow也没有显示，并且第三方rom全屏,所以要挨到最底边
                final int extraHeight = checkExtraHeight();
                if (mKeyboardState.isRomNavigationBarShow()) {
                    coverViewHeightTemp += extraHeight;
                    if (coverViewHeightTemp > keyboardFrame.top) {
                        coverViewHeightTemp -= extraHeight;
                    }
                }
            }
        }
        final int coverViewHeight = coverViewHeightTemp;
        context.runOnNativeModulesQueueThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mCoverView != null) {
                            context.getNativeModule(UIManagerModule.class)
                                    .updateNodeSize(mCoverView.getId(), coverViewWidth, coverViewHeight);
                        }
                    }
                }
        );
    }

    private void dismissPopupWindow() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
    }

    private void hidePopupWindow() {
        if (mPopupWindow != null && mKeyboardState != null) {
            Rect keyboardFrame = mKeyboardState.getKeyboardFrame();
            mPopupWindow.update(0, keyboardFrame.top + keyboardFrame.height(), -1, -1);
        }
    }

    private void removeCoverFromSuper() {
        if (mCoverView == null) {
            return;
        }

        ViewGroup parent = (ViewGroup) mCoverView.getParent();
        if (parent != null) {
            parent.removeView(mCoverView);
        }
    }

    public void setHideWhenKeyboardIsDismissed(boolean hideWhenKeyboardIsDismissed) {
        mHideWhenKeyboardIsDismissed = hideWhenKeyboardIsDismissed;

        if (mKeyboardState != null && mCoverView != null && !mKeyboardState.isKeyboardShowing()) {
            if (mHideWhenKeyboardIsDismissed) {
                mCoverView.setVisibility(GONE);
            } else {
                mCoverView.setVisibility(VISIBLE);
            }
        }
    }


    public void setKeyboardPlaceholderHeight(int keyboardPlaceholderHeight) {
        mKeyboardPlaceholderHeight = (int) (keyboardPlaceholderHeight * mScale);
        showKeyboardPlaceHolder(mKeyboardPlaceholderHeight);
    }

    public void setContentVisible(boolean contentVisible) {
        mContentVisible = contentVisible;

        if (contentVisible) {
            showOrUpdatePopupWindow();
        } else {
            hidePopupWindow();
        }
    }

    private void showKeyboardPlaceHolder(int keyboardPlaceholderHeight) {
        if (mContentView == null) {
            mKeyboardPlaceholderFrame = null;
        } else if (mKeyboardState != null) {
            Rect visibleViewArea = mKeyboardState.getVisibleViewArea();

            if (keyboardPlaceholderHeight > 0) {
                mKeyboardPlaceholderFrame = new Rect(visibleViewArea.left, visibleViewArea.bottom - keyboardPlaceholderHeight, visibleViewArea.right, visibleViewArea.bottom);

                if (!mKeyboardState.isKeyboardShowing()) {
                    resizeCover();
                    showOrUpdatePopupWindow();
                    receiveEvent(Events.EVENT_SHOW);
                }
            } else {
                mKeyboardPlaceholderFrame = null;
                if (!mKeyboardState.isKeyboardShowing()) {
                    resizeCover();
                    hidePopupWindow();
                    receiveEvent(Events.EVENT_HIDE);
                }
            }
        }
    }

    private void receiveEvent(Events event) {
        mEventEmitter.receiveEvent(getId(), event.toString(), null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (translationSlide != null) {
            translationSlide = null;
        }
    }

    int getNavigationSize() {
        if (mKeyboardState != null && mKeyboardState.isInitDataCompelete()) {
            if (mKeyboardState.isNavigationBarShow()) {
                return navigationBarHeight;
            } else {
                return 0;
            }
        }
        return 0;
    }
}
