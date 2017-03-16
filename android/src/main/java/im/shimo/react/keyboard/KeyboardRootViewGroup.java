package im.shimo.react.keyboard;

import android.content.Context;
import android.support.annotation.Nullable;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.JSTouchDispatcher;
import com.facebook.react.uimanager.RootView;
import com.facebook.react.uimanager.TouchTargetHelper;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.view.ReactViewGroup;

class KeyboardRootViewGroup extends ReactViewGroup implements RootView {

    private final JSTouchDispatcher mJSTouchDispatcher = new JSTouchDispatcher(this);
    private KeyboardView mDelegatedKeyboardView;
    private int mTargetTag = -1;

    private ViewGroup mContainerView;

    public KeyboardRootViewGroup(Context context, KeyboardView delegate) {
        super(context);
        mDelegatedKeyboardView = delegate;
    }

    @Override
    public void addView(View child, int index) {
        // Assume the first child is the container ViewGroup
        if (index == 0) {
            mContainerView = (ViewGroup) child;
        }

        super.addView(child, index);
    }


    @Override
    public void removeView(View child) {
        if (child == mContainerView) {
            mContainerView = null;
        }
        super.removeView(child);
    }

    @Override
    public void removeViewAt(int index) {
        removeView(getChildAt(index));
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mContainerView != null) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                    .updateNodeSize(mContainerView.getId(), w, h);
                        }
                    });
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !onTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // In case when there is no children interested in handling touch event, we return true from
        // the root view in order to receive subsequent events related to that gesture


        if (mContainerView != null) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;

            int target = TouchTargetHelper.findTargetTagAndCoordinatesForTouch(
                    event.getX() - mContainerView.getTranslationX(),
                    event.getY() - mContainerView.getTranslationY(),
                    mContainerView,
                    new float[2],
                    null
            );


            if (action == MotionEvent.ACTION_DOWN) {
                if (mTargetTag != -1) {
                    return true;
                }

                mTargetTag = target;
            }
            if (mTargetTag == mContainerView.getId()) {
                mDelegatedKeyboardView.onTouchEvent(event);
            } else {
                mJSTouchDispatcher.handleTouchEvent(event, getEventDispatcher());
            }

            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                mTargetTag = -1;
            }
        }

        return true;
    }

    @Override
    public void onChildStartedNativeGesture(MotionEvent androidEvent) {
        mJSTouchDispatcher.onChildStartedNativeGesture(androidEvent, getEventDispatcher());
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // No-op - override in order to still receive events to onInterceptTouchEvent
        // even when some other view disallow that
    }

    private EventDispatcher getEventDispatcher() {
        ReactContext reactContext = (ReactContext) getContext();
        return reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
    }

    public void setContentHeight(final int contentHeight) {
        ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                new Runnable() {
                    @Override
                    public void run() {
                        int content = getContentView().getId();
                        ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                .updateNodeSize(content, ViewGroup.LayoutParams.MATCH_PARENT, contentHeight);
                    }
                });

    }

    public View getContentView() {
        return mContainerView.getChildAt(mContainerView.getChildCount() - 1);
    }

    public void setContentVisible(boolean contentVisible) {
        getContentView().setVisibility(contentVisible ? View.VISIBLE : View.GONE);
    }
}
