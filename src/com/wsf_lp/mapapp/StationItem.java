package com.wsf_lp.mapapp;

import java.util.Comparator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wsf_lp.mapapp.data.Station;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class StationItem {
	private Station station;
	private long latlng;
	//private Drawable marker;
	private Marker marker;

	public StationItem(Station station) {
		this.station = station;
		this.latlng = (long)-station.getLatitudeE6() * 0x100000000L + station.getLongitudeE6();
	}

	public Station getStation() { return station; }
	public void setStation(Station station) { this.station = station; }

	public long getLatLng() { return latlng; }

	public Marker getMarker() { return marker; }
	public Marker createMarker(Resources resources, GoogleMap map) {
		MarkerOptions opts = new MarkerOptions();
		opts.title(station.getTitle(resources));
		opts.snippet(station.getSubtitle());
		opts.position(station.getLatLng());
		opts.icon(BitmapDescriptorFactory.fromResource(station.getPinId()));
		opts.anchor(0.5f, 1.0f);
		marker = map.addMarker(opts);
		return marker;
	}
	public void removeMarker() {
		if(marker != null) {
			marker.remove();
			marker = null;
		}
	}

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
