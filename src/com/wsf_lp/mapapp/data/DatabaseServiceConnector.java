package com.wsf_lp.mapapp.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DatabaseServiceConnector implements ServiceConnection {

	public interface Listener {
		void onDatabaseConnected(DatabaseService service);
		void onDatabaseDisconnected();
	}

	private boolean bound;
	private Listener listener;
	private DatabaseService databaseService;

	private Context context;
	private Intent intent;

	public boolean isBound() { return bound; }
	public DatabaseService getDatabaseService() { return databaseService; }

	public void connect(Context context, Listener listener) {
		this.context = context;
		this.listener = listener;
		intent = new Intent(context, DatabaseService.class);
		context.bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	public void disconnect() {
		context.unbindService(this);
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		databaseService = ((DatabaseService.DatabaseBinder)service).getService();
		context.startService(intent);
		bound = true;
		if(listener != null) {
			listener.onDatabaseConnected(databaseService);
		}
	}

	public void onServiceDisconnected(ComponentName name) {
		bound = false;
		if(listener != null) {
			listener.onDatabaseDisconnected();
		}
	}

}
