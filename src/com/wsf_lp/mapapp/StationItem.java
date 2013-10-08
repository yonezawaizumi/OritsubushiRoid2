package com.wsf_lp.mapapp;

import java.util.Comparator;

import com.wsf_lp.mapapp.data.Station;

import android.graphics.drawable.Drawable;

public class StationItem {
	private Station station;
	private long latlng;
	private Drawable marker;

	public StationItem(Station station) {
		this.station = station;
		this.latlng = (long)-station.getLatitudeE6() * 0x100000000L + station.getLongitudeE6();
	}

	public Station getStation() { return station; }
	public void setStation(Station station) { this.station = station; }

	public Drawable getMarker() { return marker; }
	public void setMarker(Drawable marker) { this.marker = marker; }
	public long getLatLng() { return latlng; }

	@Override
	public boolean equals(Object dest) {
		return getStation() != null && dest instanceof StationItem && getStation().equals(((StationItem)dest).getStation());
	}

	private static Comparator<StationItem> overlayComparator = new Comparator<StationItem> () {
		@Override
		public int compare(StationItem o1, StationItem o2) {
			final long sign = o1.getLatLng() - o2.getLatLng();
			if(sign > 0) {
				return 1;
			} else if(sign == 0) {
				return 0;
			} else {
				return -1;
			}
		}
	};
	public static Comparator<StationItem> getOverlayComparator() { return overlayComparator; }
}
