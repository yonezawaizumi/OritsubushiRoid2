package com.wsf_lp.mapapp.data;

import com.wsf_lp.oritsubushi.R;

import android.content.res.Resources;

public class OperatorTypes {
	public static final int JR = 1;
	public static final int MAJOR = 2;
	public static final int SEMI_MAJOR = 3;
	public static final int MUNICIPAL = 4;
	public static final int LOCAL = 5;
	public static final int MONORAIL = 6;
	public static final int NTS = 7;
	public static final int FUNICULAR = 8;
	public static final int TROLLEY_BUS = 9;
	public static final int LEVITATED = 10;

	public static final boolean[] needsNoStatisticsLoad = new boolean[11];
	static {
		needsNoStatisticsLoad[LOCAL] = true;
	}

	public static final String[] getOperatorTypes(Resources resources) {
		return resources.getStringArray(R.array.operator_types);
	}

	public static final String getOperatorTypeName(int type, Resources resources) {
		String[] values = getOperatorTypes(resources);
		return 1 <= type && type < values.length ? values[type] : null;
	}

	public static final boolean needsNoStatisticsLoad(int type) {
		return 1 <= type && type < needsNoStatisticsLoad.length && needsNoStatisticsLoad[type];
	}

}
