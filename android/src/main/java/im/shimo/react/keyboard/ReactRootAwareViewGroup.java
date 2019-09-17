package im.shimo.react.keyboard;

import android.content.Context;
import androidx.annotation.Nullable;
import android.view.ViewParent;

import com.facebook.react.ReactRootView;
import com.facebook.react.views.view.ReactViewGroup;

public class ReactRootAwareViewGroup extends ReactViewGroup {
    private @Nullable ReactRootView mReactRootView;

    public ReactRootAwareViewGroup(Context context) {
        super(context);
    }

    protected  @Nullable ReactRootView getReactRootView() {
        if (mReactRootView == null) {
            ViewParent parent = getParent();

            while (parent != null && !(parent instanceof ReactRootView)) {
                if (parent instanceof ReactRootAwareViewGroup) {
                    ReactRootAwareViewGroup reactRootAwareViewGroup = (ReactRootAwareViewGroup)parent;
                    parent = reactRootAwareViewGroup.getReactRootView();
                } else {
                    parent = parent.getParent();
                }
            }
            mReactRootView = (ReactRootView) parent;
        }

        return mReactRootView;
    }
}
