package com.wsf_lp.mapapp.data;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public interface SimpleCellItem extends CellItem {
	public Drawable getCellIcon(Resources resources);
	public String getCellTitle(Resources resources);
	public String getCellDescription(Resources resources);
}
