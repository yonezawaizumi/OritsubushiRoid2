package com.wsf_lp.android;

import java.util.ArrayList;
import java.util.List;

import com.wsf_lp.oritsubushi.R;

import android.content.res.Resources;

public class Prefs {
    public static final int HOKKAIDO = 1;
    public static final int AOMORI = 2;
    public static final int IWATE = 3;
    public static final int MIYAGI = 4;
    public static final int AKITA = 5;
    public static final int YAMAGATA = 6;
    public static final int FUKUSHIMA = 7;
    public static final int IBARAKI = 8;
    public static final int TOCHIGI = 9;
    public static final int GUMMA = 10;
    public static final int SAITAMA = 11;
    public static final int CHIBA = 12;
    public static final int TOKYO = 13;
    public static final int KANAGAWA = 14;
    public static final int NIIGATA = 15;
    public static final int TOYAMA = 16;
    public static final int ISHIKAWA = 17;
    public static final int FUKUI = 18;
    public static final int YAMANASHI = 19;
    public static final int NAGANO = 20;
    public static final int GIFU = 21;
    public static final int SHIZUOKA = 22;
    public static final int AICHI = 23;
    public static final int MIE = 24;
    public static final int SHIGA = 25;
    public static final int KYOTO = 26;
    public static final int OSAKA = 27;
    public static final int HYOGO = 28;
    public static final int NARA = 29;
    public static final int WAKAYAMA = 30;
    public static final int TOTTORI = 31;
    public static final int SHIMANE = 32;
    public static final int OKAYAMA = 33;
    public static final int HIROSHIMA = 34;
    public static final int YAMAGUCHI = 35;
    public static final int TOKUSHIMA = 36;
    public static final int KAGAWA = 37;
    public static final int EHIME = 38;
    public static final int KOUCHI = 39;
    public static final int FUKUOKA = 40;
    public static final int SAGA = 41;
    public static final int NAGASAKI = 42;
    public static final int KUMAMOTO = 43;
    public static final int OOITA = 44;
    public static final int MIYAZAKI = 45;
    public static final int KAGOSHIMA = 46;
    public static final int OKINAWA = 47;

    public static String[] getValues(Resources resources) {
    	return resources.getStringArray(R.array.prefs);
    }
    public static String getValue(int pref, Resources resources) {
    	final String[] values = getValues(resources);
    	return 1 <= pref && pref < values.length ? values[pref] : null;
    }
	public static List<Integer> getMatchedPrefs(String prefix, Resources resources) {
    	final String[] values = getValues(resources);
		ArrayList<Integer> results = new ArrayList<Integer>();
		for(int pref = 1; pref < values.length; ++pref) {
			if(values[pref].contains(prefix)) {
				results.add(pref);
			}
		}
		return results;
	}
}
