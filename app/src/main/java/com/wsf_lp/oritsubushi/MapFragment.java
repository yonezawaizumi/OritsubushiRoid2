package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.preference.PreferenceManager;
import android.util.Log;
import androidx.collection.SparseArrayCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.wsf_lp.android.Geocoder;
import com.wsf_lp.mapapp.MapAreaV2;
import com.wsf_lp.mapapp.StationItem;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;


public class MapFragment extends DBAccessFragmentBase
	implements ListView.OnItemClickListener,
		RadioGroup.OnCheckedChangeListener,
		OnClickListener, OnFocusChangeListener, OnEditorActionListener,
		OnCameraIdleListener, OnMapClickListener, OnMarkerClickListener, OnInfoWindowClickListener, InfoWindowAdapter,
		Geocoder.OnResultListener,
		OnMapReadyCallback,
		MainActivity.OnAcceptMyLocationListener,
		OritsubushiBroadcastReceiver.MapListener {

	private static final int[] VISIBILITY_BUTTON_IDS = {
		R.id.mapbutton_all,
		R.id.mapbutton_comp,
		R.id.mapbutton_incomp
	};
	private static final int[] STYLE_BUTTON_IDS = {
		R.id.mapbutton_map,
		R.id.mapbutton_satellite,
		R.id.mapbutton_hybrid,
		R.id.mapbutton_list
	};

    public static final class Style {
		public static final int MAP = 0;
		public static final int SATELLITE = 1;
		public static final int HYBRID = 2;
		public static final int LIST = 3;
	};

	public static final String STATE_VISIBILITY_TYPE = "VisibilityType";
	public static final String STATE_STYLE = "Style";

	private MapView mMapView;
	private GoogleMap mMap;
	private View mPopupWindow;
	private long mWaitMilliSec;
	private int mInfoWindowAnimationDuration;
	private View mVisualControlsContainer;
	//private View mFilterControls;
	private EditText mSearchEdit;
	private RadioGroup mVisibilityTypeGroup;
	private int mVisibilityType;
	private RadioGroup mStyleGroup;
	private int mStyle;
	private ListView mListView;
	private ArrayList<Station> mStationList = new ArrayList<Station>();
	private SparseArrayCompat<StationItem> mStationItems = new SparseArrayCompat<StationItem>();
	private LinkedList<LatLng> mCenterPoints = new LinkedList<LatLng>();
	private CellAdapter mCellAdapter;
	private MapAreaV2 mMapArea;
	private long mNextUpdateTime;
	private int mPopupStationCode;
	private long mPopupCallSequence = Long.MAX_VALUE;
	private boolean mGPSIsEnabled;
	private boolean mIsInitialized;
	private int mNumFadeInAnimation;
	private int mNumFadeOutAnimation;
	private View mLocationWrapper;
	private Geocoder mGeocoder;

	private boolean isAnimating() { return mNumFadeInAnimation > 0 || mNumFadeOutAnimation > 0; }

	@Override
	protected IntentFilter getIntentFilter() {
		return OritsubushiNotificationIntent.getMapIntentFilter();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Resources resources = getResources();
		mWaitMilliSec = resources.getInteger(R.integer.settings_map_update_wait_msec);
		mInfoWindowAnimationDuration = resources.getInteger(R.integer.settings_map_info_window_animation_duration);
		mIsInitialized = false;
		mPopupStationCode = 0;
		if(savedInstanceState != null) {
			mVisibilityType = savedInstanceState.getInt(STATE_VISIBILITY_TYPE);
			mStyle = savedInstanceState.getInt(STATE_STYLE);
		} else {
			mVisibilityType = Database.VisibilityType.ALL_STATIONS;
			mStyle = Style.MAP;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View contentView = inflater.inflate(R.layout.map, container, false);
		mPopupWindow = inflater.inflate(R.layout.map_info_window, null);

		mMapView = (MapView)contentView.findViewById(R.id.mapview);

		mVisualControlsContainer = contentView.findViewById(R.id.visual_controls);
		//mFilterControls = contentView.findViewById(R.id.filter_controls);

		mVisibilityTypeGroup = (RadioGroup)contentView.findViewById(R.id.radio_map_visibility);
		mVisibilityTypeGroup.setOnCheckedChangeListener(this);
		mVisibilityTypeGroup.check(VISIBILITY_BUTTON_IDS[mVisibilityType]);
		mStyleGroup = (RadioGroup)contentView.findViewById(R.id.radio_map_style);
		mStyleGroup.setOnCheckedChangeListener(this);
		mStyleGroup.check(STYLE_BUTTON_IDS[mStyle]);

		mSearchEdit = (EditText)contentView.findViewById(R.id.text_search);
		mSearchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		mSearchEdit.setOnClickListener(this);
		mSearchEdit.setOnFocusChangeListener(this);
		mSearchEdit.setOnEditorActionListener(this);

		mLocationWrapper = contentView.findViewById(R.id.location_wrapper);

		mListView = (ListView)contentView.findViewById(R.id.map_list_view);
		mCellAdapter = new CellAdapter(mStationList, getActivity());
		mListView.setAdapter(mCellAdapter);
		mListView.setOnItemClickListener(this);

		return contentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		MainActivity activity = (MainActivity)getActivity();
		View container = getView();

        mGPSIsEnabled = activity.getMapStatus() != MainActivity.MAP_IS_DISABLED;

		container.findViewById(R.id.map_disabled).setVisibility(mGPSIsEnabled ? View.GONE : View.VISIBLE);

		View wrapper = container.findViewById(R.id.wrapper);
		View loadingWrapper = container.findViewById(R.id.loading_wrapper);

		if(mGPSIsEnabled) {
			mMapView.onCreate(savedInstanceState);
            mMapView.onResume();
            mMapView.getMapAsync(this);

			if(mIsInitialized) {
				//TODO: 検索モード時の処理
				wrapper.setVisibility(View.GONE);
				loadingWrapper.setVisibility(View.GONE);
			}
			onStyleCheckedChanged(mStyle, true);
		} else {
			mMapView.setVisibility(View.GONE);
			wrapper.setVisibility(View.GONE);
			loadingWrapper.setVisibility(View.GONE);
			mVisualControlsContainer.setVisibility(View.GONE);
			mLocationWrapper.setVisibility(View.GONE);
		}
	}

	public void onAcceptMyLocation() {
		try {
			mMap.setMyLocationEnabled(true);
		} catch (SecurityException e) {
			;
		}
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
        googleMap.setOnCameraIdleListener(this);
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setInfoWindowAdapter(this);
        UiSettings settings = googleMap.getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setCompassEnabled(true);
        settings.setMyLocationButtonEnabled(true);
        settings.setZoomControlsEnabled(true);
        onStyleCheckedChanged(mStyle, true);
        startMap(googleMap);
	}

    private void startMap(GoogleMap map) {
        if (mMap != map) {
            mMap = map;
			((MainActivity)getActivity()).setOnAcceptMyLocationListener(this);
            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final float INVALID_LAT = 100;
            float lat = preference.getFloat(PreferenceKey.MAP_CAMERA_LAT, INVALID_LAT);
            float lng;
            if(lat == INVALID_LAT) {
                lat = Float.parseFloat(getString(R.string.heso_latitude));
                lng = Float.parseFloat(getString(R.string.heso_longitude));
            } else {
                lng = preference.getFloat(PreferenceKey.MAP_CAMERA_LNG, 0);
            }
            CameraPosition position = new CameraPosition(
                    new LatLng(lat, lng),
                    preference.getFloat(PreferenceKey.MAP_CAMERA_ZOOM, (mMap.getMinZoomLevel() + mMap.getMaxZoomLevel()) / 2),
                    preference.getFloat(PreferenceKey.MAP_CAMERA_TILT, 0),
                    preference.getFloat(PreferenceKey.MAP_CAMERA_BEARING, 0)
            );
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        } else {
			try {
				mMap.setMyLocationEnabled(true);
			} catch (SecurityException e) {
				;
			}
		}
    }

	@Override
	public void onStart() {
		super.onStart();
		if(mGPSIsEnabled && mMap != null) {
            startMap(mMap);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if(mGPSIsEnabled) {
			mMapView.onResume();
			initialize();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if(mGPSIsEnabled) {
			mMapView.onPause();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if(mGPSIsEnabled && mMap != null) {
			if (((MainActivity)getActivity()).getMapStatus() == MainActivity.MY_LOCATION_IS_ENABLED) {
				try {
					mMap.setMyLocationEnabled(false);
				} catch (SecurityException e) {
					;
				}
			}
			CameraPosition position = mMap.getCameraPosition();
			PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putFloat(PreferenceKey.MAP_CAMERA_LAT, (float)position.target.latitude)
				.putFloat(PreferenceKey.MAP_CAMERA_LNG, (float)position.target.longitude)
				.putFloat(PreferenceKey.MAP_CAMERA_ZOOM, (float)position.zoom)
				.putFloat(PreferenceKey.MAP_CAMERA_TILT, (float)position.tilt)
				.putFloat(PreferenceKey.MAP_CAMERA_BEARING, (float)position.bearing)
				.apply();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mGPSIsEnabled && mMapView != null) {
			mMapView.onSaveInstanceState(outState);
			outState.putInt(STATE_VISIBILITY_TYPE, mVisibilityType);
			outState.putInt(STATE_STYLE, mStyle);
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if(mGPSIsEnabled) {
			mMapView.onLowMemory();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if(mGPSIsEnabled) {
			mMapView.setVisibility(View.GONE);
			mMapView.onDestroy();
		}
		mMapView = null;
		mVisualControlsContainer = null;
		//mFilterControls = null;
		mLocationWrapper = null;
		mSearchEdit = null;
		mVisibilityTypeGroup = null;
		mStyleGroup = null;
		mListView = null;
		mCellAdapter = null;
		mMapArea = null;
		mNextUpdateTime = 0;
		mStationList.clear();
		mStationItems.clear();
		mCenterPoints.clear();
		mGeocoder = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onStationUpdated(Station station) {
		if(mMapView == null || mMap == null) {
			return;
		}
		boolean remove;
		int checkedRadioButtonId = mVisibilityTypeGroup.getCheckedRadioButtonId();
		if (checkedRadioButtonId == R.id.mapbutton_comp) {
			remove = !station.isCompleted();
		} else if (checkedRadioButtonId == R.id.mapbutton_incomp) {
			remove = station.isCompleted();
		} else {
			remove = false;
		}
		int code = station.getCode();
		int index = mStationItems.indexOfKey(code);
		if(index >= 0) {
			StationItem item = mStationItems.valueAt(index);
			if(remove) {
				if(mPopupStationCode == code) {
					mPopupStationCode = 0;
					mStationItems.valueAt(index).getMarker().hideInfoWindow();
				}
				item.removeMarker();
				mStationItems.removeAt(index);
			} else {
				if(mPopupStationCode == code) {
					mStationItems.valueAt(index).getMarker().hideInfoWindow();
				}
				item.removeMarker();
				item.setStation(station);
				Marker marker = item.createMarker(getResources(), mMap);
				if(mPopupStationCode == code) {
					marker.showInfoWindow();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		if(methodName.equals(Database.MethodName.GET_STATIONS)) {
			updateStations((SparseArrayCompat<Station>)result);
		} else if(methodName.equals(Database.MethodName.LOAD_LINES)) {
			Station station = (Station)result;
			int code = station.getCode();
			StationItem item = mStationItems.get(code);
			if(item != null) {
				item.setStation(station);
				if(mPopupStationCode == code) {
					popupInfoWindow(sequence == mPopupCallSequence);
				}
			}
		}
	}

	@Override
	protected void onDatabaseUpdated(boolean isFirst) {
		if(!mGPSIsEnabled) {
			return;
		} else if(isFirst) {
			initialize();
			initializeStationVisibility();
		} else {
			clearStations();
			if(mMapView != null) {
				loadStations(true);
			}
		}
	}

	@Override
	protected void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations) {
		// TODO updatedStationsを見る
		if(isEnabled) {
			initialize();
			initializeStationVisibility();
		}
	}

	private void onVisibilityCheckedChanged(int visibilityType, boolean forceInitialize) {
		if(mVisibilityType != visibilityType || forceInitialize) {
			mVisibilityType = visibilityType;
			callDatabase(Database.MethodName.SET_VISIBILITY_TYPE, getResources(), visibilityType);
		}
	}

	@Override
	public void onMapStatusChanged() {
		loadStations(true);
	}

	@Override
	public void onMapMoveTo(Station station) {
		if(mGPSIsEnabled && mMap != null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLng(station.getLatLng()), mInfoWindowAnimationDuration, null);
		}
	}

	//直接ブロードキャストしないで外部からこれ呼んでください
	public static void moveTo(Context context, Station station) {
		context.sendBroadcast(new OritsubushiNotificationIntent().setMapMoveTo(station));
		LatLng latLng = station.getLatLng();
		PreferenceManager.getDefaultSharedPreferences(context).edit()
			.putFloat(PreferenceKey.MAP_CAMERA_LAT, (float)latLng.latitude)
			.putFloat(PreferenceKey.MAP_CAMERA_LNG, (float)latLng.longitude)
			.apply();
	}

	private void onStyleCheckedChanged(int style, boolean forceInitialize) {
		if(mStyle == style && !forceInitialize) {
			return;
		}
		mStyle = style;
		if(style == Style.LIST) {
			if(mListView.getVisibility() != View.VISIBLE) {
				if(mLocationWrapper.getVisibility() == View.VISIBLE) {
					mLocationWrapper.setVisibility(View.INVISIBLE);
					if(!forceInitialize) {
						mLocationWrapper.startAnimation(createFadeOutAnimation());
					}
				}
				if(mVisualControlsContainer.getVisibility() == View.INVISIBLE) {
					mVisualControlsContainer.setVisibility(View.VISIBLE);
					if(!forceInitialize) {
						mVisualControlsContainer.startAnimation(createFadeInAnimation());
					}
				}
				mMapView.setVisibility(View.INVISIBLE);
				mListView.setVisibility(View.VISIBLE);
				if(!forceInitialize) {
					mMapView.startAnimation(createFadeOutAnimation());
					mListView.startAnimation(createFadeInAnimation());
				}
			}
		} else {
			if(mListView.getVisibility() == View.VISIBLE) {
				mLocationWrapper.setVisibility(View.VISIBLE);
				if(!forceInitialize) {
					mLocationWrapper.startAnimation(createFadeInAnimation());
				}
				mMapView.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.INVISIBLE);
				if(!forceInitialize) {
					mMapView.startAnimation(createFadeInAnimation());
					mListView.startAnimation(createFadeOutAnimation());
				}
			}
            if (mMap != null) {
                switch (style) {
                    case Style.MAP:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                    case Style.SATELLITE:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case Style.HYBRID:
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                }
            }
		}
	}

	private void initializeStationVisibility() {
		onVisibilityCheckedChanged(mVisibilityType, true);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		int groupId = group.getId();
		if (groupId == R.id.radio_map_visibility) {
			for(int visibility = VISIBILITY_BUTTON_IDS.length - 1; visibility >= 0; --visibility) {
				if(VISIBILITY_BUTTON_IDS[visibility] == checkedId) {
					onVisibilityCheckedChanged(visibility, false);
					break;
				}
			}
		} else if (groupId == R.id.radio_map_style) {
			for(int style = STYLE_BUTTON_IDS.length - 1; style >= 0; --style) {
				if(STYLE_BUTTON_IDS[style] == checkedId) {
					onStyleCheckedChanged(style, false);
					break;
				}
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if(isAnimating()) {
			return;
		}
		StationFragment.show(this, (Station)mListView.getItemAtPosition(position), false);
	}

	@Override
	public View getInfoContents(Marker marker) {
		return null;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		if(mPopupStationCode != 0) {
			StationItem item = mStationItems.get(mPopupStationCode);
			if(item != null) {
				Station station = item.getStation();
				TextView textView = (TextView)mPopupWindow.findViewById(R.id.title_view);
				textView.setText(station.getTitle());
				textView = (TextView)mPopupWindow.findViewById(R.id.operator_view);
				textView.setText(station.getOperator().getName());
				textView = (TextView)mPopupWindow.findViewById(R.id.lines_view);
				textView.setText(station.getLineNames());
				return mPopupWindow;
			}
		}
		return null;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		if(isAnimating()) {
			return;
		}
		StationItem item = mStationItems.get(mPopupStationCode);
		if(item != null && item.getMarker().equals(marker)) {
			StationFragment.show(this, item.getStation(), true);
		}
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		for(int index = mStationItems.size() - 1; index >= 0; --index) {
			StationItem item = mStationItems.valueAt(index);
			if(marker.equals(item.getMarker())) {
				mPopupStationCode = item.getStation().getCode();
				popupInfoWindow(true);
			}
		}
		return true;
	}

	@Override
	public void onMapClick(LatLng point) {
		if(mPopupStationCode != 0) {
			StationItem item = mStationItems.get(mPopupStationCode);
			if(item != null) {
				item.getMarker().hideInfoWindow();
			}
			mPopupStationCode = 0;
			return;
		}
		if(isAnimating()) {
			return;
		}
		boolean isShown = mLocationWrapper.getVisibility() == View.VISIBLE;
		mLocationWrapper.setVisibility(isShown ? View.INVISIBLE : View.VISIBLE);
		mLocationWrapper.startAnimation(isShown ? createFadeOutAnimation() : createFadeInAnimation());
		isShown = mVisualControlsContainer.getVisibility() == View.VISIBLE;
		mVisualControlsContainer.setVisibility(isShown ? View.INVISIBLE : View.VISIBLE);
		mVisualControlsContainer.startAnimation(isShown ? createFadeOutAnimation() : createFadeInAnimation());
	}

    @Override
    public void onCameraIdle() {
        loadStations(false);
    }

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (mGeocoder == null && (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
			mMapView.requestFocusFromTouch();
			doSearch();
		}
		return false;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		if(hasFocus) {
			imm.showSoftInput(v, 0);
		} else {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			//activity.mapOverlayView.onLostEditTextFocus();
		}
	}

	@Override
	public void onClick(View v) {
		if(isAnimating()) {
			return;
		}
		v.requestFocusFromTouch();
	}

	private boolean initialize() {
		if(isDatabaseEnabled() && mMapView != null && !mIsInitialized && mGPSIsEnabled) {
			mIsInitialized = true;
			View container = getView();
			View wrapper = container.findViewById(R.id.wrapper);
			Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.wrapper_fade_out);
			wrapper.startAnimation(animation);
			wrapper.setVisibility(View.GONE);
			wrapper = container.findViewById(R.id.loading_wrapper);
			animation = AnimationUtils.loadAnimation(getActivity(), R.anim.wrapper_fade_out);
			wrapper.startAnimation(animation);
			wrapper.setVisibility(View.GONE);
			loadStations(true);
			return true;
		} else {
			return false;
		}
	}

	private void loadStations(boolean forceLoad) {
		if(!mGPSIsEnabled || !isDatabaseEnabled() || mMap == null) {
			return;
		}
		long current = SystemClock.uptimeMillis();
		if(mMapArea != null && current < mNextUpdateTime) {
			return;
		}
		MapAreaV2 currentMapArea = new MapAreaV2(mMap);
		if(forceLoad || mMapArea == null || !currentMapArea.equals(mMapArea)) {
			Log.d(this.getClass().getName(), "loadStations-2(update)");
			mMapArea = currentMapArea;
			mCenterPoints.addLast(mMapArea.getCenterPoint());
			mNextUpdateTime = current + mWaitMilliSec;
			//setLoading();
			callDatabase(Database.MethodName.GET_STATIONS, mMapArea.clone());
		}
	}

	private void resetStations() {
		for(int index = mStationItems.size() - 1; index >= 0; --index) {
			StationItem item = mStationItems.valueAt(index);
			if(item.getStation().getCode() == mPopupStationCode) {
				item.getMarker().hideInfoWindow();
				mPopupStationCode = 0;
			}
			item.removeMarker();
		}
	}

	private void clearStations() {
		resetStations();
		mStationItems.clear();
	}

	private void updateStations(SparseArrayCompat<Station> newStations) {
		if(!mGPSIsEnabled || mMapView == null) {
			return;
		}
		//マーカー・リストの更新
		Resources resources = getResources();
		LatLng centerPoint = mCenterPoints.poll();
		final int size = newStations.size();
		mStationList.clear();
		mStationList.ensureCapacity(size);
		SparseArrayCompat<StationItem> newStationItems = new SparseArrayCompat<StationItem>(size);
		for(int index = size - 1; index >= 0; --index) {
			Station station = newStations.valueAt(index);
			station.calcDistance(centerPoint);
			mStationList.add(station);
			int code = station.getCode();
			int recentIndex = mStationItems.indexOfKey(code);
			if(recentIndex >= 0) {
				StationItem item = mStationItems.valueAt(recentIndex);
				newStationItems.append(code, item);
				mStationItems.removeAt(recentIndex);
			} else {
				StationItem item = new StationItem(station);
				newStationItems.append(code, item);
			}
		}
		resetStations();
		for(int index = newStationItems.size() - 1; index >= 0; --index) {
			StationItem item = newStationItems.valueAt(index);
			if(mMap != null && item.getMarker() == null) {
				item.createMarker(resources, mMap);
			}
		}
		mStationItems = newStationItems;
		Collections.sort(mStationList, Station.getDistanceComparator());
		mCellAdapter.notifyDataSetChanged();
		mNextUpdateTime = SystemClock.uptimeMillis();
		popupInfoWindow(false);
		loadStations(false);
	}

	private void popupInfoWindow(boolean animation) {
		if(mPopupStationCode != 0 && mMap != null) {
			StationItem item = mStationItems.get(mPopupStationCode);
			if(item != null) {
				if(item.getStation().isReadyToCreateSubtitle()) {
					if(animation) {
						mMap.animateCamera(CameraUpdateFactory.newLatLng(item.getStation().getLatLng()), mInfoWindowAnimationDuration, null);
					}
					item.getMarker().showInfoWindow();
				} else {
					long sequence = callDatabase(Database.MethodName.LOAD_LINES, item.getStation());
					if(animation) {
						mPopupCallSequence = sequence;
					}
				}
			} else {
				mPopupStationCode = 0;
			}
		}
	}

	private void doSearch() {
		//TODO:現時点では地点検索のみの実装
		mGeocoder = new Geocoder(this, R.string.maybe);
		mGeocoder.request(mSearchEdit.getText().toString(), getContext());
	}

	@Override
	public void onGeocoderAddressSelect(String title, LatLng latLng) {
		if(mMap != null) {
			mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng), mInfoWindowAnimationDuration, null);
		}
		mGeocoder = null;
		//TODO:検索バー消す
	}

	@Override
	public void onGeocoderAddressNotFound() {
		Toast.makeText(getActivity(), getString(R.string.search_zero_results_format, mSearchEdit.getText().toString()), Toast.LENGTH_LONG).show();
		mGeocoder = null;
	}

	@Override
	public void onGeocoderError(int reason) {
		int id;
		switch(reason) {
		case Geocoder.REQUEST_DENIED:
			id = R.string.search_request_denied;
			break;
		case Geocoder.NETWORK_ERROR:
			id = R.string.search_network_error;
			break;
		default:
			id = R.string.search_fatal_error;
			break;
		}
		Toast.makeText(getActivity(), id, Toast.LENGTH_LONG).show();
		mGeocoder = null;
	}

	@Override
	public void onGeocoderCancel() {
		mGeocoder = null;
	}

	private Animation createFadeInAnimation() {
		Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.map_controls_fade_in);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) { --mNumFadeInAnimation; }
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) { ++mNumFadeInAnimation; }
		});
		return animation;
	}
	private Animation createFadeOutAnimation() {
		Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.map_controls_fade_out);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) { --mNumFadeOutAnimation; }
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) { ++mNumFadeOutAnimation; }
		});
		return animation;
	}

}