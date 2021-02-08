package com.bx.pilot.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

public class CommonUtil {
    /**
     * 获取屏幕宽度
     *
     * @param context 上下文
     * @return 屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;

    }

    /**
     * 获取屏幕高度
     *
     * @param context 上下文
     * @return 屏幕高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
