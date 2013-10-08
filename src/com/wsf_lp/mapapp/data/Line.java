package com.wsf_lp.mapapp.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Line implements Parcelable {
	private int code;
	private int type;
	private String name;
	private int operatorCode;

	public int getCode() { return code; }
	public int getType() { return type; }
	public String getName() { return name; }
	public int getOperatorCode() { return operatorCode; }

	public void setFromCursor(Cursor cursor) {
		code = cursor.getInt(0);
		name = cursor.getString(1);
		operatorCode = cursor.getInt(2);
		type = cursor.getInt(4);
	}

	public Line() {}
	
	public static final Parcelable.Creator<Line> CREATOR = new Creator<Line>() {
		@Override
		public Line[] newArray(int size) {
			return new Line[size];
		}
		
		@Override
		public Line createFromParcel(Parcel source) {
			return new Line(source);
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}
	public Line(Parcel source) {
		code = source.readInt();
		type = source.readInt();
		name = source.readString();
		operatorCode = source.readInt();
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(code);
		dest.writeInt(type);
		dest.writeString(name);
		dest.writeInt(operatorCode);
	}
}
