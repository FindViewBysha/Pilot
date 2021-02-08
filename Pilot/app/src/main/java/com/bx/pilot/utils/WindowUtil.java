package com.bx.pilot.utils;

import android.app.Activity;
import android.view.WindowManager;

public class WindowUtil {
    public static void lucencyBackground(Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = 0.3f;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        activity.getWindow().setAttributes(lp);
    }

    public static void cancelBackground(Activity activity) {
        WindowManager.LayoutParams lp1 = activity.getWindow().getAttributes();
        lp1.alpha = 1f;
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        activity.getWindow().setAttributes(lp1);
    }
}
