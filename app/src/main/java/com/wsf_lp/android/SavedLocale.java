package com.wsf_lp.android;

import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Created by yonezawaizumi on 2016/12/01.
 */

public class SavedLocale {

    private Locale mLocale;
    private LocaleList mLocaleList;

    @SuppressWarnings("deprecation")
    public boolean compareAndSave(Configuration configuration) {
        if (Build.VERSION.SDK_INT < 24) {
            Locale locale = configuration.locale;
            if (locale.equals(mLocale)) {
                return false;
            } else {
                mLocale = locale;
                return true;
            }
        } else {
            LocaleList localeList = configuration.getLocales();
            if (localeList.equals(mLocaleList)) {
                return false;
            } else {
                mLocaleList = localeList;
                return true;
            }
        }
    }

}
