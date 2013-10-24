package com.wsf_lp.oritsubushi;

import java.util.List;

import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class DBAccessFragmentBase extends Fragment
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

	protected abstract void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations);
	protected abstract void onQueryFinished(String methodName, Object result, long sequence);
	protected abstract void onDatabaseUpdated(boolean isFirst);
	protected abstract void onStationUpdated(Station station);

	protected long callDatabase(String methodName, Object... args) {
		return isDatabaseEnabled() ? getDatabaseService().callDatabase(this, methodName, args) : Long.MAX_VALUE;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mIsAlive = true;
		mRecentDBNotifySequence = savedInstanceState != null ? savedInstanceState.getLong(STATE_RECENT_DB_NOTIFY) : 0;

        mBroadcastReceiver = new OritsubushiBroadcastReceiver(this);
        mBroadcastReceiver.registerTo(getActivity(), OritsubushiNotificationIntent.getIntentFilter());
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
		if(isAlive()) {
			mDatabaseService = service;
			mIsEnabled = service.isEnabled();
			//TODO: recentDBNotifySequence をDBに問い合わせて最新の更新情報を得る
			onDatabaseConnected(mIsEnabled, false, null);
		}
	}

	@Override
	public final void onDatabaseDisconnected() {
		mDatabaseService = null;
		mIsEnabled = false;
		onDatabaseDisconnected(false);
	}

	public void onDatabaseDisconnected(boolean dummy) {
	}

	@Override
	public final void onDatabaseResult(long sequence, String methodName, Object result) {
		if(isAlive()) {
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
