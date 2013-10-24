package com.wsf_lp.mapapp;

import java.util.ArrayList;
import java.util.Collections;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
//import android.os.Debug;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.Station;

public class MapOverlayView extends Overlay
		implements GestureDetector.OnDoubleTapListener,
		GestureDetector.OnGestureListener,
		MapBalloonView.MapBalloonViewListener,
		DatabaseResultReceiver {
	//シングルタップ・ダブルタップのキャプチャ
	private GestureDetector gesture = new GestureDetector(this);
	//コントロールボタン群の制御
	private MapView mapView;
	private View controlsContainer;
	private boolean searchTextHasLostFocus;
	private Animation fadeInAnimation;
	private Animation fadeOutAnimation;
	//ItemizedOverlayの代替
	private SparseArray<StationItem> items = new SparseArray<StationItem>();
	private ArrayList<StationItem> drawItems;
	//private StationItem focusedItem = null;
	//private int focusedItemIndex = -1;
	//バルーン
	public interface Listener {
		public Region onCalcVisibleRegion();
		public void onBalloonClick(Station station);
	}
	private MapBalloonView balloon;
	private Listener listener;

	private DatabaseService databaseService;

	public MapOverlayView(
			MapView mapView,
			View controlsContainer,
			Listener listener,
			Animation fadeInAnimation,
			Animation fadeOutAnimation
		) {
		this.mapView = mapView;
		this.controlsContainer = controlsContainer;
		this.fadeInAnimation = fadeInAnimation;
		this.fadeOutAnimation = fadeOutAnimation;
		this.balloon = new MapBalloonView(mapView);
		this.balloon.setListener(this);
		this.listener = listener;
}

	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	//シングルタップ・ダブルタップの判定
	@Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		gesture.onTouchEvent(e);
		return super.onTouchEvent(e, mapView);
	}

	public void onLongPress(MotionEvent e) {

	}

	public boolean onDown(MotionEvent e) {
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	public void onShowPress(MotionEvent e) {

	}

	public boolean onSingleTapUp(MotionEvent e) {
		/*cancelTimerTask();
		timerTask = new TimerTask() {
			@Override
			public void run() {
				controlsContainer.setVisibility(controlsContainer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
			}
		};
		timer.schedule(timerTask, 250);
		*/
		return false;
	}

	//ダブルタップでズームアップ
	public boolean onDoubleTap(MotionEvent e) {
		mapView.getController().zoomInFixing((int)e.getX(), (int)e.getY());
		return false;
	}

	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	//検索ボタンタップなどの理由で呼び出せ
	public void onLostEditTextFocus() {
		searchTextHasLostFocus = true;
	}

	//シングルタップでコントロールボタン群をオンオフ
	public boolean onSingleTapConfirmed(MotionEvent e) {
		Log.d("VccMapOverlay", "onSingleTapConfirmed");
		//EditTextがフォーカス持ってる状態だったらそのフォーカス外れるだけでトグルにしない
		if(searchTextHasLostFocus) {
			searchTextHasLostFocus = false;
			return false;
		}
		//アイテムクリックかどうかを判定
		int x = (int)e.getX();
		int y = (int)e.getY();
		StationItem selectedItem = null;
		//if(focusedItem != null && focusedItem.getMarker().getBounds().contains(x, y)) {
		//	selectedItem = focusedItem;
		//} else if(drawItems != null) {
		if(drawItems != null) {
			for(int index = drawItems.size() - 1; index >= 0; --index) {
				//if(index != focusedItemIndex) {
					StationItem item = drawItems.get(index);
					/*if(item.getMarker().getBounds().contains(x, y)) {
						selectedItem = item;
						break;
					}*/
				//}
			}
		}
		if(selectedItem != null) {
			//if(focusedItem != selectedItem) {
			//	if(focusedItem != null) {
			//		mapView.invalidate(focusedItem.getMarker().getBounds());
			//	}
			//	focusedItem = selectedItem;
			//	Rect bounds = focusedItem.getMarker().getBounds();
				//Rect bounds = selectedItem.getMarker().getBounds();
			//	mapView.invalidate(bounds);
			//	Station station = focusedItem.getStation();
				Station station = selectedItem.getStation();
				if(station.isReadyToCreateSubtitle()) {
					//balloon.setStation(focusedItem.getStation());
					balloon.setStation(station);
					//balloon.realize(bounds.height());
				} else if(databaseService != null) {
					databaseService.callDatabase(this, Database.MethodName.LOAD_LINES, station);
				}
			//} else {
			//	balloon.close();
			//	balloon.clearLocation();
			//	selectedItem = null;
			//}
			return false;
		} else if(balloon.getStation() != null) {
			balloon.close();
			balloon.clearLocation();
			return false;
		}

		if(controlsContainer != null && controlsContainer.getAnimation() == null) {
			toggleVisibility();
		}
		return false;
	}

	//バルーンの大きさが確定した
	public void onBalloonSizeChanged(MapBalloonView balloonView) {
		Projection projection = mapView.getProjection();
		Point point = projection.toPixels(mapView.getMapCenter(), null);
		Point offset = balloon.getDecideOffset(listener.onCalcVisibleRegion());
		point.offset(offset.x, offset.y);
		mapView.getController().animateTo(projection.fromPixels(point.x, point.y));
	}

	//バルーンがタッチされた
	public void onClick(View balloon) {
		if(balloon == this.balloon && this.balloon.getStation() != null) {
			listener.onBalloonClick(this.balloon.getStation());
		}
	}

	//コントロールボタン群のアニメーション
	protected void toggleVisibility() {
		if(isControlsVisible()) {
			controlsContainer.startAnimation(fadeOutAnimation);
			controlsContainer.setVisibility(View.INVISIBLE);
		} else {
			controlsContainer.startAnimation(fadeInAnimation);
			controlsContainer.setVisibility(View.VISIBLE);
		}
	}

	private void drawMarker(Canvas canvas, Projection projection, StationItem item) {
		Point point = projection.toPixels(/*item.getStation().getPoint()*/null, null);
		/*Drawable marker = item.getMarker();
		int width = marker.getIntrinsicWidth();
		int height = marker.getIntrinsicHeight();
		point.x -= width / 2;
		point.y -= height;
		marker.setBounds(point.x, point.y, point.x + width, point.y + height);
		marker.draw(canvas);*/
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		Projection projection = mapView.getProjection();
		//次に位置アイコン
		if(drawItems != null) {
			/*int index = 0;
			for(StationItem item : drawItems) {
				if(index != focusedItemIndex) {
					drawMarker(canvas, projection, item);
				}
			}
			if(focusedItem != null) {
				drawMarker(canvas, projection, focusedItem);
			}*/
			for(StationItem item : drawItems) {
				drawMarker(canvas, projection, item);
			}
		}
		//Log.d("VccMapOverlay", "draw()");
	}

	public boolean isControlsVisible() {
		return controlsContainer.getVisibility() == View.VISIBLE;
	}

	public void clearStationItems() {
		items.clear();
	}

	public void updateStations(SparseArray<Station> stations) {
		//Debug.startMethodTracing("updateMapItem");
		Log.d(this.getClass().getName(), "updateLocations");
		Resources resources = mapView.getResources();
		SparseArray<StationItem> newItems = new SparseArray<StationItem>();
		drawItems = new ArrayList<StationItem>(stations.size());
		//boolean focusedItemIsLeft = false;
		//int focusedItemCode = focusedItem != null ? focusedItem.getStation().getCode() : 0;
		final int NO_BALLOON = -1;
		final int VALID_BALLOON = -2;
		int balloonCode = balloon != null && balloon.getStation() != null ? balloon.getStation().getCode() : NO_BALLOON;
		for(int index = 0; index < stations.size(); ++index) {
			int code = stations.keyAt(index);
			StationItem item = items.get(code);
			if(item == null) {
				Station station = stations.valueAt(index);
				item = new StationItem(station);
				//item.setMarker(station.getMarker(resources));
			}
			//focusedItemIsLeft = focusedItemIsLeft || (focusedItemCode != 0 && item.getStation().getCode() == focusedItemCode);
			newItems.put(code, item);
			drawItems.add(item);
			if(code == balloonCode) {
				balloonCode = VALID_BALLOON;
			}
		}
		items = newItems;
		Collections.sort(drawItems, StationItem.getOverlayComparator());
		if(balloonCode != NO_BALLOON && balloonCode != VALID_BALLOON) {
			balloon.close();
			balloon.clearLocation();
		}
		//if(focusedItemIsLeft) {
		//	for(int index = drawItems.size() - 1; index >= 0; --index) {
		//		StationItem item = drawItems.get(index);
		//		if(item.getStation().getCode() == focusedItemCode) {
		//			focusedItemIndex = index;
		//			break;
		//		}
		//	}
		//} else {
		//	focusedItem = null;
		//	focusedItemIndex = -1;
		//}
		mapView.invalidate();
		//Debug.stopMethodTracing();
	}

	public void updateStation(Station station) {
		StationItem item = items.get(station.getCode());
		if(item != null) {
			item.setStation(station);
			//item.setMarker(station.getMarker(mapView.getResources()));
			mapView.invalidate();
			if(balloon != null && balloon.getStation() != null && balloon.getStation().equals(station)) {
				balloon.setStation(station);
			}
		}
	}

	public void removeStation(Station station) {
		final int code = station.getCode();
		final StationItem item = items.get(code);
		if(item != null) {
			items.remove(code);
			final int index = drawItems.indexOf(items);
			if(index >= 0) {
				drawItems.remove(index);
			}
			if(balloon != null && balloon.getStation() != null && balloon.getStation().equals(station)) {
				balloon.close();
				balloon.clearLocation();
			}
			mapView.invalidate();
		}
	}

	public void getStationsForList(ArrayList<Station> stations, GeoPoint centerPoint) {
		stations.clear();
		stations.ensureCapacity(items.size());
		for(int index = items.size() - 1; index >= 0; --index) {
			final Station station = items.valueAt(index).getStation();
			station.calcDistance(/*centerPoint*/null);
			stations.add(station);
		}
		Collections.sort(stations, Station.getDistanceComparator());
	}

	@Override
	public void onDatabaseResult(long sequence, String methodName, Object result) {
		//if(focusedItem != null && focusedItem.getStation() == result) {
		//	balloon.setStation(focusedItem.getStation());
		//	balloon.realize(focusedItem.getMarker().getBounds().height());
		//}
		final Station station = (Station)result;
		final StationItem item = station != null ? items.get(station.getCode()) : null;
		if(item != null) {
			balloon.setStation(station);
			//balloon.realize(item.getMarker().getBounds().height());
		}
	}

}
