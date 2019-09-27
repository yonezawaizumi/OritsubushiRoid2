package com.wsf_lp.mapapp.data;

import com.wsf_lp.oritsubushi.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.res.ResourcesCompat;

public class Group implements Parcelable, CellItem {
	private int code;
	private String headerTitle;
	private String title;
	private int total;
	private int completions;

	public Group() {}
	public Group(Group group) {
		code = group.code;
		headerTitle = group.headerTitle;
		title = group.title;
		total = group.total;
		completions = group.completions;
	}

	public void copyStatistics(Group group) {
		total = group.total;
		completions = group.completions;
	}

	public int getCode() { return code; }
	public void setCode(int code) { this.code = code; }
	public String getTitle(Resources resources) { return title != null ? title : ""; }
	public void setTitle(String title) { this.title = title; }
	public String getHeaderTitle(Resources resources) {
		return headerTitle != null ? headerTitle : getTitle(resources);
	}
	public void setHeaderTitle(String headerTitle) { this.headerTitle = headerTitle; }
	public int getTotal() { return total; }
	public void setTotal(int total) { this.total = total; }
	public int getCompletions() { return completions; }
	public void setCompletions(int completions) { this.completions = completions; }
	public int getIncompletions() { return total - completions; }
	public int getRatio() { return total > 0 ? completions * 1000 / total : -1; }
	@Override
	public String getDescription(Resources resource) {
		final int ratio = getRatio();
		return ratio >= 0 ? String.format(resource.getString(
				R.string.group_statistics_formatter),
				getTotal(),
				getCompletions(),
				ratio / 10,
				ratio % 10,
				getTotal() - getCompletions())
			: "";
	}
	public static Drawable getStatusIcon(final Resources resources, final int completions, final int total) {
	    int id;
	    if(total <= 0) {
	    	id = R.drawable.statusicon_notload;
	    } else if(total > completions) {
	    	id = R.drawable.statusicon_incomp;
	    } else {
	    	id = R.drawable.statusicon_comp;
	    }
	    return ResourcesCompat.getDrawable(resources, id, null);
	}

	@Override
	public Drawable getStatusIcon(Resources resource) {
		return getStatusIcon(resource, getCompletions(), getTotal());
	}

	public Group(Parcel source) {
		code = source.readInt();
		headerTitle = source.readString();
		title = source.readString();
		total = source.readInt();
		completions = source.readInt();
	}

	public static final Parcelable.Creator<Group> CREATOR = new Creator<Group>() {
		@Override
		public Group createFromParcel(Parcel source) {
			return new Group(source);
		}

		@Override
		public Group[] newArray(int size) {
			return new Group[size];
		}

	};
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(code);
		dest.writeString(headerTitle);
		dest.writeString(title);
		dest.writeInt(total);
		dest.writeInt(completions);
	}

	@Override
	public long getCellId() {
		return getCode();
	}
}
