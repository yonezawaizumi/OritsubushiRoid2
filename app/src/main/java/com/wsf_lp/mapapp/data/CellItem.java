package com.wsf_lp.mapapp.data;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public interface CellItem {
	public long getCellId();
	public String getTitle(Resources resources);
	public String getDescription(Resources resources);
	public Drawable getStatusIcon(Resources resources);
}
