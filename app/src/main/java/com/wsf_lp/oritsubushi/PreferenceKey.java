package com.wsf_lp.oritsubushi;

public class PreferenceKey {
	public static final String MAX_STATIONS = "number_of_icons";

	public static final String RECENT_COMPLETION_DATE = "VerboseActivity.recent_completion_date";

	public static final String MENU_ORDER = "ActivityChanger.order";

	public static final String SYNC_RECENT_DATE = "SyncActivity.recent_date";

	public static final String MAP_CAMERA_LAT = "MapFragment.CameraLatitude";
	public static final String MAP_CAMERA_LNG = "MapFragment.CameraLongitude";
	public static final String MAP_CAMERA_BEARING = "MapFragment.CameraBearing";
	public static final String MAP_CAMERA_TILT = "MapFragment.CameraTilt";
	public static final String MAP_CAMERA_ZOOM = "MapFragment.CameraZoom";

	public static final String RECENT_FRAGMENT_POSITION = "MainActivity.RecentFragmentPosition";

	//ついにパッケージ分離の意味がなくなった。死んでしまえAndroid。
	public static final String DATABASE_VERSION = "Database.version";
}
