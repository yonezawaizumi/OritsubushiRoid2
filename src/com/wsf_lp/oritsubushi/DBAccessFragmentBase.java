package com.wsf_lp.oritsubushi;

import java.util.List;

import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

import android.content.IntentFilter;
import android.os.Bundle;

public abstract class DBAccessFragmentBase extends MenuableFragmentBase
	implements DatabaseResultReceiver,
		DatabaseServiceConnector.Listener,
		OritsubushiBroadcastReceiver.UpdateListener {

	public static final String STATE_RECENT_DB_NOTIFY = "recentDBNotify";
	protected static final long RETRY_MSEC = 5000;

	private boolean mIsAlive;
	private boolean mIsEnabled;
	private long mRecentDBNotifySequence;

	private DatabaseServiceConnector mConnector;
	private DatabaseService mDatabaseService;
	private OritsubushiBroadcastReceiver mBroadcastReceiver;

	public boolean isAlive() { return mIsAlive; }
	protected DatabaseService getDatabaseService() { return mDatabaseService; }
	public boolean isDatabaseEnabled() { return mIsEnabled; }

	protected long callDatabase(String methodName, Object... args) {
		return mDatabaseService != null ? getDatabaseService().callDatabase(this, methodName, args) : Long.MAX_VALUE;
	}

	//must override
	protected abstract void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations);
	protected abstract void onQueryFinished(String methodName, Object result, long sequence);
	protected abstract void onDatabaseUpdated(boolean isFirst);
	protected abstract void onStationUpdated(Station station);

	//overridable
	protected IntentFilter getIntentFilter() {
		return OritsubushiNotificationIntent.getIntentFilter();
	}
	//overridable
	public void onDatabaseDisconnected(boolean dummy) {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mIsAlive = true;
		mRecentDBNotifySequence = savedInstanceState != null ? savedInstanceState.getLong(STATE_RECENT_DB_NOTIFY) : 0;

		mConnector = new DatabaseServiceConnector();
		mConnector.connect(getActivity(), this);

        setRetainInstance(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_RECENT_DB_NOTIFY, mRecentDBNotifySequence);
	}

	@Override
	public void onDestroy() {
		mIsAlive = false;
		mBroadcastReceiver.unregisterFrom(getActivity());
		mConnector.disconnect();
		super.onDestroy();
	}

	@Override
	public final void onDatabaseConnected(DatabaseService service) {
		mDatabaseService = service;
		//mIsEnabled = service.isEnabled();
		//TODO: recentDBNotifySequence をDBに問い合わせて最新の更新情報を得る
		callDatabase(Database.MethodName.ECHO, this);
		//onDatabaseConnected(mIsEnabled, false, null);
	}

	@Override
	public final void onDatabaseDisconnected() {
		mDatabaseService = null;
		mIsEnabled = false;
		onDatabaseDisconnected(false);
	}

	@Override
	public final void onDatabaseResult(long sequence, String methodName, Object result) {
		if(Database.MethodName.ECHO.equals(methodName)) {
			mIsEnabled = true;
			mBroadcastReceiver = new OritsubushiBroadcastReceiver(this);
			mBroadcastReceiver.registerTo(getActivity(), getIntentFilter());
			onDatabaseConnected(true, false, null);
		} else if(isAlive()) {
			onQueryFinished(methodName, result, sequence);
		}
	}

	@Override
	public final void onDatabaseUpdated(Station station, int sequence) {
		mRecentDBNotifySequence = sequence;
		if(station == null) {
			boolean isFirst = !mIsEnabled;
			mIsEnabled = true;
			onDatabaseUpdated(isFirst);
		} else {
			onStationUpdated(station);
		}
	}

}
