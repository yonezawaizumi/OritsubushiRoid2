package com.wsf_lp.android;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class AndroidUtils {
	public static final String TAG = "AndroidUtils";
	public static void dumpViewTree(View v, String padding) {
		Log.d(TAG, padding + v.getClass().getName());
		if(v instanceof ViewGroup) {
			ViewGroup g = (ViewGroup)v;
			for(int i = 0; i < g.getChildCount(); i++){
				dumpViewTree(g.getChildAt(i), padding + ' ');
			}
		}
	}

	public static String getResourceClassName(String customCanonicalName) {
		int pos = customCanonicalName.lastIndexOf('.');
		String name = customCanonicalName.substring(0, pos);
		pos = name.lastIndexOf('.');
		return name.substring(0, pos + 1) + 'R';
	}
}
