package im.shimo.react.keyboard;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ReactShadowNode;
import com.facebook.yoga.YogaJustify;

public class KeyboardViewShadowView extends LayoutShadowNode {
    private static final Point MIN_POINT = new Point();
    private static final Point MAX_POINT = new Point();
    private static final Point SIZE_POINT = new Point();

    @Override
    public void addChildAt(ReactShadowNode child, int i) {
        super.addChildAt(child, i);
        WindowManager wm = (WindowManager) getThemedContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = Assertions.assertNotNull(wm).getDefaultDisplay();
        // getCurrentSizeRange will return the min and max width and height that the window can be
        display.getCurrentSizeRange(MIN_POINT, MAX_POINT);
        // getSize will return the dimensions of the screen in its current orientation
        display.getSize(SIZE_POINT);

        int width;
        int height;

        if (SIZE_POINT.x < SIZE_POINT.y) {
            // If we are vertical the width value comes from min width and height comes from max height
            width = MIN_POINT.x;
            height = MAX_POINT.y;
        } else {
            // If we are horizontal the width value comes from max width and height comes from min height
            width = MAX_POINT.x;
            height = MIN_POINT.y;
        }

        child.setStyleWidth(width);
        child.setStyleHeight(height);
        child.setJustifyContent(YogaJustify.FLEX_END);
    }
}
