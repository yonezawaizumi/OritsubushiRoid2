package com.wsf_lp.mapapp;

import android.content.res.Resources;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wsf_lp.mapapp.data.Station;

public class MarkerOptionsUtil {

	public static MarkerOptions createMarker(Resources resources, Station station) {
		MarkerOptions opts = new MarkerOptions();
		opts.title(station.getTitle(resources));
		opts.snippet(station.getSubtitle());
		opts.position(station.getLatLng());
		opts.icon(BitmapDescriptorFactory.fromResource(station.getPinId()));
		opts.anchor(0.5f, 1.0f);
		return opts;
	}
}
