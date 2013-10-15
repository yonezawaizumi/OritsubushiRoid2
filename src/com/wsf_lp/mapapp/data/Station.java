package com.wsf_lp.mapapp.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import com.wsf_lp.oritsubushi.R;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.google.android.maps.GeoPoint;

public class Station implements Parcelable, CellItem {
	public static final int INCOMPLETE = 0;
	public static final int UNANBIGUOUS_YEAR = 1;
	public static final int UNANBIGUOUS_MONTH = 0;
	public static final int UNANBIGUOUS_DAY = 0;

	private int code;
	private int lat1E6;
	private int lng1E6;
	private transient GeoPoint point;
	private String name;

	private transient double distance = -1.0;

	private String yomi;
	private String wiki;
	private int pref;
	private String address;
	private int operatorCode;
	private Operator operator;
	private ArrayList<Line> lines;

	private int completionDate;
	private String memo;

	public Station() {
		lines = new ArrayList<Line>();
	}

	@SuppressWarnings("unchecked")
	public Station(Station src) {
		code = src.code;
		lat1E6 = src.lat1E6;
		lng1E6 = src.lng1E6;
		point = src.point;
		name = src.name;
		yomi = src.yomi;
		wiki = src.wiki;
		pref = src.pref;
		address = src.address;
		operatorCode = src.operatorCode;
		operator = src.operator;
		lines = (ArrayList<Line>)src.lines.clone();
		completionDate = src.completionDate;
		memo = src.memo;
	}

	@Override
	public boolean equals(Object another) {
		return another instanceof Station && ((Station)another).getCode() == getCode();
	}

	public int getCode() { return code; }
	protected void setCode(int code) { this.code = code; }
	public int getLatitudeE6() { return lat1E6; }
	public int getLongitudeE6() { return lng1E6; }
	public GeoPoint getPoint() {
		if(point == null) {
			point = new GeoPoint(lat1E6, lng1E6);
		}
		return point;
	}
	protected void setPoint(GeoPoint point) {
		this.point = point;
		this.lat1E6 = point.getLatitudeE6();
		this.lng1E6 = point.getLongitudeE6();
	}
	//別スレッドからGeoPointを生成すると極端に遅くなる問題への対処
	protected void setPoint(int latitudeE6, int longitudeE6) {
		lat1E6 = latitudeE6;
		lng1E6 = longitudeE6;
		point = null;
	}
	public String getName() { return name; }
	protected void setName(String name) { this.name = name; }
	public String getYomi() { return yomi; }
	public void setYomi(String yomi) { this.yomi = yomi; }
	public String getWiki() {
		return wiki != null && wiki.length() > 0 ? wiki : name;
	}
	public void setWiki(String wiki) { this.wiki = wiki; }
	public int getPref() { return pref; }
	public void setPref(int pref) { this.pref = pref; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	private void setOperatorCode(int operatorCode) { this.operatorCode = operatorCode; }
	public Operator getOperator() {
		return operator;
	}
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	public ArrayList<Line> getLines() {
		return lines;
	}

	public boolean isReadyToCreateSubtitle() { return !getLines().isEmpty(); }

	public String getTitle() { return getName(); }
	public String getSubtitle() {
		if(getOperator() == null) {
			return "-";
			}
			StringBuilder builder = new StringBuilder(getOperator().getName());
			builder.append(" / ");
			for(Line line : getLines()) {
				builder.append(line.getName());
				builder.append(", ");
			}
		return builder.substring(0, builder.length() - 2);
	}

	public static int calcCompletionDateInt(int year, int month, int day) {
		if(year == UNANBIGUOUS_YEAR) {
			return 1;
		} else if(month == UNANBIGUOUS_MONTH) {
			return year * 10000;
		} else if(day == UNANBIGUOUS_DAY) {
			return year * 10000 + month * 100;
		} else {
			return year * 10000 + month * 100 + day;
		}
	}
	public static int calcCompletionDateInt(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calcCompletionDateInt(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE));
	}
	public static int calcYear(int date) {
		return date == 1 ? UNANBIGUOUS_YEAR : date / 10000;
	}
	public static int calcMonth(int date) {
		return date == 1 ? UNANBIGUOUS_MONTH : date / 100 % 100;
	}
	public static int calcDay(int date) {
		return date == 1 ? UNANBIGUOUS_DAY : date % 100;
	}
	public int getCompletionDate() { return completionDate; }
	public boolean isCompleted() { return completionDate > 0; }
	public String getCompletionDateString(Resources resources) {
	    if(!isCompleted()) {
	        return resources.getString(R.string.complete_date_incomplete);
	    } else if(completionDate < 19000000) {
	        return resources.getString(R.string.complete_date_unknown);
	    }
	    int year = completionDate / 10000;
	    int month = completionDate % 10000 / 100;
	    int day = completionDate % 100;
	    if(month == 0) {
	        return resources.getString(R.string.complete_date_year, year);
	    } else if(day == 0) {
	        return resources.getString(R.string.complete_date_month, year, month);
	    } else {
	        return resources.getString(R.string.complete_date_day, year, month, day);
	    }
	}
	public String getCompletionDateShortString(Resources resources) {
		if(!isCompleted()) {
			return resources.getString(R.string.complete_date_incomplete_short);
	    } else if(completionDate < 19000000) {
	        return resources.getString(R.string.complete_date_unknown_short);
	    }
	    int year = completionDate / 10000;
	    int month = completionDate % 10000 / 100;
	    int day = completionDate % 100;
	    if(month == 0) {
	        return resources.getString(R.string.complete_date_year_short, year);
	    } else if(day == 0) {
	        return resources.getString(R.string.complete_date_month_short, year, month);
	    } else {
	        return resources.getString(R.string.complete_date_day_short, year, month, day);
	    }
	}
	public void setCompletionDate(int completionDate) { this.completionDate = completionDate; }
	public void setCompletionToday() {
		setCompletionDate(calcCompletionDateInt(new Date()));
	}
	public String getMemo() { return memo; }
	public void setMemo(String memo) { this.memo = memo; }
	public int getPinId() {
		return isCompleted() ? R.drawable.pin_green : R.drawable.pin_red;
	}
	public int getIconId() {
		return isCompleted() ? R.drawable.statusicon_comp : R.drawable.statusicon_incomp;
	}

