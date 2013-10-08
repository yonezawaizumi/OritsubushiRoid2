package com.wsf_lp.mapapp;

import java.util.List;

import com.wsf_lp.mapapp.data.Line;
import com.wsf_lp.mapapp.data.Station;
import com.wsf_lp.oritsubushi.R;

import com.google.android.maps.MapView;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
public class MapBalloonView extends FrameLayout implements OnTouchListener {
	private View container;
	private View balloonHolder;
	private TextView titleView;
	private TextView operatorView;
	private TextView linesView;
	private boolean dragging;

	public interface MapBalloonViewListener extends OnClickListener {
		public void onBalloonSizeChanged(MapBalloonView balloonView);
	}

	private MapView mapView;
	private Station station;
	private MapBalloonViewListener listener;

	public MapBalloonView(MapView mapView) {
		super(mapView.getContext());
		this.mapView = mapView;
		LayoutInflater inflater = LayoutInflater.from(mapView.getContext());
		container = inflater.inflate(R.layout.map_balloon, null);
		balloonHolder = (View)container.findViewById(R.id.balloon_holder);
		titleView = (TextView)container.findViewById(R.id.title_view);
		operatorView = (TextView)container.findViewById(R.id.operator_view);
		linesView = (TextView)container.findViewById(R.id.lines_view);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;
		container.setVisibility(GONE);
		addView(container, params);
		balloonHolder.setOnTouchListener(this);
	}

	public Station getStation() { return station; }
	public void setStation(Station station) {
		this.station = station;
	}
	public void clearLocation() { this.station = null; }

	public MapBalloonViewListener getListener() { return listener; }
	public void setListener(MapBalloonViewListener listener) {
		this.listener = listener;
	}

	private void setHighlighted(boolean highlighted) {
		balloonHolder.setBackgroundDrawable(getMapView().getContext().getResources().getDrawable(
				highlighted ? R.drawable.map_balloon_highlighted : R.drawable.map_balloon
		));
		invalidate();
	}

	private boolean contains(MotionEvent e) {
		Rect hitRect = new Rect();
		balloonHolder.getHitRect(hitRect);
		return hitRect.contains((int)e.getX(), (int)e.getY());
	}

	public boolean onTouch(View v, MotionEvent e) {
		Log.d("MapBalloonView", "(" + e.getX() + ", " + e.getY() + ")");
		switch(e.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			setHighlighted(true);
			dragging = true;
			return true;
		case MotionEvent.ACTION_UP:
			if(dragging && getListener() != null && contains(e)) {
				getListener().onClick(this);
			}
			dragging = false;
			setHighlighted(false);
			return true;
		case MotionEvent.ACTION_MOVE:
			if(dragging && !contains(e)) {
				setHighlighted(false);
				dragging = false;
			}
			return true;
		case MotionEvent.ACTION_CANCEL:
			setHighlighted(false);
			dragging = false;
			return true;
		}
		return true;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if(getListener() != null/* &&  oldw == 0*/ && w != 0) {
			getListener().onBalloonSizeChanged(this);
		}
	}
	public Point getDecideOffset(Region visibleRegion) {
		int offsetDx = 0;
		int offsetDy = 0;
		Region balloon = new Region(getLeft(), getBottom() - 1, getRight(), getBottom());
		if(balloon.op(visibleRegion, Op.DIFFERENCE)) {
			boolean correctedHorizontal = false;
			if(!balloon.isComplex()) {
				Rect rect = balloon.getBounds();
				//左右のはみ出した分移動させてみる
				if(!visibleRegion.contains(rect.left - 1, rect.bottom - 1)) {
					offsetDx = rect.width();
					correctedHorizontal = true;
				} else if(!visibleRegion.contains(rect.right, rect.bottom)) {
					if(visibleRegion.contains(getLeft() - rect.width(), rect.bottom)) {
						offsetDx = -rect.width();
						correctedHorizontal = true;
					}
				}
			}
			if(!correctedHorizontal) {
				//両側はみ出し→左に寄せる
				Region diff = new Region(balloon);	
				diff.op(balloon, Op.DIFFERENCE);
				offsetDx = diff.getBounds().left - getLeft();
			}
		}
		balloon = new Region(getLeft() + offsetDx, getTop(), getLeft() + offsetDx + 1, getBottom());
		if(balloon.op(visibleRegion,  Op.DIFFERENCE)) {
			Rect rect = balloon.getBounds();
			if(!visibleRegion.contains(rect.left, rect.top)) {
				offsetDy = rect.height();
			}
		}
/*		if(diff.isEmpty()) {
			offsetDx = offsetDy = 0;
		} else {
		if(getLeft() < validRectInMapView.left) {
			offsetDx = getLeft() - validRectInMapView.left;
		} else if(getRight() > validRectInMapView.right) {
			offsetDx = getRight() - validRectInMapView.right;
		} else {
			offsetDx = 0;
		}
		if(getTop() < validRectInMapView.top) {
			offsetDy = getTop() - validRectInMapView.top;
		} else {
			offsetDy = 0;
		}*/
		Log.d("MapBalloonView", "(" + offsetDx + "," + offsetDy + ") " + getLeft() + "/" + getRight());
		return new Point(-offsetDx, -offsetDy);
	}

	protected MapView getMapView() { return mapView; }

	public void realize(int offsetBottom) {
		setPadding(0, 0, 0, offsetBottom);
		Station station = getStation();
		if(station != null) {
			titleView.setText(station.getName());
			operatorView.setText(station.getOperator().getName());
			List<Line> lines = station.getLines();
			if(!lines.isEmpty()) {
				StringBuilder linesString = new StringBuilder();
				for(Line line : lines) {
					linesString.append(line.getName());
					linesString.append(", ");
				}
				linesView.setText(linesString.substring(0, linesString.length() - 2));
			} else {
				linesView.setText("");
			}
			MapView.LayoutParams params = new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT,
					station.getPoint(),
					MapView.LayoutParams.BOTTOM_CENTER
			);
			params.mode = MapView.LayoutParams.MODE_MAP;
			if(getParent() == getMapView()) {
				setLayoutParams(params);
			} else {
				getMapView().addView(this, params);
			}
			container.setVisibility(VISIBLE);
		} else {
			container.setVisibility(GONE);
		}
	}

	public void close() {
		container.setVisibility(GONE);
	}
}
