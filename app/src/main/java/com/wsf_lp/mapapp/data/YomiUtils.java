package com.wsf_lp.mapapp.data;

public class YomiUtils {

	public static int getYomi1Code(final String yomi) {
		return yomi.charAt(0);
	}

	public static int getYomi2Code(final String yomi) {
		final int code = getYomi1Code(yomi);
		return yomi.length() > 1 ? code | (int)yomi.charAt(1) << 16 : code;
	}


}
