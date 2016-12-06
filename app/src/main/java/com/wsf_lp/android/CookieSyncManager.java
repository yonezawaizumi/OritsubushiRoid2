package com.wsf_lp.android;

import android.content.Context;
import android.os.Build;

/**
 * Created by yonezawaizumi on 2016/11/30.
 */

public class CookieSyncManager {

    @SuppressWarnings("deprecation")
    public static void createInstance(Context context) {
        if (Build.VERSION.SDK_INT < 21) {
            android.webkit.CookieSyncManager.createInstance(context);
        }
    }

    @SuppressWarnings("deprecation")
    public static void startSync() {
        if (Build.VERSION.SDK_INT < 21) {
            android.webkit.CookieSyncManager.getInstance().startSync();
        }
    }

    @SuppressWarnings("deprecation")
    public static void stopSync() {
        if (Build.VERSION.SDK_INT < 21) {
            android.webkit.CookieSyncManager.getInstance().stopSync();
        }
    }

    @SuppressWarnings("deprecation")
    public static void sync() {
        if (Build.VERSION.SDK_INT < 21) {
            android.webkit.CookieSyncManager.getInstance().sync();
        }
    }
}
