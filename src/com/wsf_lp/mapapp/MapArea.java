package com.wsf_lp.mapapp;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class MapArea implements Cloneable {
	private int centerLatitude;
	private int centerLongitude;
	private int latitudeSpan;
	private int longitudeSpan;
	
	public MapArea() {
	}
	
	public MapArea(MapView mapView) {
		set(mapView);
	}
	
	public int getCenterLatitude() { return centerLatitude; }
	public int getCenterLongitude() { return centerLongitude; }
	public int getLatitudeSpan() { return latitudeSpan; }
	public int getLongitudeSpan() { return longitudeSpan; }
	
	public int getMinLatitude() { return centerLatitude - latitudeSpan / 2 - 1; }
	public int getMaxLatitude() { return centerLatitude + latitudeSpan / 2 + 1; }
	public int getMinLongitude() { return centerLongitude - longitudeSpan / 2 - 1; }
	public int getMaxLongitude() { return centerLongitude + longitudeSpan / 2 + 1; }
	
	public void set(MapView mapView) {
		if(mapView != null) {
			centerLatitude = mapView.getMapCenter().getLatitudeE6();
			centerLongitude = mapView.getMapCenter().getLongitudeE6();
			latitudeSpan = mapView.getLatitudeSpan();
			longitudeSpan = mapView.getLongitudeSpan();
		} else {
			centerLatitude = 0;
			centerLongitude = 0;
			latitudeSpan = 0;
			longitudeSpan = 0;
		}
	}
	
	public GeoPoint getCenterPoint() {
		return new GeoPoint(centerLatitude, centerLongitude);
	}
	
	@Override
	public MapArea clone() {
		MapArea dst = new MapArea();
		dst.centerLatitude = centerLatitude;
		dst.centerLongitude = centerLongitude;
		dst.latitudeSpan = latitudeSpan;
		dst.longitudeSpan = longitudeSpan;
		return dst;
	}
	
	public boolean equals(MapArea mapArea) {
		return getCenterLongitude() == mapArea.getCenterLongitude()
			&& getCenterLatitude() == mapArea.getCenterLatitude()
			&& getLatitudeSpan() == mapArea.getLatitudeSpan()
			&& getLongitudeSpan() == mapArea.getLongitudeSpan();
	}

}
