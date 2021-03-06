package com.wsf_lp.mapapp.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wsf_lp.oritsubushi.MainActivity;
import com.wsf_lp.oritsubushi.R;
import com.wsf_lp.oritsubushi.PreferenceKey;
import com.wsf_lp.utils.MethodUtil;
import com.wsf_lp.android.SavedLocale;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import android.widget.Toast;

import cz.msebera.android.httpclient.Header;

public class DatabaseService extends Service
		implements SharedPreferences.OnSharedPreferenceChangeListener,
			Database.OnProgressListener {
	public static final String WORKER_THREAD_NAME = "DBWorker";
	public static final String WAIT_END_THREAD_NAME = "DBWorkerWaitEnd";
	public static final int WAIT_END_SEC = 10;
	private static final int NOTIFICATION_ID_INITIALIZE = 1;
	private static final int NOTIFICATION_ID_SYNC = 2;
	private static final int NOTIFICATION_ID_SERVICE = 3;

	public static final String CHANNEL_ID = "database";

	//UIスレッドからのみ呼ばれる
	private final IBinder binder = new DatabaseBinder(this);
	private Map<String, Method> methodMap = MethodUtil.createMethodMap(Database.class);
	private SavedLocale locale;

	public static class DatabaseBinder extends Binder {
		private WeakReference<DatabaseService> service;
		DatabaseBinder(DatabaseService service) {
			this.service = new WeakReference<DatabaseService>(service);
		}
		public DatabaseService getService() {
			return service.get();
		}
	}

	//UIスレッド・ワーカスレッドの両方から呼ばれる
	public static final int OK = 0;
	public static final int DATABASE_NOT_READY = -1;

	private static class Request implements Comparable<Request> {
		private final static AtomicLong sequencer = new AtomicLong(1);
		public WeakReference<DatabaseResultReceiver> receiver;
		public Method method;
		public Object[] args;
		private final long sequence = sequencer.getAndIncrement();
		public Request(DatabaseResultReceiver receiver, Method method, Object[] args) {
			this.receiver = new WeakReference<DatabaseResultReceiver>(receiver);
			this.method = method;
			this.args = args;
		}
		private Request() {}
		public static Request createTerminateRequest() {
			return new Request();
		}
		public boolean isTerminator() { return method == null; }
		public int compareTo(Request another) {
			if(isTerminator()) {
				return -1;
			} else if(another.isTerminator()) {
				return 1;
			} else {
				return sequence - another.sequence < 0 ? -1 : 1;
			}
		}
		public long getSequence() {
			return sequence;
		}
	}
	private PriorityBlockingQueue<Request> requests = new PriorityBlockingQueue<Request>();
	private Handler handler = new Handler();
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> checkEndFuture;
	//private Notification initializingNotification;
	private PendingIntent initializingPendingIntent;
	//private Notification syncingNotification;
	private PendingIntent syncingPendingIntent;

	private AtomicBoolean isSyncing = new AtomicBoolean();

	protected int getMaxStations() {
		//この呼び出しの前にSharedPreferencesは初期化されていないといけない
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKey.MAX_STATIONS, ""));
	}

	//ワーカスレッド本体
	private static class Worker extends Thread {
		private WeakReference<DatabaseService> service;
		public Worker(DatabaseService service) {
			super(WORKER_THREAD_NAME);
			this.service = new WeakReference<DatabaseService>(service);
		}
		@Override
		public void run() {
			Database database = new Database();
			try {
				DatabaseService service = this.service.get();
				if(service == null) {
					return;
				}
				final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(service);
				int version = Integer.parseInt(service.getString(R.string.database_version));
				version = Math.max(preferences.getInt(PreferenceKey.DATABASE_VERSION, version), version);
				version = database.initialize(service, service.getMaxStations(), version, service);
				preferences.edit().putInt(PreferenceKey.DATABASE_VERSION, version).commit();
			} catch(SQLiteException e) {
				e.printStackTrace();
				DatabaseService service = this.service.get();
				if(service == null) {
					return;
				}
				service.handler.post(new Runnable() {
					public void run() {
						DatabaseService service = Worker.this.service.get();
						if(service == null) {
							return;
						}
						Toast.makeText(service, R.string.database_cannot_open, Toast.LENGTH_LONG).show();
					}
				});
				database = null;
			}
			{
				DatabaseService service = this.service.get();
				if(service == null) {
					return;
				}
				service.handler.post(new Runnable() {
					@Override
					public void run() {
						DatabaseService service = Worker.this.service.get();
						if(service == null) {
							return;
						}
						service.onProgress(-1);
						//TODO: sequence
						service.sendBroadcast(new OritsubushiNotificationIntent().setNeedsReload(/*sequence*/0));
					}
				});
			}
			for(;;) {
				Request request = null;
				try {
					DatabaseService service = this.service.get();
					if(service == null) {
						return;
					}
					request = service.requests.take();
				} catch (InterruptedException e) {
				}
				if(request == null) {
					continue;
				} else if(request.isTerminator()) {
					return;
				}
				Object result = null;
				try {
					result = request.method.invoke(database, request.args);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				if(result == null) {
					continue;
				}
				DatabaseService service = this.service.get();
				if(service == null) {
					return;
				}
				//dirty hack
				if(Database.MethodName.UPDATE_SYNC.equals(request.method.getName())) {
					final int version = (Integer)result;
					if(version > 0) {
						PreferenceManager.getDefaultSharedPreferences(service).edit().putInt(PreferenceKey.DATABASE_VERSION, version).commit();
					}
					service.handler.post(new Runnable() {
						@Override
						public void run() {
							DatabaseService service = Worker.this.service.get();
							if(service != null) {
								service.finishSync(version > 0);
							}
						}
					});
				} else if(Database.MethodName.PREPARE_SYNC.equals(request.method.getName())) {
					final Database.SyncPreparationProperties results = (Database.SyncPreparationProperties)result;
					service.handler.post(new Runnable() {
						@Override
						public void run() {
							DatabaseService service = Worker.this.service.get();
							if(service != null) {
								service.requestSync(results);
							}
						}
					});
				} else if(result instanceof Intent) {
					service.sendBroadcast((Intent)result);
				} else if(request.receiver.get() != null) {
					final Request request_ = request;
					final Object result_ = result;
					service.handler.post(new Runnable() {
						public void run() {
							DatabaseResultReceiver receiver = request_.receiver.get();
							if(receiver != null) {
								receiver.onDatabaseResult(request_.getSequence(), request_.method.getName(), result_);
							}
						}
					});
				}
			}
		}
	}

	//以下、UIスレッドのみから呼ばれる

	private int previousDatabaseInitializingPercentile;

	@Override
	public void onCreate() {
		locale = new SavedLocale();
		locale.compareAndSave(getResources().getConfiguration());
		previousDatabaseInitializingPercentile = 0;
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager nm = getSystemService(NotificationManager.class);
			if (nm.getNotificationChannel(CHANNEL_ID) == null) {
				String name = getString(R.string.notification_channel_database);
				int importance = NotificationManager.IMPORTANCE_HIGH;
				NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
				channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
				channel.enableVibration(false);
				channel.setShowBadge(false);
				channel.setSound(null, null);
				nm.createNotificationChannel(channel);
			}
		}
		(new Worker(this)).start();
	}


	@Override
	public void onProgress(int percentile) {
		if(percentile == 0) {
			final String statusText = getString(R.string.notification_database_initializing, percentile);
			initializingPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
				.setSmallIcon(R.drawable.notification)
				.setTicker(statusText)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(statusText)
				.setWhen(System.currentTimeMillis())
				//.setDefaults(Notification.FLAG_NO_CLE	AR)
				.setAutoCancel(false)
				.setOngoing(true)
				.setContentIntent(initializingPendingIntent);
			startForeground(NOTIFICATION_ID_INITIALIZE, builder.build());
		} else if(percentile < 0) {
			NotificationManager manager = (NotificationManager)this.getSystemService(Service.NOTIFICATION_SERVICE);
			manager.cancel(NOTIFICATION_ID_INITIALIZE);
			stopForeground(true);
			initializingPendingIntent = null;
		} else if(percentile > previousDatabaseInitializingPercentile) {
			previousDatabaseInitializingPercentile = percentile;
			final String statusText = getString(R.string.notification_database_initializing, percentile);
			final NotificationManager manager = (NotificationManager)this.getSystemService(Service.NOTIFICATION_SERVICE);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
				.setSmallIcon(R.drawable.notification)
				.setTicker(statusText)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(statusText)
				.setWhen(System.currentTimeMillis())
				//.setDefaults(Notification.FLAG_NO_CLEAR)
				.setAutoCancel(false)
				.setOngoing(true)
				.setContentIntent(initializingPendingIntent);
			manager.notify(NOTIFICATION_ID_INITIALIZE, builder.build());
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String statusText = getString(R.string.notification_database_service);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
				.setSmallIcon(R.drawable.notification)
				.setTicker(statusText)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(statusText)
				.setWhen(System.currentTimeMillis())
				//.setDefaults(Notification.FLAG_NO_CLE	AR)
				.setAutoCancel(true)
				.setOngoing(false);
		startForeground(NOTIFICATION_ID_SERVICE, builder.build());
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		if(checkEndFuture != null) {
			checkEndFuture.cancel(true);
			checkEndFuture = null;
		}
		return binder;
	}

	@Override
	public boolean onUnbind(Intent arg0) {
		checkEndFuture = scheduler.schedule(new Runnable() {
			public void run() {
				stopSelf();
			}
		}, WAIT_END_SEC, TimeUnit.SECONDS);
		return false;
	}

	@Override
	public void onDestroy() {
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		//	getSystemService(NotificationManager.class).deleteNotificationChannel(CHANNEL_ID);
		//}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		scheduler.shutdownNow();
		requests.offer(Request.createTerminateRequest());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (locale.compareAndSave(getResources().getConfiguration())) {
			callDatabase(null, "updateResources", getResources());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		callDatabase(null, "setMaxStations", getMaxStations());
	}


	private void startSyncNotification() {
		isSyncing.set(true);
		final String statusText = getString(R.string.sync_preparing);
		syncingPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
			.setSmallIcon(R.drawable.notification)
			.setTicker(statusText)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(statusText)
			.setWhen(System.currentTimeMillis())
			//.setDefaults(Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR)
			.setOngoing(true)
			.setAutoCancel(false)
			.setContentIntent(syncingPendingIntent);
		startForeground(NOTIFICATION_ID_SYNC, builder.build());
	}

	private void updateSyncingNotification(int id) {
		String statusText = getString(id);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
			.setSmallIcon(R.drawable.notification)
			.setTicker(statusText)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(statusText)
			.setWhen(System.currentTimeMillis())
			//.setDefaults(Notification.FLAG_NO_CLEAR)
			.setAutoCancel(false)
			.setOngoing(true)
			.setContentIntent(syncingPendingIntent);
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID_SYNC, builder.build());
	}

	private void finishSyncingNotification(int id) {
		stopForeground(true);
		final String statusText = getString(id);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
			.setSmallIcon(R.drawable.notification)
			.setTicker(statusText)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(statusText)
			.setWhen(System.currentTimeMillis())
			.setAutoCancel(true)
			.setContentIntent(syncingPendingIntent);
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID_SYNC, builder.build());
		syncingPendingIntent = null;
		sendBroadcast(new OritsubushiNotificationIntent().setSyncFinish(id));
		isSyncing.set(false);
	}

	public long callDatabase(DatabaseResultReceiver receiver, String methodName, Object... args) {
		Method method = methodMap.get(methodName);
		if(method == null) {
			throw new IllegalArgumentException("method " + methodName + " doesn't exist on Database class");
		}
		Request request = new Request(receiver, method, args);
		final long sequence = request.getSequence();
		requests.offer(request);
		return sequence;
	}

	public boolean isSyncing() {
		return isSyncing.get();
	}

	public boolean prepareSync(int updateDate) {
		if(isSyncing.get()) {
			return false;
		}
		startSyncNotification();
		Database.SyncPreparationProperties properties = new Database.SyncPreparationProperties();
		Context context = getApplicationContext();
		properties.file = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED ? context.getExternalCacheDir() : null;
		if(properties.file == null) {
			properties.file = context.getCacheDir();
		}
		properties.updateDate = Integer.toString(updateDate);
		callDatabase(null, Database.MethodName.PREPARE_SYNC, properties);
		return true;
	}

	private void requestSync(final Database.SyncPreparationProperties results) {
		if(results.file == null) {
			finishSyncingNotification(R.string.sync_io_error);
			return;
		}

		updateSyncingNotification(R.string.sync_server);

		RequestParams params = new RequestParams();
		params.put("v", results.databaseVersion);
		params.put("d", results.updateDate);
		try {
			params.put("f", new FileInputStream(results.file), results.file.getName(), "text/plain");
			final HttpClientForSync client = new HttpClientForSync(this);
			client.post(this, getString(R.string.sync_url), params, new HttpResponseHanderForSync(this, client, results.file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			finishSyncingNotification(R.string.sync_io_error);
		}
	}

	private static class HttpResponseHanderForSync extends BinaryHttpResponseHandler {
		private WeakReference<DatabaseService> service;
		private HttpClientForSync client;
		private File file;
		public HttpResponseHanderForSync(DatabaseService service, HttpClientForSync client, File file) {
			super(new String[] { "text/plain; charset=utf-8" });
			this.service = new WeakReference<DatabaseService>(service);
			this.client = client;
			this.file = file;
		}
		@Override
        public void onSuccess(int i, Header[] headers, byte[] bytes) {
			DatabaseService service = this.service.get();
			if(service != null) {
				if(client.getLogin()) {
					service.updateSyncingNotification(R.string.sync_database);
					service.callDatabase(null, Database.MethodName.UPDATE_SYNC, bytes);
				} else {
					service.finishSyncingNotification(R.string.sync_login_failed);
				}
			}
		}
		@Override
        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
			DatabaseService service = this.service.get();
			if(service != null) {
				service.finishSyncingNotification(R.string.sync_network_error);
			}
		}
		@Override
		public void onFinish() {
			file.delete();
		}
    }

	private void finishSync(boolean updated) {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putLong(PreferenceKey.SYNC_RECENT_DATE, System.currentTimeMillis()).commit();
		if(updated) {
			//TODO: sequence
			sendBroadcast(new OritsubushiNotificationIntent().setNeedsReload(/*sequence*/0));
		}
		finishSyncingNotification(R.string.sync_done);
	}

}
