package im.shimo.react.keyboard;

import android.view.View;

/**
 * Created by song on 2018/1/17.
 */

public class KeyboardStateFactory {
    final static String SMARTISAN = "SMARTISAN";

    public static AbstractKeyboardState create(View rootView, int navigationBarHeight, int statusBarHeight) {
        if (android.os.Build.BRAND.contains(SMARTISAN)) {
            return new KeyboardStateSmartianOS(rootView, navigationBarHeight, statusBarHeight);
        } else {
            return new KeyboardStateNormal(rootView, navigationBarHeight, statusBarHeight);
        }
    }
}
