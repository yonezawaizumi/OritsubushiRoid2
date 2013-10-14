package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.HttpClientForSync;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class SyncActivity extends Activity
		implements DatabaseServiceConnector.Listener,
			OritsubushiBroadcastReceiver.UpdateListener,
			OritsubushiBroadcastReceiver.SyncListener,
			OnClickListener {
	public static final int ID = R.id.sync;

	private ActivityChanger activityChanger;

	private boolean inInitializing;
	private boolean isActive;

	private View progress;
	private View container;
	private TextView recentDate;
	private TextView loginName;
	private Button clearRecentDateButton;
	private Button loginButton;
	private Button startSyncButton;
	private WebView webView;
	private SyncWebViewClient webViewClient;
	private DatabaseServiceConnector connector;
	private DatabaseService databaseService;
	private OritsubushiBroadcastReceiver broadcastReceiver;

	private String userName = null;
	private Date recentTime;

	private static final int STATE_NOT_AUTHED = 0;
	private static final int STATE_AUTHED = 1;
	private static final int STATE_BEGIN_LOGIN = 2;
	private static final int STATE_BEGIN_LOGIN_TWITTER = 3;
	private static final int STATE_BEGIN_LOGOUT = 4;
	private static final int STATE_REQUEST_USER = 5;

	private int authState;

	private static String syncUrl;
	private static String loginUrl;
	private static String logoutUrl;
	private static String twitterOAuthUrl;
	private static String twitterOAuthUrl1_1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        activityChanger = new ActivityChanger(this, ID);

		setContentView(R.layout.sync);

		progress = findViewById(R.id.progress);
		container = findViewById(R.id.container);
		recentDate = (TextView)findViewById(R.id.sync_recent_date);
		loginName = (TextView)findViewById(R.id.sync_login_user);
		clearRecentDateButton = (Button)findViewById(R.id.button_clear_recent_date);
		clearRecentDateButton.setOnClickListener(this);
		startSyncButton = (Button)findViewById(R.id.button_start_sync);
		startSyncButton.setOnClickListener(this);
		loginButton = (Button)findViewById(R.id.button_logout);
		loginButton.setOnClickListener(this);

		webViewClient = new SyncWebViewClient(this);
		webView = (WebView)findViewById(R.id.web_view);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(webViewClient);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

		if(syncUrl == null) {
			syncUrl = getString(R.string.sync_url);
			loginUrl = getString(R.string.users_url);
			logoutUrl = getString(R.string.logout_url);
			twitterOAuthUrl = getString(R.string.twitter_oauth_url);
			twitterOAuthUrl1_1 = getString(R.string.twitter_oauth_url_1_1);
		}

		broadcastReceiver = new OritsubushiBroadcastReceiver(this);
        broadcastReceiver.registerTo(this, OritsubushiNotificationIntent.getSyncFinishIntentFilter());
        connector = new DatabaseServiceConnector();
        connector.connect(this, this);

        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();

        startProgress(true);
        voidAllButtons();
	}

	@Override
	protected void onStart() {
		super.onStart();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final long time = preferences.getLong(PreferenceKey.SYNC_RECENT_DATE, -1);
		recentTime = time >= 0 ? new Date(time) : null;
		isActive = true;

		if(userName == null) {
			inInitializing = true;
			requestUserName();
		} else {
			updateStatuses();
		}
	}

	@Override
	protected void onStop() {
		isActive = false;
		super.onStop();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(container.getVisibility() != View.VISIBLE) {
				if(progress.getVisibility() != View.VISIBLE) {
					authState = STATE_NOT_AUTHED;
					setWebViewMode(false);
					updateStatuses();
				}
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		broadcastReceiver.unregisterFrom(this);
		connector.disconnect();
		CookieSyncManager.getInstance().stopSync();
		super.onDestroy();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        activityChanger.createMenu(menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		activityChanger.prepareMenu(menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        return activityChanger.onSelectActivityMenu(item.getItemId());
    }

	private void requestUserName() {
		switch(authState) {
		case STATE_BEGIN_LOGIN:
		case STATE_BEGIN_LOGOUT:
		case STATE_REQUEST_USER:
			return;
		}
		authState = STATE_REQUEST_USER;
		startProgress(true);
		HttpClientForSync client = new HttpClientForSync(this);
		client.get(this, syncUrl, new GetNameHandler(this, client));
	}

	public void onGotUserName(String userName) {
		authState = STATE_AUTHED;
		inInitializing = false;
		if(!userName.equals(this.userName)) {
			this.userName = userName;
			updateStatuses();
		}
	}

	public void onFailureUserName(boolean networkError) {
		authState = STATE_NOT_AUTHED;
		if(networkError) {
			Toast.makeText(this, R.string.sync_network_error, Toast.LENGTH_LONG).show();
			inInitializing = false;
			updateStatuses();
		} else {
			if(inInitializing) {
				inInitializing = false;
			}
			logout();
		}
	}

	private void updateStatuses() {
		final boolean isSyncing = databaseService != null && databaseService.isSyncing();
		startProgress(!isSyncing && inInitializing);
		if(recentTime == null) {
			recentDate.setText(R.string.sync_date_none);
			clearRecentDateButton.setEnabled(false);
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(recentTime);
			recentDate.setText(getString(R.string.sync_date_format,
					calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
					calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)
					));
			clearRecentDateButton.setEnabled(!isSyncing && !inInitializing);
		}
		startSyncButton.setText(isSyncing ? R.string.sync_doing : R.string.sync_button_start);
		if(userName != null && userName.length() > 0) {
			loginName.setText(userName);
			loginButton.setText(R.string.sync_button_logout);
			startSyncButton.setEnabled(!isSyncing && !inInitializing && this.databaseService != null);
		} else {
			if(userName != null) {
				loginName.setText(R.string.sync_not_login);
			}
			loginButton.setText(R.string.sync_button_login);
			startSyncButton.setEnabled(false);
		}
		loginButton.setEnabled(!isSyncing && !inInitializing);
	}

	private void voidAllButtons() {
		clearRecentDateButton.setEnabled(false);
		loginButton.setEnabled(false);
		startSyncButton.setEnabled(false);
	}

	private void onClearRecentDate() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().remove(PreferenceKey.SYNC_RECENT_DATE).commit();
		recentTime = null;
		updateStatuses();
	}

	private void onStartSync() {
		if(databaseService == null || !databaseService.isEnabled()) {
			Toast.makeText(this, R.string.sync_not_ready, Toast.LENGTH_LONG).show();
			return;
		}
		if(databaseService.prepareSync(recentTime != null ? (int)(recentTime.getTime() / 1000) : 0)) {
			updateStatuses();
		} else {
			Toast.makeText(this, R.string.sync_busy, Toast.LENGTH_LONG).show();
		}
	}

	private void startProgress(boolean start) {
		progress.setVisibility(start ? View.VISIBLE : View.INVISIBLE);
	}

	private void setWebViewMode(boolean set) {
		webView.setVisibility(set ? View.VISIBLE : View.INVISIBLE);
		container.setVisibility(set ? View.INVISIBLE : View.VISIBLE);
	}

	private void logout() {
		switch(authState) {
		case STATE_BEGIN_LOGIN:
		case STATE_BEGIN_LOGOUT:
		case STATE_REQUEST_USER:
			return;
		}
		authState = STATE_BEGIN_LOGOUT;
		voidAllButtons();
		HttpClientForSync.logout(this);
		webView.loadUrl(logoutUrl);
	}

	private void login() {
		switch(authState) {
		case STATE_BEGIN_LOGIN:
		case STATE_BEGIN_LOGOUT:
		case STATE_REQUEST_USER:
			return;
		}
		authState = STATE_BEGIN_LOGIN;
		voidAllButtons();
		webView.loadUrl(syncUrl);
	}

	@Override
	public void onClick(View v) {
		if(v == startSyncButton) {
			onStartSync();
		} else if(v == loginButton) {
			voidAllButtons();
			if(userName != null && userName.length() > 0) {
				logout();
			} else {
				login();
			}
		} else if(v == clearRecentDateButton) {
			onClearRecentDate();
		}
	}

	private static class GetNameHandler extends AsyncHttpResponseHandler {
		private WeakReference<SyncActivity> activity;
		HttpClientForSync client;
		public GetNameHandler(SyncActivity activity, HttpClientForSync client) {
			this.activity = new WeakReference<SyncActivity>(activity);
			this.client = client;
		}
		@Override
		public void onSuccess(String content) {
			SyncActivity self = activity.get();
			if(self == null) {
				return;
			} else if(client.getLogin()) {
				self.onGotUserName(content);
			} else {
				self.onFailureUserName(false);
			}
		}
		@Override
		public void onFailure(Throwable e, String content) {
			SyncActivity self = activity.get();
			if(self != null) {
				self.onFailureUserName(true);
			}
		}
	}

	private static class SyncWebViewClient extends WebViewClient {
		WeakReference<SyncActivity> activity;
		boolean postForLogin;
		public SyncWebViewClient(SyncActivity activity) {
			this.activity = new WeakReference<SyncActivity>(activity);
		}
		@Override
		public void onPageStarted(WebView webView, String url, Bitmap favicon) {
			SyncActivity self = activity.get();
			if(self == null) {
				webView.stopLoading();
				return;
			}
			self.setWebViewMode(false);
			switch(self.authState) {
			case STATE_BEGIN_LOGIN:
			case STATE_BEGIN_LOGIN_TWITTER:
				if(url.contains("https://twitter.com/")) {
					self.authState = STATE_BEGIN_LOGIN_TWITTER;
				} else {
					self.authState = STATE_BEGIN_LOGIN;
				}
				self.startProgress(true);
				break;
			case STATE_BEGIN_LOGOUT:
				break;
			default:
				webView.stopLoading();
				return;
			}
			postForLogin = url.equals(loginUrl);
		}
		@Override
		public void onPageFinished(WebView webView, String url) {
			SyncActivity self = activity.get();
			if(self == null) {
				return;
			}
			CookieManager cookieManager = CookieManager.getInstance();
			HttpClientForSync.overrideCookies(self, cookieManager.getCookie(url));
			self.startProgress(false);
			final boolean loggedIn = HttpClientForSync.getLogin(self);
			switch(self.authState) {
			case STATE_BEGIN_LOGIN:
			case STATE_BEGIN_LOGIN_TWITTER:
				if(loggedIn) {
					self.authState = STATE_AUTHED;
					self.setWebViewMode(false);
					self.requestUserName();
				} else if(postForLogin) {
					self.startProgress(true);
					webView.loadUrl(syncUrl);
				} else {
					self.setWebViewMode(true);
				}
				break;
			case STATE_BEGIN_LOGOUT:
				if(!loggedIn) {
					self.authState = STATE_NOT_AUTHED;
					self.setWebViewMode(false);
					if(self.userName == null || self.userName.length() > 0) {
						self.userName = "";
						self.updateStatuses();
					}
				}
				break;
			}
		}
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			SyncActivity self = activity.get();
			if(self == null) {
				return true;
			};
			switch(self.authState) {
			case STATE_BEGIN_LOGIN:
				if(url.indexOf(twitterOAuthUrl) == 0) {
					return false;
				} else if(url.indexOf(twitterOAuthUrl1_1) == 0) {
					return false;
				} else if(url.equals(loginUrl)) {
					return false;
				} else if(url.indexOf(syncUrl) == 0) {
					return false;
				}
				break;
			case STATE_BEGIN_LOGIN_TWITTER:
				if(url.indexOf(twitterOAuthUrl) == 0) {
					return false;
				} else if(url.indexOf(twitterOAuthUrl1_1) == 0) {
					return false;
				} else if(url.indexOf(loginUrl) == 0) {
					return false;
				}
				break;
			case STATE_BEGIN_LOGOUT:
				return false;
			default:
				return true;
			}
			Uri uri = Uri.parse(url);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			self.startActivity(intent);
			return true;
		}
	}

	@Override
	public void onDatabaseUpdated(Station station, int sequence) {
		;
	}

	@Override
	public void onDatabaseConnected(DatabaseService service) {
		this.databaseService = service;
		updateStatuses();
	}

	@Override
	public void onDatabaseDisconnected() {
		this.databaseService = null;
	}

	@Override
	public void onSyncFinish(int code) {
		if (code == R.string.sync_done) {
			recentTime = new Date(PreferenceManager.getDefaultSharedPreferences(this).getLong(PreferenceKey.SYNC_RECENT_DATE, 0));
		} else if (code == R.string.sync_login_failed) {
			logout();
		}
		if(isActive) {
			Toast.makeText(SyncActivity.this, code, Toast.LENGTH_LONG).show();
			updateStatuses();
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
		}
	}
}
