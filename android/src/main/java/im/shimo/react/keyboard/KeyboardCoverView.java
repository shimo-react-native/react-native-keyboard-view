package im.shimo.react.keyboard;


import android.content.Context;
import androidx.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.react.uimanager.PointerEvents;
import com.facebook.react.views.view.ReactViewGroup;

public class KeyboardCoverView extends ReactViewGroup {
    private volatile PointerEvents pointerEvents = PointerEvents.BOX_NONE;

    public KeyboardCoverView(Context context) {
        super(context);
    }

    @Override
    public PointerEvents getPointerEvents() {
        // Override getPointerEvents or it will return PointerEvents.AUTO.
        return pointerEvents;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Override onTouchEvent or it will return true.
        return false;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            pointerEvents = PointerEvents.BOX_NONE;
        } else {
            pointerEvents = PointerEvents.NONE;
        }
    }
}
