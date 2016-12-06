package com.wsf_lp.mapapp.data;

import com.wsf_lp.oritsubushi.R;

import android.content.res.Resources;

public class DatabaseFilter {
    public static final int NONE = 0;
    public static final int PREF = 1;
    public static final int NAME_FORWARD = 2;
    public static final int NAME = 3;
    public static final int YOMI_FORWARD = 4;
    public static final int YOMI = 5;
    public static final int LINE_FORWARD = 6;
    public static final int LINE = 7;
    public static final int DATE = 8;
    public static final int NUM_TYPES = 9;
    
    public static String getFormattedString(Resources resources, int filterType, String searchKeyword) {
        if(filterType >= NUM_TYPES || filterType < 0 || searchKeyword.length() == 0) {
            return "";
        }
        String[] labels = resources.getStringArray(R.array.dbfilter_prefix_labels);
        StringBuilder builder = new StringBuilder(labels[filterType]);
        builder.append(':');
        builder.append(searchKeyword);
        builder.append(' ');
        labels = resources.getStringArray(R.array.dbfilte_suffix_labels);
        builder.append(labels[filterType]);
        return builder.toString();
    	
    }


}
