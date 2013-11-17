package com.wsf_lp.mapapp.data;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.wsf_lp.oritsubushi.R;

public class CompletionDateGroup extends Group implements Parcelable {
	public static class DateSpan {
		public int begin;
		public int span;
		DateSpan(int begin, int span) { this.begin = begin; this.span = span; }
	};

	public CompletionDateGroup() {}

	@Override
	public String getTitle(Resources resources) {
		final int date = getCode();
		if(date == 0) {
			return resources.getString(R.string.group_completion_date_incomplete);
		} else if(date == 1) {
			return resources.getString(R.string.group_completion_date_ambigous);
		} else if(date <= 9999) {
			return resources.getString(R.string.group_completion_date_year, date);
		} else if(date <= 999912) {
			final int month = date % 100;
			return resources.getString(month > 0 ? R.string.group_completion_date_month : R.string.group_completion_date_year_ambigous, date / 100, month);
		} else {
			final int day = date % 100;
			return resources.getString(day > 0 ? R.string.group_completion_date_day : R.string.group_completion_date_month_ambigous, date / 10000, date / 100 % 100, day);
		}
	}

	/*@Override
	public String getHeaderTitle(Resources resources) {
		final int date = getCode();
		if(date == 0) {
			return resources.getString(R.string.group_completion_date_incomplete_short);
		} else if(date == 1) {
			return resources.getString(R.string.group_completion_date_ambigous_short);
		} else if(date <= 9999) {
			return resources.getString(R.string.group_completion_date_year_short, date);
		} else if(date <= 999912) {
			final int month = date % 100;
			return resources.getString(month > 0 ? R.string.group_completion_date_month_short : R.string.group_completion_date_year_ambigous_short, date / 100, month);
		} else {
			final int day = date % 100;
			return resources.getString(day > 0 ? R.string.group_completion_date_day_short : R.string.group_completion_date_month_ambigous_short, date / 10000, date / 100 % 100, day);
		}
	}*/

	/*public String getCellTitle(Resources resources) {
		final int date = getCode();
		if(date == 0) {
			return resources.getString(R.string.group_completion_date_incomplete_cell);
		} else if(date == 1) {
			return resources.getString(R.string.group_completion_date_ambigous_cell);
		} else if(date <= 9999) {
			return resources.getString(R.string.group_completion_date_year_cell, date);
		} else if(date <= 999912) {
			final int month = date % 100;
			return resources.getString(month > 0 ? R.string.group_completion_date_month_cell : R.string.group_completion_date_year_ambigous_cell, date / 100, month);
		} else {
			final int day = date % 100;
			return resources.getString(day > 0 ? R.string.group_completion_date_day_cell : R.string.group_completion_date_month_ambigous_cell, date / 10000, date / 100 % 100, day);
		}
	}*/

	@Override
	public String getDescription(Resources resource) {
		final int ratio = getRatio();
		return ratio >= 0 ? String.format(resource.getString(
				R.string.group_completion_date_statistics_formatter),
				getTotal(),
				getCompletions(),
				ratio / 10,
				ratio % 10)
			: "";
	}

	public static boolean nextIsStationPanel(Group group) {
		final int date = group.getCode();
		return date <= 1 || 9999 < date && date <= 999900 && date % 100 == 0;
	}

	public static DateSpan getCompletionDateSpan(Resources resources, String searchKeyword) {
		final boolean isAmbigous = searchKeyword.contains(resources.getString(R.string.group_completion_date_ambigous_label));
		int ints[] = { 0, 0, 0 };
		int index = 0;
		int diff = 0;
		for(int pos = 0; pos < searchKeyword.length() && index < 3; ++pos) {
			char ch = searchKeyword.charAt(pos);
			if('0' <= ch && ch <= '9') {
				ints[index] = ints[index] * 10 + ch - '0';
				diff = 1;
			} else {
				index += diff;
				diff = 0;
			}
		}
		switch(index) {
		case 0:
			return isAmbigous ? new DateSpan(1, 0) : null;
		case 1:
			return new DateSpan(ints[0] * 10000, isAmbigous ? 0 : 1231);
		case 2:
			return new DateSpan(ints[0] * 10000 + ints[1] * 100, isAmbigous ? 0 : 31);
		case 3:
			return isAmbigous ? null : new DateSpan(ints[0] * 10000 + ints[1] * 100 + ints[2], 0);
		default:
			return null;
		}
	}

	@Override
	public Drawable getStatusIcon(Resources resource) {
		//return getCode() > 0 ? super.getStatusIcon(resource) : resource.getDrawable(R.drawable.statusicon_incomp);
		if(getTotal() <= 0) {
			return resource.getDrawable(R.drawable.statusicon_notload);
		} else if(getCode() > 0) {
			return resource.getDrawable(R.drawable.statusicon_comp);
		} else {
			return resource.getDrawable(R.drawable.statusicon_incomp);
		}
	}

	public CompletionDateGroup(Parcel source) {
		super(source);
	}

	public static final Parcelable.Creator<CompletionDateGroup> CREATOR = new Creator<CompletionDateGroup>() {
		@Override
		public CompletionDateGroup createFromParcel(Parcel source) {
			return new CompletionDateGroup(source);
		}

		@Override
		public CompletionDateGroup[] newArray(int size) {
			return new CompletionDateGroup[size];
		}

	};
}
