package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.wsf_lp.android.GeocoderDialogUtil;
import com.wsf_lp.oritsubushi.R;
import com.wsf_lp.mapapp.MapArea;
import com.wsf_lp.mapapp.MapOverlayView;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
//import android.view.Window;
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
import android.widget.ZoomControls;

public class OritsubushiRoidActivity extends MapActivity
		implements MapOverlayView.Listener,
			ListView.OnItemClickListener,
			GeocoderDialogUtil.OnResultListener,
			DatabaseResultReceiver,
			DatabaseServiceConnector.Listener,
			OritsubushiBroadcastReceiver.UpdateListener,
			OritsubushiBroadcastReceiver.MapListener,
			RadioGroup.OnCheckedChangeListener {
	public static final int ID = R.id.main;
	private static final int[] visibilityTypeButtonIds = {
		R.id.mapbutton_all,
		R.id.mapbutton_comp,
		R.id.mapbutton_incomp
	};
	private static final int[] styleButtonIds = {
		R.id.mapbutton_map,
		R.id.mapbutton_satellite,
		R.id.mapbutton_list
	};
	public static class Style {
		public static final int MAP = 0;
		public static final int SATELLITE = 1;
		public static final int LIST = 2;
	};
	Handler handler = new Handler();
	private MapView mapView;
	private DatabaseServiceConnector connector;
	private DatabaseService databaseService;
	private long waitMilliSec;
	private MyLocationOverlay myLocationOverlay;
	private MapOverlayView mapOverlayView;
	private OritsubushiBroadcastReceiver broadcastReceiver;
	private Button currentLocationButton;
	private EditText searchEdit;
	private RadioGroup visibilityTypeGroup;
	private int visibilityType = Database.VisibilityType.ALL_STATIONS;
	private RadioGroup styleGroup;
	private int style = Style.MAP;
	private ListView listView;
	private ArrayList<Station> stationList = new ArrayList<Station>();
	private LinkedList<GeoPoint> centerPoints = new LinkedList<GeoPoint>();
	private CellAdapter cellAdapter;
	private int loading = 0;
	private ProgressBar loadingIcon;
	private ZoomControls zoomControls;
	private static boolean inLocationInitializing = true;
	private boolean isDatabaseActive;
	private boolean needsReloadStations;
	private boolean isActive;
	private ActivityChanger activityChanger;
	private Animation fadeInAnimation;
	private Animation fadeOutAnimation;
	//private View locationWrapper;
	private boolean inSearch;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityChanger = new ActivityChanger(this, ID);
        isDatabaseActive = false;
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.map_title);
        loadingIcon = (ProgressBar)findViewById(R.id.loading);
        loadingIcon.setVisibility(View.INVISIBLE);

        //初期化値を先に設定しておかないとデータベース生成時の初期化が完全に行われない
        PreferenceManager.setDefaultValues(this, R.xml.preference, true);

        broadcastReceiver = new OritsubushiBroadcastReceiver(this);
        broadcastReceiver.registerTo(this, OritsubushiNotificationIntent.getMapIntentFilter());
        connector = new DatabaseServiceConnector();
        connector.connect(this, this);


        waitMilliSec = getResources().getInteger(R.integer.settings_map_update_wait_msec);

		mapView = new MapView(this, getString(R.string.map_key)) {
			public void computeScroll() {
				super.computeScroll();
				loadStations();
			}
		};
		mapView.setEnabled(true);
		mapView.setClickable(true);
		mapView.setFocusable(true);
		mapView.setBuiltInZoomControls(false);

		final List<Overlay> overlays = mapView.getOverlays();

		myLocationOverlay = new MyLocationOverlay(this, mapView);
		myLocationOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						OritsubushiRoidActivity.this.onCheckLocation();
					}
				});
			}
		});
		overlays.add(myLocationOverlay);

		setContentView(R.layout.main);

		final View skelton = findViewById(R.id.mapview_skel);
		final ViewGroup main = (ViewGroup)skelton.getParent();
		final int index = main.indexOfChild(skelton);
		main.addView(mapView, index, skelton.getLayoutParams());
		main.removeView(skelton);

		currentLocationButton = (Button)findViewById(R.id.button_mylocation);
		currentLocationButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mapView.getController().animateTo(myLocationOverlay.getMyLocation());
			}
		});
		visibilityTypeGroup = (RadioGroup)findViewById(R.id.radio_map_visibility);
		visibilityTypeGroup.check(visibilityTypeButtonIds[visibilityType]);
		visibilityTypeGroup.setOnCheckedChangeListener(this);
		styleGroup = (RadioGroup)findViewById(R.id.radio_map_style);
		styleGroup.check(styleButtonIds[style]);
		styleGroup.setOnCheckedChangeListener(this);
		//loadingProgress = findViewById(R.id.progress_loading);

		final MapController controller = mapView.getController();
		zoomControls = (ZoomControls)findViewById(R.id.zoomControls);
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				controller.zoomIn();
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				controller.zoomOut();
			}
		});
		fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.map_controls_fade_in);
		fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.map_controls_fade_out);
		mapOverlayView = new MapOverlayView(mapView, findViewById(R.id.layout_controls_container), this, fadeInAnimation, fadeOutAnimation);

		overlays.add(mapOverlayView);
		findViewById(R.id.layout_controls_container).setVisibility(View.VISIBLE);

		SearchBarListener listener = new SearchBarListener(this);
		searchEdit = (EditText)findViewById(R.id.text_search);
		searchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		searchEdit.setOnClickListener(listener);
		searchEdit.setOnFocusChangeListener(listener);
		searchEdit.setOnEditorActionListener(listener);

		//locationWrapper = findViewById(R.id.location_wrapper);
		inSearch = false;

		listView = (ListView)findViewById(R.id.map_list_view);
		cellAdapter = new CellAdapter(stationList, this);
		listView.setAdapter(cellAdapter);
		listView.setOnItemClickListener(this);

		mapView.invalidate();
    }

	private void setLocationButtonsEnabled() {
		currentLocationButton.setEnabled(myLocationOverlay.getMyLocation() != null);
	}


	private boolean onInitialized() {
		if(isDatabaseActive) {
			final Animation animation = AnimationUtils.loadAnimation(this, R.anim.wrapper_fade_out);
			View wrapper = findViewById(R.id.wrapper);
			wrapper.startAnimation(animation);
			wrapper.setVisibility(View.GONE);
			wrapper = findViewById(R.id.wrapper_parts_container);
			wrapper.startAnimation(animation);
			wrapper.setVisibility(View.GONE);
			setLocationButtonsEnabled();
			loadStations();
			return true;
		} else {
			return false;
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        activityChanger.createMenu(menu);
        return isActive;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		activityChanger.prepareMenu(menu);
		return isActive;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        return activityChanger.onSelectActivityMenu(item.getItemId());
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		VerboseActivity.start(this, (Station)listView.getItemAtPosition(position));
	}

	private void reduceRegion(int id, Region region) {
		final ViewGroup view = (ViewGroup)findViewById(id);
		final Rect rect = new Rect();
		final int count = view.getChildCount();
		for(int index = 0; index < count; ++index) {
			View child = view.getChildAt(index);
			if(child.getVisibility() == View.VISIBLE) {
				child.getGlobalVisibleRect(rect);
				region.op(rect, Op.DIFFERENCE);
			}
		}
	}

	@Override
	public Region onCalcVisibleRegion() {
		final Rect baseRect = new Rect();
		mapView.getGlobalVisibleRect(baseRect);
		Region region = new Region(baseRect);
		if(mapOverlayView.isControlsVisible()) {
			reduceRegion(R.id.filter_controls, region);
			reduceRegion(R.id.visual_controls, region);
		}
		region.translate(-baseRect.left, -baseRect.top);
		return region;
	}

	@Override
	public void onBalloonClick(Station station) {
		VerboseActivity.start(this, station);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void reserveCheckLocation() {
		handler.postDelayed(new Runnable() {
			public void run() {
				onCheckLocation();
			}
		}, (long)getResources().getInteger(R.integer.settings_mapview_update_disabled_delay_sec) * 1000);
	}

	@Override
	public void onStart() {
		super.onStart();
		isActive = true;
		myLocationOverlay.enableMyLocation();
		if(inLocationInitializing) {
			reserveCheckLocation();
		}
		if(needsReloadStations) {
			loadStations();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		myLocationOverlay.disableMyLocation();
		isActive = false;
	}

	@Override
	public void onDestroy() {
		Log.d(this.getClass().getName(), "onDestroy");
		broadcastReceiver.unregisterFrom(this);
		connector.disconnect();
		isDatabaseActive = false;
		super.onDestroy();
	}

	private void setLoading() {
		if(++loading > 0) {
	        loadingIcon.setVisibility(View.VISIBLE);
		}
	}
	private void unsetLoading() {
		if(--loading <= 0) {
			loading = 0;
	        loadingIcon.setVisibility(View.INVISIBLE);
		}
	}

	private void onCheckLocation() {
		if(inLocationInitializing) {
			GeoPoint myLocation = myLocationOverlay.getMyLocation();
			if(myLocation != null) {
				inLocationInitializing = false;
				//mapView.getController().animateTo(myLocation);
			}
		}
		setLocationButtonsEnabled();
		if(inLocationInitializing) {
			reserveCheckLocation();
		}
	}

	MapArea mapArea = null;
	long nextUpdateTime = 0;
	private void loadStations() {
		if(!isActive || !isDatabaseActive) {
			return;
		}
		long current = SystemClock.uptimeMillis();
		if(!needsReloadStations && mapArea != null && current < nextUpdateTime) {
			return;
		}
		MapArea currentMapArea = new MapArea(mapView);
		if(needsReloadStations || mapArea == null || !currentMapArea.equals(mapArea)) {
			Log.d(this.getClass().getName(), "loadStations-2(update)");
			mapArea = currentMapArea;
			centerPoints.addLast(mapArea.getCenterPoint());
			nextUpdateTime = current + waitMilliSec;
			needsReloadStations = false;
			setLoading();
			databaseService.callDatabase(this, Database.MethodName.GET_STATIONS, mapArea.clone());
		}
	}
	private void onUpdateStations(final SparseArray<Station> stations) {
		mapOverlayView.updateStations(stations);
		mapOverlayView.getStationsForList(stationList, centerPoints.poll());
		cellAdapter.notifyDataSetChanged();
		nextUpdateTime = SystemClock.uptimeMillis();
		loadStations();
	}

	@Override
	public void onDatabaseConnected(DatabaseService databaseService) {
		this.databaseService = databaseService;
		mapOverlayView.setDatabaseService(databaseService);
		isDatabaseActive = databaseService.isEnabled();
		if(isDatabaseActive) {
			onInitialized();
		}
	}

	@Override
	public void onDatabaseDisconnected() {
		mapOverlayView.setDatabaseService(null);
		this.databaseService = null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onDatabaseResult(long sequence, String methodName, Object result) {
		if(methodName.equals(Database.MethodName.GET_STATIONS)) {
			onUpdateStations((SparseArray<Station>)result);
		}
		unsetLoading();
	}

	@Override
	public void onDatabaseUpdated(final Station station) {
		if(isDatabaseActive) {
			if(station != null) {
				boolean remove;
				int checkedRadioButtonId = visibilityTypeGroup.getCheckedRadioButtonId();
				if (checkedRadioButtonId == R.id.mapbutton_comp) {
					remove = !station.isCompleted();
				} else if (checkedRadioButtonId == R.id.mapbutton_incomp) {
					remove = station.isCompleted();
				} else {
					remove = false;
				}
				if(remove) {
					mapOverlayView.removeStation(station);
				} else {
					mapOverlayView.updateStation(station);
				}
				final int index = stationList.indexOf(station);
				if(index >= 0) {
					mapOverlayView.clearStationItems();
					needsReloadStations = true;
					loadStations();
				}
			} else {
				mapOverlayView.clearStationItems();
				needsReloadStations = true;
				loadStations();
			}
		} else {
			isDatabaseActive = true;
			onInitialized();
		}
	}
	@Override
	public void onMapStatusChanged() {
		needsReloadStations = true;
		if(isDatabaseActive) {
			loadStations();
		}
	}

	@Override
	public void onMapMoveTo(Station station) {
		mapView.getController().animateTo(station.getPoint());
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		int id = group.getId();
		if (id == R.id.radio_map_visibility) {
			if(databaseService != null) {
				for(int visibility = visibilityTypeButtonIds.length - 1; visibility >= 0; --visibility) {
					if(visibilityTypeButtonIds[visibility] == checkedId) {
						databaseService.callDatabase(this, Database.MethodName.SET_VISIBILITY_TYPE, getResources(), Integer.valueOf(visibility));
						break;
					}
				}
			}
		} else if (id == R.id.radio_map_style) {
			if (checkedId == R.id.mapbutton_map) {
				currentLocationButton.setVisibility(View.VISIBLE);
				searchEdit.setVisibility(View.VISIBLE);
				zoomControls.setVisibility(View.VISIBLE);
				mapView.setSatellite(false);
				if(mapView.getVisibility() != View.VISIBLE) {
					mapView.startAnimation(fadeInAnimation);
					mapView.setVisibility(View.VISIBLE);
				}
				if(listView.getVisibility() == View.VISIBLE) {
					listView.startAnimation(fadeOutAnimation);
					listView.setVisibility(View.INVISIBLE);
				}
			} else if (checkedId == R.id.mapbutton_satellite) {
				currentLocationButton.setVisibility(View.VISIBLE);
				searchEdit.setVisibility(View.VISIBLE);
				zoomControls.setVisibility(View.VISIBLE);
				mapView.setSatellite(true);
				if(mapView.getVisibility() != View.VISIBLE) {
					mapView.startAnimation(fadeInAnimation);
					mapView.setVisibility(View.VISIBLE);
				}
				if(listView.getVisibility() == View.VISIBLE) {
					listView.startAnimation(fadeOutAnimation);
					listView.setVisibility(View.INVISIBLE);
				}
			} else if (checkedId == R.id.mapbutton_list) {
				if(listView.getVisibility() != View.VISIBLE) {
					currentLocationButton.setVisibility(View.INVISIBLE);
					searchEdit.setVisibility(View.INVISIBLE);
					zoomControls.setVisibility(View.INVISIBLE);
					mapView.startAnimation(fadeOutAnimation);
					mapView.setVisibility(View.INVISIBLE);
					listView.startAnimation(fadeInAnimation);
					listView.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private static class SearchBarListener implements OnClickListener, OnFocusChangeListener, OnEditorActionListener {
		private WeakReference<OritsubushiRoidActivity> activity;
		public SearchBarListener(OritsubushiRoidActivity activity) {
			this.activity = new WeakReference<OritsubushiRoidActivity>(activity);
		}
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			final OritsubushiRoidActivity activity = this.activity.get();
			if (activity != null && !activity.inSearch && (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
				activity.mapView.requestFocusFromTouch();
				activity.doSearch();
			}
			return false;
		}

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			final OritsubushiRoidActivity activity = this.activity.get();
			if(activity == null) {
				return;
			}
			InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if(hasFocus) {
				imm.showSoftInput(v, 0);
			} else {
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				activity.mapOverlayView.onLostEditTextFocus();
			}
		}

		@Override
		public void onClick(View v) {
			v.requestFocusFromTouch();
		}

	}

	private void doSearch() {
		//TODO:現時点では地点検索のみの実装
		inSearch = true;
		new GeocoderDialogUtil(this, R.string.maybe, R.string.cancel)
			.setListener(this)
			.setLocation(searchEdit.getText().toString())
			.request();
	}

	@Override
	public void onGeocoderAddressSelect(GeocoderDialogUtil dialogUtil, String title, int lat, int lng) {
		mapView.getController().animateTo(new GeoPoint(lat, lng));
		inSearch = false;
		//TODO:検索バー消す
	}

	@Override
	public void onGeocoderAddressNotFound(GeocoderDialogUtil dialogUtil) {
		Toast.makeText(this, getString(R.string.search_zero_results_format, searchEdit.getText().toString()), Toast.LENGTH_LONG).show();
		inSearch = false;
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
		inSearch = false;
		//TODO:検索バー消す
	}

	@Override
	public void onGeocoderCancel(GeocoderDialogUtil dialogUtil) {
		inSearch = false;
		//TODO:検索バー消す
	}

}