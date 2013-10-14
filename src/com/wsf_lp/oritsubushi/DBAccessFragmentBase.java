package com.wsf_lp.oritsubushi;

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

	private boolean isAlive;
	private long recentDBNotifySequence;
	
	private DatabaseServiceConnector connector;
	private DatabaseService databaseService;
	private OritsubushiBroadcastReceiver broadcastReceiver;

	protected boolean isAlive() { return isAlive; }
	protected DatabaseService getDatabaseService() { return databaseService; }

	protected abstract void onQueryFinished(String methodName, Object result, long sequence);
	protected abstract void onDatabaseUpdated();
	protected abstract void onStationUpdated(Station station);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isAlive = true;
		recentDBNotifySequence = savedInstanceState != null ? savedInstanceState.getLong(STATE_RECENT_DB_NOTIFY) : 0;
		
        broadcastReceiver = new OritsubushiBroadcastReceiver(this);
        broadcastReceiver.registerTo(getActivity(), OritsubushiNotificationIntent.getIntentFilter());
        connector = new DatabaseServiceConnector();
        connector.connect(getActivity(), this);
        
        setRetainInstance(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_RECENT_DB_NOTIFY, recentDBNotifySequence);
	}
	
	@Override
	public void onDestroy() {
		isAlive = false;
		broadcastReceiver.unregisterFrom(getActivity());
		connector.disconnect();
		super.onDestroy();
	}

	@Override
	public void onDatabaseConnected(DatabaseService service) {
		if(isAlive()) {
			this.databaseService = service;
			//TODO: recentDBNotifySequence をDBに問い合わせて最新の更新情報を得る
		}
	}

	@Override
	public void onDatabaseDisconnected() {
		this.databaseService = null;
	}
	
	@Override
	public void onDatabaseResult(long sequence, String methodName, Object result) {
		if(isAlive()) {
			onQueryFinished(methodName, result, sequence);
		}
	}
	
	@Override
	public void onDatabaseUpdated(Station station, int sequence) {
		recentDBNotifySequence = sequence;
		if(station == null) {
			onDatabaseUpdated();
		} else {
			onStationUpdated(station);
		}
	}

}
