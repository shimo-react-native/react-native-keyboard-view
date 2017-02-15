package im.shimo.react.keyboard;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

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

    public KeyboardRootViewGroup(Context context, KeyboardView delegate) {
        super(context);
        mDelegatedKeyboardView = delegate;
    }

    public void setContentVisible(boolean contentVisible) {
        ViewGroup container = (ViewGroup) getChildAt(0);
        if (contentVisible) {
            container.getChildAt(0).setVisibility(View.VISIBLE);
        } else {
            container.getChildAt(0).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getChildCount() > 0) {
            ((ReactContext) getContext()).runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                    .updateNodeSize(getChildAt(0).getId(), w, h);
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
        try {
            // Try to find the target view in KeyboardRootViewGroup.
            // If something went wrong there, then delegate the onTouchEvent to ReactRootView.
            TouchTargetHelper.findTargetTagAndCoordinatesForTouch(
                    event.getX(),
                    event.getY(),
                    this,
                    new float[2],
                    null
            );

            mJSTouchDispatcher.handleTouchEvent(event, getEventDispatcher());
            // In case when there is no children interested in handling touch event, we return true from
            // the root view in order to receive subsequent events related to that gesture
            return true;
        } catch (Exception e) {
            mDelegatedKeyboardView.onTouchEvent(event);
            return false;
        }
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

    public boolean hasContent() {
        ViewGroup container = (ViewGroup) getChildAt(0);
        return container != null && container.getChildCount() > 0;
    }
}
