package com.wsf_lp.oritsubushi;

import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class DBAccessFragmentBase extends Fragment
	implements DatabaseResultReceiver,
		DatabaseServiceConnector.Listener,
		OritsubushiBroadcastReceiver.UpdateListener {

	protected static final long RETRY_MSEC = 5000;

	private boolean isAlive;

	private DatabaseServiceConnector connector;
	private DatabaseService databaseService;
	private OritsubushiBroadcastReceiver broadcastReceiver;

	protected boolean isAlive() { return isAlive; }
	protected DatabaseService getDatabaseService() { return databaseService; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isAlive = true;
        broadcastReceiver = new OritsubushiBroadcastReceiver(this);
        broadcastReceiver.registerTo(getActivity(), OritsubushiNotificationIntent.getIntentFilter());
        connector = new DatabaseServiceConnector();
        connector.connect(getActivity(), this);
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
		}
	}

	@Override
	public void onDatabaseDisconnected() {
		this.databaseService = null;
	}

}
