package im.shimo.react.keyboard;

import java.util.ArrayList;

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
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;

/**
 *
 * ContentView is layout to cover the keyboard,
 * CoverView is layout to fill the rest part on the screen.
 *
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
 *
 */


public class KeyboardView extends ReactRootAwareViewGroup implements LifecycleEventListener {
    private @Nullable PopupWindow mPopupWindow;
    private @Nullable KeyboardState mKeyboardState;
    private @Nullable View mContentView;
    private @Nullable View mCoverView;
    private int mChildCount = 0;
    private KeyboardState.OnKeyboardChangeListener mOnKeyboardChangeListener;
    private ActivityEventListener mActivityEventListener;

    public KeyboardView(ThemedReactContext context) {
        super(context);
        bindKeyboardState();
        context.addLifecycleEventListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing as we are laid out by UIManager
    }

    @Override
    public void addView(View child, int index) {
        if (child instanceof KeyboardContentView) {
            mContentView = child;
            mChildCount++;
            if (mKeyboardState != null) {
                showOrUpdatePopupWindow();
            }
        } else if (child instanceof KeyboardCoverView) {
            mCoverView = child;
            mChildCount++;
            resizeCover();
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

    @Override
    public void removeView(View child) {
        if (child instanceof KeyboardContentView) {
            dismissPopupWindow();
            mContentView = null;
            mChildCount--;
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
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resizeCover();
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
        ((ReactContext) getContext()).removeLifecycleEventListener(this);
        dismissPopupWindow();
        removeCoverFromSuper();
        unbindKeyboardState();
    }

    @Override
    public void onHostResume() {
        if (mKeyboardState != null) {
            showOrUpdatePopupWindow();
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
        final ReactContext context = (ReactContext) getContext();
        Activity activity = ((ReactContext) getContext()).getCurrentActivity();
        if (activity != null) {
            mOnKeyboardChangeListener = new KeyboardState.OnKeyboardChangeListener() {
                @Override
                public void onKeyboardShown(Rect keyboardFrame) {
                    showOrUpdatePopupWindow(keyboardFrame);
                    resizeCover();
                }

                @Override
                public void onKeyboardClosed() {
                    hidePopupWindow();
                    resizeCover();
                }
            };
            mKeyboardState = new KeyboardState(activity.findViewById(android.R.id.content));
            mKeyboardState.addOnKeyboardChangeListener(mOnKeyboardChangeListener);
        } else if (mActivityEventListener == null)  {
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

    private void showOrUpdatePopupWindow() {
        if (mKeyboardState != null) {
            showOrUpdatePopupWindow(mKeyboardState.getKeyboardFrame());
        }
    }

    private void showOrUpdatePopupWindow(final Rect keyboardFrame) {
        if (mContentView != null) {
            if (mPopupWindow == null) {
                mPopupWindow = new PopupWindow(mContentView, keyboardFrame.width(), keyboardFrame.height());
                mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                mPopupWindow.setAnimationStyle(R.style.DialogAnimationSlide);
                mPopupWindow.setClippingEnabled(false);
                mPopupWindow.showAtLocation(getRootView(), Gravity.NO_GRAVITY, 0, keyboardFrame.top);
            } else {
                mPopupWindow.update(0, keyboardFrame.top, keyboardFrame.width(), keyboardFrame.height());
            }

            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mContentView !=  null) {
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                        .updateNodeSize(mContentView.getId(), keyboardFrame.width(), keyboardFrame.height());
                            }
                        }
                    });
        }
    }

    private void resizeCover() {
        ReactRootView reactRootView = getReactRootView();

        if (mKeyboardState == null || mCoverView == null || reactRootView == null) {
            return;
        }

        if (mCoverView.getParent() == null) {
            reactRootView.addView(mCoverView);
        }

        int rootHeight = reactRootView.getHeight();
        Rect keyboardFrame = mKeyboardState.getKeyboardFrame();
        final ReactContext context = (ReactContext)getContext();
        final int coverViewWidth = keyboardFrame.width();
        final int coverViewHeight = rootHeight - keyboardFrame.height();

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

        ViewGroup parent = (ViewGroup)mCoverView.getParent();
        if (parent != null) {
            parent.removeView(mCoverView);
        }
    }
}
