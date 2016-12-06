package com.wsf_lp.mapapp.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Operator implements Parcelable {
	private int code;
	private int operatorType;
	private String name;

	public int getCode() { return code; }
	public int getOperatorType() { return operatorType; }
	public String getName() { return name; }
	
	public Operator() {}

	//called from same package
	protected void setCode(int code) { this.code = code; }
	protected void setOperatorType(int operatorType) { this.operatorType = operatorType; }
	protected void setName(String name) { this.name = name; }

	public static final Parcelable.Creator<Operator> CREATOR = new Creator<Operator>() {
		@Override
		public Operator[] newArray(int size) {
			return new Operator[size];
		}
		
		@Override
		public Operator createFromParcel(Parcel source) {
			return new Operator(source);
		}
	};
	@Override
	public int describeContents() {
		return 0;
	}
	public Operator(Parcel source) {
		code = source.readInt();
		operatorType = source.readInt();
		name = source.readString();
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(code);
		dest.writeInt(operatorType);
		dest.writeString(name);
	}
}
