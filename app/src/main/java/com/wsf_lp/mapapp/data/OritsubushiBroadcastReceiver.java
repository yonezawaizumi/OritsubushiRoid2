package com.wsf_lp.mapapp.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class OritsubushiBroadcastReceiver extends BroadcastReceiver {
	public interface UpdateListener {
		public void onDatabaseUpdated(Station station, int sequence);
	}
	public interface MapListener {
		public void onMapStatusChanged();
		public void onMapMoveTo(Station station);
	}
	public interface SyncListener {
		public void onSyncFinish(int code);
	}

	private UpdateListener listener;

	public OritsubushiBroadcastReceiver(UpdateListener listener) {
		this.listener = listener;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(OritsubushiNotificationIntent.isSyncFInishIntent(intent)) {
			if(listener instanceof SyncListener) {
				((SyncListener)listener).onSyncFinish(OritsubushiNotificationIntent.getSyncFinishCode(intent));
			}
		} else if(OritsubushiNotificationIntent.isMapStatusChangedIntent(intent)) {
			if(listener instanceof MapListener) {
				((MapListener)listener).onMapStatusChanged();
			}
		} else if(OritsubushiNotificationIntent.isMapMoveToIntent(intent)) {
			if(listener instanceof MapListener) {
				((MapListener)listener).onMapMoveTo(OritsubushiNotificationIntent.getStation(intent));
			}
		} else {
			listener.onDatabaseUpdated(OritsubushiNotificationIntent.getStation(intent), OritsubushiNotificationIntent.getSequence(intent));
		}
	}

	public void registerTo(Context context, IntentFilter intentFilter) {
		context.registerReceiver(this, intentFilter);
	}

	public void unregisterFrom(Context context) {
		context.unregisterReceiver(this);
	}

}
