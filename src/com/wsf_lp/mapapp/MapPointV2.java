package com.wsf_lp.mapapp;

import com.google.android.gms.maps.model.LatLng;

public class MapPointV2 {

	public int latitude;
	public int longitude;

	public MapPointV2() {
		latitude = 0;
		longitude = 0;
	}
	public MapPointV2(MapPointV2 src) {
		latitude = src.latitude;
		longitude = src.longitude;
	}
	public MapPointV2(int latitude, int longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	public MapPointV2(LatLng latLng) {
		latitude = (int)(latLng.latitude * 1E6);
		longitude = (int)(latLng.longitude * 1E6);
	}
	public boolean equals(MapPointV2 dst) {
		return latitude == dst.latitude && longitude == dst.longitude;
	}
}
