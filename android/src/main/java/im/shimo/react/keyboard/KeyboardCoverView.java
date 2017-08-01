package im.shimo.react.keyboard;


import android.content.Context;
import android.view.MotionEvent;

import com.facebook.react.uimanager.PointerEvents;
import com.facebook.react.views.view.ReactViewGroup;

public class KeyboardCoverView extends ReactViewGroup {
    public KeyboardCoverView(Context context) {
        super(context);
    }

    @Override
    public PointerEvents getPointerEvents() {
        // Override getPointerEvents or it will return PointerEvents.AUTO.
        return PointerEvents.BOX_NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Override onTouchEvent or it will return true.
        return false;
    }
}
