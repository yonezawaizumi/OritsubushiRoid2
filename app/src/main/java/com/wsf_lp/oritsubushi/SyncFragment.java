package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wsf_lp.mapapp.data.HttpClientForSync;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;
import com.wsf_lp.android.CookieSyncManager;

@SuppressWarnings("deprecation")
public class SyncFragment extends DBAccessFragmentBase
		implements View.OnClickListener, OnBackPressedListener,	OritsubushiBroadcastReceiver.SyncListener {

	private View mProgress;
	private View mContainer;
	private TextView mRecentDate;
	private TextView mLoginName;
	private Button mClearRecentDateButton;
	private Button mLoginButton;
	private Button mStartSyncButton;
	private WebView mWebView;

	private String mUserName;
	private Date mRecentTime;

	private static final int STATE_NOT_INITIALIZED = 0;
	private static final int STATE_NOT_AUTHED = 1;
	private static final int STATE_AUTHED = 2;
	private static final int STATE_BEGIN_LOGIN = 3;
	private static final int STATE_BEGIN_LOGIN_TWITTER = 4;
	private static final int STATE_BEGIN_LOGOUT = 5;
	private static final int STATE_REQUEST_USER = 6;

	private int mAuthState;

	private static String syncUrl;
	private static String loginUrl;
	private static String logoutUrl;
	private static String twitterOAuthUrl;
	private static String twitterOAuthUrl1_1;

	@Override
	protected IntentFilter getIntentFilter() {
		return OritsubushiNotificationIntent.getSyncFinishIntentFilter();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(syncUrl == null) {
			syncUrl = getString(R.string.sync_url);
			loginUrl = getString(R.string.users_url);
			logoutUrl = getString(R.string.logout_url);
			twitterOAuthUrl = getString(R.string.twitter_oauth_url);
			twitterOAuthUrl1_1 = getString(R.string.twitter_oauth_url_1_1);
		}

		CookieSyncManager.createInstance(getActivity().getApplicationContext());
		CookieSyncManager.startSync();

		mAuthState = STATE_NOT_INITIALIZED;
		requestUserName();
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.sync, container, false);

		mProgress = view.findViewById(R.id.progress);
		mContainer = view.findViewById(R.id.container);
		mRecentDate = (TextView)view.findViewById(R.id.sync_recent_date);
		mLoginName = (TextView)view.findViewById(R.id.sync_login_user);
		mClearRecentDateButton = (Button)view.findViewById(R.id.button_clear_recent_date);
		mClearRecentDateButton.setOnClickListener(this);
		mStartSyncButton = (Button)view.findViewById(R.id.button_start_sync);
		mStartSyncButton.setOnClickListener(this);
		mLoginButton = (Button)view.findViewById(R.id.button_logout);
		mLoginButton.setOnClickListener(this);

		mWebView = (WebView)view.findViewById(R.id.web_view);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new SyncWebViewClient(this));
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

		if(syncUrl == null) {
			syncUrl = getString(R.string.sync_url);
			loginUrl = getString(R.string.users_url);
			logoutUrl = getString(R.string.logout_url);
			twitterOAuthUrl = getString(R.string.twitter_oauth_url);
			twitterOAuthUrl1_1 = getString(R.string.twitter_oauth_url_1_1);
		}

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mWebView.restoreState(savedInstanceState);

        startProgress(true);
        voidAllButtons();
	}

	@Override
	public void onStart() {
		super.onStart();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final long time = preferences.getLong(PreferenceKey.SYNC_RECENT_DATE, -1);
		mRecentTime = time >= 0 ? new Date(time) : null;

		if(mAuthState == STATE_NOT_INITIALIZED) {
			requestUserName();
		} else {
			updateStatuses();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		((MainActivity)getActivity()).registerOnBackPressedListener(this);
	}

	@Override
	public void onPause() {
		((MainActivity)getActivity()).unregisterOnBackPressedListener(this);
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mWebView != null) {
			mWebView.saveState(outState);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mProgress = null;
		mContainer = null;
		mRecentDate = null;
		mLoginName = null;
		mClearRecentDateButton = null;
		mLoginButton = null;
		mStartSyncButton = null;
		mWebView = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		CookieSyncManager.stopSync();
	}

	@Override
	public boolean onBackPressed(MainActivity activity) {
		if(mContainer.getVisibility() != View.VISIBLE) {
			if(mProgress.getVisibility() != View.VISIBLE) {
				mAuthState = STATE_NOT_AUTHED;
				setWebViewMode(false);
				updateStatuses();
			}
			return true;
		} else {
			return false;
		}
	}

	private void requestUserName() {
		switch(mAuthState) {
		case STATE_BEGIN_LOGIN:
		case STATE_BEGIN_LOGOUT:
		case STATE_REQUEST_USER:
			return;
		}
		mUserName = null;
		mAuthState = STATE_REQUEST_USER;
		updateStatuses();
		startProgress(true);
		Context context = getActivity().getApplicationContext();
		HttpClientForSync client = new HttpClientForSync(context);
		client.get(context, syncUrl, new GetNameHandler(this, client));
	}

	public void onGotUserName(String userName) {
		mAuthState = STATE_AUTHED;
		if(!userName.equals(mUserName)) {
			mUserName = userName;
			updateStatuses();
		}
	}

	public void onFailureUserName(boolean networkError) {
		if(networkError) {
			mAuthState = STATE_NOT_INITIALIZED;
			if(mContainer != null) {
				Toast.makeText(getActivity(), R.string.sync_network_error, Toast.LENGTH_LONG).show();
				updateStatuses();
			}
		} else {
			mAuthState = STATE_NOT_AUTHED;
			logout();
		}
	}

	private void updateStatuses() {
		if(mContainer == null) {
			return;
		}
		final boolean isSyncing = isDatabaseEnabled() && getDatabaseService().isSyncing();
		startProgress(isSyncing || !isDatabaseEnabled());
		if(mRecentTime == null) {
			mRecentDate.setText(R.string.sync_date_none);
			mClearRecentDateButton.setEnabled(false);
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(mRecentTime);
			mRecentDate.setText(getString(R.string.sync_date_format,
					calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
					calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)
					));
			mClearRecentDateButton.setEnabled(!isSyncing/* && !mIsInitializing*/);
		}
		mStartSyncButton.setText(isSyncing ? R.string.sync_doing : R.string.sync_button_start);
		if(mUserName == null) {
			mLoginName.setText(R.string.sync_unknown_login);
			mLoginButton.setText(mAuthState == STATE_NOT_INITIALIZED ? R.string.sync_button_retry_login : R.string.sync_button_login_initializing);
			mStartSyncButton.setEnabled(false);
		} else if(mUserName.length() == 0) {
			mLoginName.setText(R.string.sync_not_login);
			mLoginButton.setText(R.string.sync_button_login);
			mStartSyncButton.setEnabled(false);
		} else {
			mLoginName.setText(mUserName);
			mLoginButton.setText(R.string.sync_button_logout);
			mStartSyncButton.setEnabled(!isSyncing && isDatabaseEnabled());
		}
		mLoginButton.setEnabled(!isSyncing);
	}

	private void voidAllButtons() {
		if(mContainer != null) {
			mClearRecentDateButton.setEnabled(false);
			mLoginButton.setEnabled(false);
			mStartSyncButton.setEnabled(false);
		}
	}

	private void onClearRecentDate() {
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().remove(PreferenceKey.SYNC_RECENT_DATE).apply();
		mRecentTime = null;
		updateStatuses();
	}

	private void onStartSync() {
		if(!isDatabaseEnabled()) {
			Toast.makeText(getActivity(), R.string.sync_not_ready, Toast.LENGTH_LONG).show();
			return;
		}
		if(getDatabaseService().prepareSync(mRecentTime != null ? (int)(mRecentTime.getTime() / 1000) : 0)) {
			updateStatuses();
		} else {
			Toast.makeText(getActivity(), R.string.sync_busy, Toast.LENGTH_LONG).show();
		}
	}

	private void startProgress(boolean start) {
		if(mProgress != null) {
			mProgress.setVisibility(start ? View.VISIBLE : View.INVISIBLE);
		}
	}

	private void setWebViewMode(boolean set) {
		if(mContainer != null) {
			mWebView.setVisibility(set ? View.VISIBLE : View.INVISIBLE);
			mContainer.setVisibility(set ? View.INVISIBLE : View.VISIBLE);
		}
	}

	private void logout() {
		switch(mAuthState) {
		case STATE_BEGIN_LOGIN:
		case STATE_BEGIN_LOGOUT:
		case STATE_REQUEST_USER:
			return;
		}
		mAuthState = STATE_BEGIN_LOGOUT;
		voidAllButtons();
		HttpClientForSync.logout(getActivity().getApplicationContext());
		if(mWebView != null) {
			mWebView.loadUrl(logoutUrl);
		}
	}

	private void login() {
		switch(mAuthState) {
		case STATE_BEGIN_LOGIN:
		case STATE_BEGIN_LOGOUT:
		case STATE_REQUEST_USER:
			return;
		}
		mAuthState = STATE_BEGIN_LOGIN;
		voidAllButtons();
		mWebView.loadUrl(syncUrl);
	}

	@Override
	public void onClick(View v) {
		if(v == mStartSyncButton) {
			onStartSync();
		} else if(v == mLoginButton) {
			voidAllButtons();
			if(mAuthState == STATE_NOT_INITIALIZED) {
				requestUserName();
			} else if(mUserName != null && mUserName.length() > 0) {
				logout();
			} else {
				login();
			}
		} else if(v == mClearRecentDateButton) {
			onClearRecentDate();
		}
	}

	private static class GetNameHandler extends AsyncHttpResponseHandler {
		private WeakReference<SyncFragment> mFragment;
		HttpClientForSync mClient;
		public GetNameHandler(SyncFragment fragment, HttpClientForSync client) {
			mFragment = new WeakReference<SyncFragment>(fragment);
			mClient = client;
		}
		@Override
		public void onSuccess(String content) {
			SyncFragment fragment = mFragment.get();
			if(fragment == null) {
				return;
			} else if(mClient.getLogin()) {
				fragment.onGotUserName(content);
			} else {
				fragment.onFailureUserName(false);
			}
		}
		@Override
		public void onFailure(Throwable e, String content) {
			SyncFragment fragment = mFragment.get();
			if(fragment != null) {
				fragment.onFailureUserName(true);
			}
		}
	}

	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {}

	@Override
	protected void onDatabaseUpdated(boolean isFirst) {
		if(isFirst) {
			updateStatuses();
		}
	}

	@Override
	protected void onStationUpdated(Station station) {}

	@Override
	protected void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations) {

	}

	private static class SyncWebViewClient extends WebViewClient {
		WeakReference<SyncFragment> mFragment;
		Context mContext;
		boolean mPostForLogin;
		boolean mDenied;
		public SyncWebViewClient(SyncFragment fragment) {
			mFragment = new WeakReference<SyncFragment>(fragment);
			mContext = fragment.getActivity().getApplicationContext();
		}
		@Override
		public void onPageStarted(WebView webView, String url, Bitmap favicon) {
			SyncFragment fragment = mFragment.get();
			if(fragment == null) {
				webView.stopLoading();
				return;
			}
			fragment.setWebViewMode(false);
			switch(fragment.mAuthState) {
			case STATE_BEGIN_LOGIN:
			case STATE_BEGIN_LOGIN_TWITTER:
				if(url.contains("https://twitter.com/")) {
					fragment.mAuthState = STATE_BEGIN_LOGIN_TWITTER;
				} else {
					fragment.mAuthState = STATE_BEGIN_LOGIN;
				}
				fragment.startProgress(true);
				break;
			case STATE_BEGIN_LOGOUT:
				break;
			default:
				webView.stopLoading();
				return;
			}
			mPostForLogin = url.equals(loginUrl);
		}
		@Override
		public void onPageFinished(WebView webView, String url) {
			SyncFragment self = mFragment.get();
			if(self == null) {
				return;
			}
			CookieManager cookieManager = CookieManager.getInstance();
			HttpClientForSync.overrideCookies(mContext, cookieManager.getCookie(url));
			self.startProgress(false);
			final boolean loggedIn = HttpClientForSync.getLogin(mContext);
			switch(self.mAuthState) {
			case STATE_BEGIN_LOGIN:
			case STATE_BEGIN_LOGIN_TWITTER:
				if(loggedIn) {
					self.mAuthState = STATE_AUTHED;
					self.setWebViewMode(false);
					self.requestUserName();
				} else if(mPostForLogin) {
					self.startProgress(true);
					webView.loadUrl(syncUrl);
				} else if(mDenied) {
					mDenied = false;
					self.startProgress(true);
					webView.loadUrl(syncUrl);
				} else {
					self.setWebViewMode(true);
				}
				break;
			case STATE_BEGIN_LOGOUT:
				if(!loggedIn) {
					self.mAuthState = STATE_NOT_AUTHED;
					self.setWebViewMode(false);
					if(self.mUserName == null || self.mUserName.length() > 0) {
						self.mUserName = "";
						self.updateStatuses();
					}
				}
				break;
			}
		}
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			SyncFragment self = mFragment.get();
			if(self == null) {
				return true;
			};
			switch(self.mAuthState) {
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
					if(url.indexOf("&denied=") > 0) {
						mDenied = true;
					}
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
	public void onSyncFinish(int code) {
		if (code == R.string.sync_done) {
			Context context = getActivity();
			long epoch;
			if(context != null) {
				epoch = PreferenceManager.getDefaultSharedPreferences(context).getLong(PreferenceKey.SYNC_RECENT_DATE, 0);
			} else {
				epoch = 0;
			}
			mRecentTime = new Date(epoch);
		} else if (code == R.string.sync_login_failed) {
			logout();
		}
		if(mContainer != null) {
			Context context = getActivity();
			Toast.makeText(context, code, Toast.LENGTH_LONG).show();
			updateStatuses();
			((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
		}
	}

}
