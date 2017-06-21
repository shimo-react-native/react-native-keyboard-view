package im.shimo.react.keyboard;


import android.content.Context;
import android.view.MotionEvent;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.JSTouchDispatcher;
import com.facebook.react.uimanager.RootView;
import com.facebook.react.uimanager.TouchTargetHelper;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.view.ReactViewGroup;

public class KeyboardCoverView extends ReactViewGroup implements RootView {
    public KeyboardCoverView(Context context) {
        super(context);
    }

    private final JSTouchDispatcher mJSTouchDispatcher = new JSTouchDispatcher(this);
    private int mTargetTag = -1;
    private Callback mOnTouchOutsideCallback;

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getChildCount() > 0) {
            final int viewTag = getChildAt(0).getId();
            ReactContext reactContext = (ReactContext) getContext();
            reactContext.runOnNativeModulesQueueThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            ((ReactContext) getContext()).getNativeModule(UIManagerModule.class)
                                    .updateNodeSize(viewTag, w, h);
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


        int action = event.getAction() & MotionEvent.ACTION_MASK;

        int target = TouchTargetHelper.findTargetTagAndCoordinatesForTouch(
                event.getX(),
                event.getY(),
                this,
                new float[2],
                null
        );


        if (action == MotionEvent.ACTION_DOWN) {
            if (mTargetTag != -1) {
                return true;
            }

            mTargetTag = target;
        }

        // Touch on the KeyboardCoverView,
        // KeyboardCoverView will delegate the events to the outside ReactRootView.
        if (mTargetTag == this.getId()) {
            if (mOnTouchOutsideCallback != null) {
                mOnTouchOutsideCallback.invoke(event);
            }
        } else {
            mJSTouchDispatcher.handleTouchEvent(event, getEventDispatcher());
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mTargetTag = -1;
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

    public void setOnTouchOutsideCallback(Callback callback) {
        mOnTouchOutsideCallback = callback;
    }
}
