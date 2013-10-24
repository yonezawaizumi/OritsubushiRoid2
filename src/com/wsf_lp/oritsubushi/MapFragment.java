package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.maps.GeoPoint;
import com.wsf_lp.android.GeocoderDialogUtil;
import com.wsf_lp.mapapp.MapArea;
import com.wsf_lp.mapapp.MapAreaV2;
import com.wsf_lp.mapapp.MapPointV2;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Station;


public class MapFragment extends DBAccessFragmentBase
	implements ListView.OnItemClickListener,
		RadioGroup.OnCheckedChangeListener,
		OnClickListener, OnFocusChangeListener, OnEditorActionListener,
		OnCameraChangeListener, OnMapClickListener, OnMarkerClickListener, OnInfoWindowClickListener {

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
	private long mWaitMilliSec;
	private View mControlsContainer;
	private EditText mSearchEdit;
	private RadioGroup mVisibilityTypeGroup;
	private int mVisibilityType;
	private RadioGroup mStyleGroup;
	private int mStyle;
	private ListView mListView;
	private ArrayList<Station> mStationList = new ArrayList<Station>();
	private HashMap<String, Station> mStationMap = new HashMap<String, Station>();
	private LinkedList<MapPointV2> mCenterPoints = new LinkedList<MapPointV2>();
	private CellAdapter mCellAdapter;
	private MapAreaV2 mMapArea;
	private long mNextUpdateTime;
	private WeakReference<Marker> mCurrentMarker;
	private boolean mIsActive;
	private boolean mIsInitialized;
	private Animation mFadeInAnimation;
	private Animation mFadeOutAnimation;
	//private View locationWrapper;
	private boolean mIsSearching;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWaitMilliSec = getResources().getInteger(R.integer.settings_map_update_wait_msec);
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

		mMapView = (MapView)contentView.findViewById(R.id.mapview_skel);

		mControlsContainer = contentView.findViewById(R.id.layout_controls_container);

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

		//locationWrapper = findViewById(R.id.location_wrapper);
		mIsSearching = false;

		mListView = (ListView)contentView.findViewById(R.id.map_list_view);
		mCellAdapter = new CellAdapter(mStationList, getActivity());
		mListView.setAdapter(mCellAdapter);
		mListView.setOnItemClickListener(this);

		return contentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Activity activity = getActivity();

		mMapView.onCreate(savedInstanceState);

		GoogleMap map = mMapView.getMap();
		map.setMyLocationEnabled(false);
		map.setOnCameraChangeListener(this);
		map.setOnMapClickListener(this);
		map.setOnMarkerClickListener(this);
		map.setOnInfoWindowClickListener(this);
		UiSettings settings = map.getUiSettings();
		settings.setAllGesturesEnabled(true);
		settings.setCompassEnabled(true);
		settings.setMyLocationButtonEnabled(true);
		settings.setZoomControlsEnabled(true);

		mFadeInAnimation = AnimationUtils.loadAnimation(activity, R.anim.map_controls_fade_in);
		mFadeOutAnimation = AnimationUtils.loadAnimation(activity, R.anim.map_controls_fade_out);
	}

	private boolean initialize() {
		if(isDatabaseEnabled() && mIsActive && !mIsInitialized) {
			mIsInitialized = true;
			Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.wrapper_fade_out);
			View container = getView();
			View wrapper = container.findViewById(R.id.wrapper);
			wrapper.startAnimation(animation);
			wrapper.setVisibility(View.GONE);
			wrapper = container.findViewById(R.id.loading_wrapper);
			wrapper.startAnimation(animation);
			wrapper.setVisibility(View.GONE);
			loadStations();
			return true;
		} else {
			return false;
		}
	}

	private void loadStations() {
		if(!mIsActive || !isDatabaseEnabled() || mMapView == null) {
			return;
		}
		long current = SystemClock.uptimeMillis();
		if(mMapArea != null && current < mNextUpdateTime) {
			return;
		}
		MapAreaV2 currentMapArea = new MapAreaV2(mMapView.getMap());
		if(mMapArea == null || !currentMapArea.equals(mMapArea)) {
			Log.d(this.getClass().getName(), "loadStations-2(update)");
			mMapArea = currentMapArea;
			mCenterPoints.addLast(mMapArea.getCenterPoint());
			mNextUpdateTime = current + mWaitMilliSec;
			//setLoading();
			//callDatabase(Database.MethodName.GET_STATIONS, mMapArea.clone());
		}
	}

	private void updateStations(SparseArray<Station> stations) {
		Marker currentMarker = mCurrentMarker != null ? mCurrentMarker.get() : null;
		int currentCode;
		if(currentMarker != null) {
			currentMarker.hideInfoWindow();
//			currentCode = currentMarker.
		}
/*		stations.clear();
		stations.ensureCapacity(items.size());
		for(int index = items.size() - 1; index >= 0; --index) {
			final Station station = items.valueAt(index).getStation();
			station.calcDistance(centerPoint);
			stations.add(station);
		}
		Collections.sort(stations, Station.getDistanceComparator());
		mapOverlayView.updateStations(stations);
		mapOverlayView.getStationsForList(stationList, centerPoints.poll());
		cellAdapter.notifyDataSetChanged();
		nextUpdateTime = SystemClock.uptimeMillis();
		loadStations();*/
	}

	@Override
	public void onStart() {
		super.onStart();
		mIsActive = true;
		mMapView.getMap().setMyLocationEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		mMapView.onResume();
		initialize();
	}

	@Override
	public void onPause() {
		super.onPause();
		mMapView.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		mIsActive = false;
		mMapView.getMap().setMyLocationEnabled(false);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mMapView.onSaveInstanceState(outState);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mMapView.onLowMemory();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mMapView.onDestroy();
		mMapView = null;
		mControlsContainer = null;
		mSearchEdit = null;
		mVisibilityTypeGroup = null;
		mStyleGroup = null;
		mListView = null;
		mCellAdapter = null;
		mFadeInAnimation = null;
		mFadeOutAnimation = null;
		mMapArea = null;
		mNextUpdateTime = 0;
		mCurrentMarker = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mIsInitialized = false;
	}

	@Override
	public void onStationUpdated(Station station) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		if(methodName.equals(Database.MethodName.GET_STATIONS)) {
			updateStations((SparseArray<Station>)result);
		}
	}

	@Override
	protected void onDatabaseUpdated(boolean isFirst) {
		// TODO 自動生成されたメソッド・スタブ
		if(isFirst) {
			initialize();
		}
	}

	@Override
	protected void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations) {
		// TODO updatedStationsを見る
		if(isEnabled) {
			initialize();
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		mCurrentMarker = null;
		// TODO StationFragment開く
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		mCurrentMarker = new WeakReference<Marker>(marker);
		return false;
	}

	@Override
	public void onMapClick(LatLng point) {
		if(mControlsContainer.isShown()) {
			mControlsContainer.startAnimation(mFadeOutAnimation);
			mControlsContainer.setVisibility(View.INVISIBLE);
		} else {
			mControlsContainer.startAnimation(mFadeInAnimation);
			mControlsContainer.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (!mIsSearching && (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
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
		v.requestFocusFromTouch();
	}

	private void doSearch() {
		//TODO:現時点では地点検索のみの実装
		//mIsSearching = true;
		/*new GeocoderDialogUtil(this, R.string.maybe, R.string.cancel)
			.setListener(this)
			.setLocation(searchEdit.getText().toString())
			.request();*/
	}
/*
	@Override
	public void onGeocoderAddressSelect(GeocoderDialogUtil dialogUtil, String title, int lat, int lng) {
		//mapView.getController().animateTo(new GeoPoint(lat, lng));
		//mIsSearching = false;
		//TODO:検索バー消す
	}

	@Override
	public void onGeocoderAddressNotFound(GeocoderDialogUtil dialogUtil) {
		Toast.makeText(this, getString(R.string.search_zero_results_format, searchEdit.getText().toString()), Toast.LENGTH_LONG).show();
		mIsSearching = false;
		//TODO:検索バー消す
	}

	@Override
	public void onGeocoderError(GeocoderDialogUtil dialogUtil, int reason) {
		int id;
		switch(reason) {
		case GeocoderDialogUtil.REQUEST_DENIED:
			id = R.string.search_request_denied;
			break;
		case GeocoderDialogUtil.NETWORK_ERROR:
			id = R.string.search_network_error;
			break;
		default:
			id = R.string.search_fatal_error;
			break;
		}
		Toast.makeText(this, id, Toast.LENGTH_LONG).show();
		mIsSearching = false;
		//TODO:検索バー消す
	}

	@Override
	public void onGeocoderCancel(GeocoderDialogUtil dialogUtil) {
		mIsSearching = false;
		//TODO:検索バー消す
	}*/
}