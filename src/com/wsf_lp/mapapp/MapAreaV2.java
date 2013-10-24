package com.wsf_lp.mapapp;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;


public class MapAreaV2 implements Cloneable {
	private int centerLatitude;
	private int centerLongitude;
	private int latitudeSpan;
	private int longitudeSpan;

	private int minLatitude;
	private int maxLatitude;
	private int minLongitude;
	private int maxLongitude;

	public MapAreaV2() {
	}

	public MapAreaV2(GoogleMap map) {
		set(map);
	}

	public int getCenterLatitude() { return centerLatitude; }
	public int getCenterLongitude() { return centerLongitude; }
	public int getLatitudeSpan() { return latitudeSpan; }
	public int getLongitudeSpan() { return longitudeSpan; }

	public int getMinLatitude() { return minLatitude; }
	public int getMaxLatitude() { return maxLatitude; }
	public int getMinLongitude() { return minLongitude; }
	public int getMaxLongitude() { return maxLongitude; }

	public void set(GoogleMap map) {
		if(map != null) {
			LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
			LatLng center = bounds.getCenter();
			centerLatitude = (int)(center.latitude * 1E6);
			centerLongitude = (int)(center.longitude * 1E6);
			minLatitude = (int)(bounds.southwest.latitude * 1E6);
			minLongitude = (int)(bounds.southwest.longitude * 1E6);
			maxLatitude = (int)(bounds.northeast.latitude * 1E6);
			maxLongitude = (int)(bounds.northeast.longitude * 1E6);
			latitudeSpan = maxLatitude - minLatitude + 1;
			longitudeSpan = maxLongitude - minLongitude + 1;
		} else {
			centerLatitude = 0;
			centerLongitude = 0;
			latitudeSpan = 0;
			longitudeSpan = 0;
			minLatitude = 0;
			maxLatitude = 0;
			minLongitude = 0;
			maxLongitude = 0;
		}
	}

	public MapPointV2 getCenterPoint() {
		return new MapPointV2(centerLatitude, centerLongitude);
	}

	@Override
	public MapAreaV2 clone() {
		MapAreaV2 dst = new MapAreaV2();
		dst.centerLatitude = centerLatitude;
		dst.centerLongitude = centerLongitude;
		dst.latitudeSpan = latitudeSpan;
		dst.longitudeSpan = longitudeSpan;
		dst.minLatitude = minLatitude;
		dst.maxLatitude = maxLatitude;
		dst.minLongitude = minLongitude;
		dst.maxLongitude = maxLongitude;
		return dst;
	}

	public boolean equals(MapAreaV2 mapArea) {
		return minLongitude == mapArea.minLongitude
			&& minLatitude == mapArea.minLatitude
			&& maxLatitude == mapArea.maxLatitude
			&& maxLongitude == mapArea.maxLongitude;
	}

}
