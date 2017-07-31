package im.shimo.react.keyboard;


import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;


import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Callback;
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


public class KeyboardView extends ViewGroup implements LifecycleEventListener {
    private PopupWindow mPopupWindow;
    private @Nullable KeyboardState mKeyboardState;
    private @Nullable ReactRootView mReactRootView;
    private View mContentView;
    private View mCoverView;
    private boolean mCoverVisible;

    private int mChildCount = 0;
    private KeyboardState.OnKeyboardChangeListener mOnKeyboardChangeListener;

    public KeyboardView(ThemedReactContext context) {
        super(context);
        context.addLifecycleEventListener(this);
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
            if (mKeyboardState != null && mKeyboardState.isKeyboardShowing()) {
                showPopupWindow(mKeyboardState.getKeyboardWidth(), mKeyboardState.getKeyboardHeight());
            }
        } else if (child instanceof KeyboardCoverView) {
            if (mCoverView != null) {
                removeView(mCoverView);
            }

            mCoverView = child;
            ((KeyboardCoverView)mCoverView).setOnTouchOutsideCallback(new Callback() {
                @Override
                public void invoke(Object... args) {
                    onTouchEvent((MotionEvent)args[0]);
                }
            });
            mChildCount++;
            if (mKeyboardState != null && mKeyboardState.isKeyboardShowing()) {
                showCover(mKeyboardState.getKeyboardWidth(), mKeyboardState.getKeyboardHeight());
            }
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
            if (mContentView != null) {
                mContentView = null;
                dismissPopupWindow();
                mChildCount--;
            }
        } else if (child instanceof KeyboardCoverView) {
            if (mCoverView != null) {
                mCoverView = null;
                dismissCoverView();
                mChildCount--;
            }
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
            dismissCoverView();
            mKeyboardState.removeOnKeyboardChangeListener(mOnKeyboardChangeListener);
            mKeyboardState = null;
        }
    }

    @Override
    public void onHostResume() {
        bindKeyboardState();
        if (mKeyboardState != null  && mKeyboardState.isKeyboardShowing()) {
            int width = mKeyboardState.getKeyboardWidth();
            int height = mKeyboardState.getKeyboardHeight();
            showPopupWindow(width, height);
            showCover(width, height);
        }
    }

    @Override
    public void onHostPause() {
        dismissPopupWindow();
        dismissCoverView();

        if (mKeyboardState != null) {
            mKeyboardState.removeOnKeyboardChangeListener(mOnKeyboardChangeListener);
            mKeyboardState = null;
        }
    }

    @Override
    public void onHostDestroy() {
        onDropInstance();
    }

    private void bindKeyboardState() {
        FrameLayout rootLayout = getRootContent();

        if (rootLayout != null) {
            mKeyboardState = new KeyboardState(rootLayout);
            mOnKeyboardChangeListener = new KeyboardState.OnKeyboardChangeListener() {
                @Override
                public void onKeyboardShown(int keyboardWidth, int keyboardHeight) {
                    showPopupWindow(keyboardWidth, keyboardHeight);
                    showCover(keyboardWidth, keyboardHeight);
                }

                @Override
                public void onKeyboardClosed() {
                    dismissPopupWindow();
                    dismissCoverView();
                }
            };

            mKeyboardState.addOnKeyboardChangeListener(mOnKeyboardChangeListener);
        }
    }

    private void showPopupWindow(final int width, final int height) {
        if (mContentView != null) {
            if (mPopupWindow == null) {
                mPopupWindow = new PopupWindow(mContentView, width, height);
                mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                mPopupWindow.setAnimationStyle(R.style.DialogAnimationSlide);
                mPopupWindow.showAtLocation(getRootView(), Gravity.BOTTOM, 0, 0);
            } else {
                mPopupWindow.update(width, height);
            }

            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mContentView != null) { // async, mContentView may be null
                                ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                    .updateNodeSize(mContentView.getId(), width, height);
                            }
                        }
                    });
        }
    }

    private void showCover(final int width, final int height) {
        final ReactContext context = (ReactContext)getContext();
        final FrameLayout rootLayout = getRootContent();

        if (mCoverView != null && rootLayout != null) {
            final int coverHeight = rootLayout.getHeight() - height;
            context.runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!mCoverVisible) {
                                mCoverVisible = true;
                                // Add cover view after cover view has been layout.
                                context.runOnUiQueueThread(new Runnable() {
                                    @Override
                                    public void run() { // async, mCoverView may be null
                                        if (mCoverView != null) {
                                            removeCoverFromSuper();
                                            rootLayout.addView(mCoverView);
                                        }
                                    }
                                });
                            }

                            context.getNativeModule(UIManagerModule.class)
                                    .updateNodeSize(mCoverView.getId(), width, coverHeight);
                        }
                    }
            );

        }
    }


    private @Nullable FrameLayout getRootContent() {
        final ReactContext context = (ReactContext)getContext();
        final Activity activity = context.getCurrentActivity();

        if (activity != null) {
            return (FrameLayout)activity.findViewById(android.R.id.content);
        }

        return null;
    }

    private void dismissPopupWindow() {
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
    }

    private void dismissCoverView() {
        mCoverVisible = false;
        if (mCoverView != null) {
            removeCoverFromSuper();
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

    private ReactRootView getReactRootView() {
        if (mReactRootView == null) {
            ViewParent parent = getParent();

            while (parent != null && !(parent instanceof ReactRootView)) {
                parent = parent.getParent();
            }
            mReactRootView = (ReactRootView) parent;
        }

        return mReactRootView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getReactRootView().dispatchTouchEvent(event);
        return false;
    }

}