    //implementation
	//calls from Database Worker Thread only
    public void setFromCursor(Cursor cursor, SparseArray<Operator> operators) {
		if(completionDateIndex < 0) {
			initializeIndexes(cursor);
		}
		setCode(cursor.getInt(0));
		setName(cursor.getString(1));
		setYomi(cursor.getString(2));
		setWiki(cursor.getString(3));
		setPref(cursor.getInt(4));
		setAddress(cursor.getString(5));
		setPoint(cursor.getInt(6), cursor.getInt(7));
		int operator = cursor.getInt(8);
		setOperatorCode(operator);
		setOperator(operators.get(operator));
		setCompletionDate(cursor.getInt(completionDateIndex));
		setMemo(cursor.getString(memoIndex));
    }

	public Drawable getMarker(Resources resources) {
		return resources.getDrawable(isCompleted() ? R.drawable.pin_green : R.drawable.pin_red);
	}

	public static Drawable getStatusIcon(Resources resources, boolean isCompleted) {
		return resources.getDrawable(isCompleted ? R.drawable.statusicon_comp : R.drawable.statusicon_incomp);
	}

	public Drawable getStatusIcon(Resources resources) {
		return getStatusIcon(resources, isCompleted());
	}

	public String getDistanceDescription(Resources resources) {
		if(distance <= 0.01) {
			return resources.getString(R.string.station_distance_in_sight, getOperator().getName());
		} else if(distance < 1) {
			return resources.getString(R.string.station_distance_meter, getOperator().getName(), (int)(distance * 1000));
		} else if(distance < 10) {
			return resources.getString(R.string.station_distance_10km, getOperator().getName(), distance);
		} else {
			return resources.getString(R.string.station_distance_long, getOperator().getName(), (int)distance);
		}
	}

	public void calcDistance(GeoPoint centerPoint) {
		double y1 = getPoint().getLatitudeE6() * Math.PI / 180000000;
		double x1 = getPoint().getLongitudeE6() * Math.PI / 180000000;
		double y2 = centerPoint.getLatitudeE6() * Math.PI / 180000000;
		double x2 = centerPoint.getLongitudeE6() * Math.PI / 180000000;
		final double earth_r = 6378137;

		double deg = Math.sin(y1) * Math.sin(y2) + Math.cos(y1) * Math.cos(y2) * Math.cos(x2 - x1);
		distance = earth_r * (Math.atan(-deg / Math.sqrt(-deg * deg + 1)) + Math.PI / 2) / 1000;
	}

	private static Comparator<Station> distanceComparator = new Comparator<Station> () {
		@Override
		public int compare(Station o1, Station o2) {
			final double sign = o1.distance - o2.distance;
			if(sign > 0) {
				return 1;
			} else if(sign == 0) {
				return 0;
			} else {
				return -1;
			}
		}
	};
	public static Comparator<Station> getDistanceComparator() { return distanceComparator; }

	// calls from Database Worker Thread only
	private static int completionDateIndex = -1;
	private static int memoIndex;

	private void initializeIndexes(Cursor cursor) {
		completionDateIndex = cursor.getColumnIndex("comp_date");
		memoIndex = cursor.getColumnIndex("memo");
	}

	@Override
	public long getCellId() { return code; }

	public static final Parcelable.Creator<Station> CREATOR = new Creator<Station>() {
		@Override
		public Station[] newArray(int size) {
			return new Station[size];
		}

		@Override
		public Station createFromParcel(Parcel source) {
			return new Station(source);
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	public Station(Parcel source) {
		code = source.readInt();
		lat1E6 = source.readInt();
		lng1E6 = source.readInt();
		point = null;
		name = source.readString();
		yomi = source.readString();
		wiki = source.readString();
		pref = source.readInt();
		address = source.readString();
		operatorCode = source.readInt();
		int exists = source.readInt();
		operator = exists > 0 ? (Operator)source.readParcelable(Operator.class.getClassLoader()) : null;
		lines = source.createTypedArrayList(Line.CREATOR);
		completionDate = source.readInt();
		memo = source.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(code);
		dest.writeInt(lat1E6);
		dest.writeInt(lng1E6);
		dest.writeString(name);
		dest.writeString(yomi);
		dest.writeString(wiki != null ? wiki : "");
		dest.writeInt(pref);
		dest.writeString(address);
		dest.writeInt(operatorCode);
		if(operator != null) {
			dest.writeInt(1);
			dest.writeParcelable(operator, flags);
		} else {
			dest.writeInt(0);
		}
		dest.writeTypedList(lines);
		dest.writeInt(completionDate);
		dest.writeString(memo);
	}

	@Override
	public String getTitle(Resources resources) {
		return getTitle();
	}
	@Override
	public String getDescription(Resources resources) {
		return getDistanceDescription(resources);
	}
	
	//for v2
	public void setExpired() { code = 0; }
	public boolean isExpired() { return code == 0; }

}
