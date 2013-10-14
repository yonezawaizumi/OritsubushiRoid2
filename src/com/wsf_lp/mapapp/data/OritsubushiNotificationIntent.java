package com.wsf_lp.mapapp.data;

import com.wsf_lp.oritsubushi.R;

import android.content.Intent;
import android.content.IntentFilter;

public class OritsubushiNotificationIntent extends Intent {
	private static final String NAME = "OritsubushiNotificationIntent";
	public static final String ACTION_UPDATED =  NAME + ".Update";
	public static final String ACTION_MAP_STATUS_CHANGED = NAME + ".MapStatusChange";
	public static final String ACTION_MAP_MOVE_TO = NAME + ".MapMoveTo";
	public static final String ACTION_SYNC_FINISH = NAME + ".SyncFinish";
	private static final String TAG_STATION = "station";
	private static final String TAG_SYNC = "sync";
	private static final String TAG_SEQUENCE = "sequence";

	public OritsubushiNotificationIntent setStation(Station station, int sequence) {
		putExtra(TAG_STATION, station);
		putExtra(TAG_SEQUENCE, sequence);
		setAction(ACTION_UPDATED);
		return this;
	}
	public OritsubushiNotificationIntent setNeedsReload(int sequence) {
		putExtra(TAG_SEQUENCE, sequence);
		setAction(ACTION_UPDATED);
		return this;
	}
	public OritsubushiNotificationIntent setMapStatusChanged() {
		setAction(ACTION_MAP_STATUS_CHANGED);
		return this;
	}
	public OritsubushiNotificationIntent setMapMoveTo(Station station) {
		putExtra(TAG_STATION, station);
		setAction(ACTION_MAP_MOVE_TO);
		return this;
	}
	public OritsubushiNotificationIntent setSyncFinish(int code) {
		putExtra(TAG_SYNC, code);
		setAction(ACTION_SYNC_FINISH);
		return this;
	}

	public boolean needsReload() {
		return ACTION_UPDATED.equals(getAction()) && !hasExtra(TAG_STATION);
	}
	public Station getStation() {
		return hasExtra(TAG_STATION) ? (Station)getParcelableExtra(TAG_STATION) : null;
	}
	public boolean isMapStatusChangedIntent() {
		return ACTION_MAP_STATUS_CHANGED.equals(getAction());
	}
	public int getSyncFinishCode() {
		return getIntExtra(TAG_SYNC, R.string.sync_error);
	}

	static public IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_UPDATED);
		return filter;
	}

	static public IntentFilter getMapIntentFilter() {
		IntentFilter filter = getIntentFilter();
		filter.addAction(ACTION_MAP_STATUS_CHANGED);
		filter.addAction(ACTION_MAP_MOVE_TO);
		return filter;
	}

	static public IntentFilter getSyncFinishIntentFilter() {
		IntentFilter filter = getIntentFilter();
		filter.addAction(ACTION_SYNC_FINISH);
		return filter;
	}

	static public boolean isMapStatusChangedIntent(Intent intent) {
		return ACTION_MAP_STATUS_CHANGED.equals(intent.getAction());
	}

	static public boolean isMapMoveToIntent(Intent intent) {
		return ACTION_MAP_MOVE_TO.equals(intent.getAction());
	}

	static public boolean isSyncFInishIntent(Intent intent) {
		return ACTION_SYNC_FINISH.equals(intent.getAction());
	}


	static public Station getStation(Intent intent) {
		if(intent.hasExtra(TAG_STATION)) {
			return intent.getParcelableExtra(TAG_STATION);
		} else {
			return null;
		}
	}

	static public int getSequence(Intent intent) {
		if(intent.hasExtra(TAG_SEQUENCE)) {
			return intent.getIntExtra(TAG_SEQUENCE, 0);
		} else {
			return 0;
		}
	}

	static public int getSyncFinishCode(Intent intent) {
		if(intent.hasExtra(TAG_SYNC)) {
			return intent.getIntExtra(TAG_SYNC, 0);
		} else {
			return 0;
		}
	}
}
