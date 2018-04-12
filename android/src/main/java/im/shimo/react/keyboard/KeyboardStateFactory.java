package im.shimo.react.keyboard;

import android.os.Build;
import android.view.View;


/**
 * Created by song on 2018/1/17.
 */

public class KeyboardStateFactory {
    private final static String SMARTISAN = "SMARTISAN";
    private final static String MIX2 = "MIX 2";
    private final static String VIVOX21A = "vivo X21A";

    public static AbstractKeyboardState create(View rootView, int navigationBarHeight, int statusBarHeight) {
        if (Build.BRAND.contains(SMARTISAN)
                || (Build.MODEL.equals(MIX2) && Build.VERSION.SDK_INT >= 26)) {
            return new KeyboardStateSmartianOS(rootView, navigationBarHeight, statusBarHeight);
        } else if (Build.MODEL.equals(VIVOX21A)) {
            return new KeyboardStateVivoX21OS(rootView, navigationBarHeight, statusBarHeight);
        } else {
            return new KeyboardStateNormal(rootView, navigationBarHeight, statusBarHeight);
        }
    }
}
