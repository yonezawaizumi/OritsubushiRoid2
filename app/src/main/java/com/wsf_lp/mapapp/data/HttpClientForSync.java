package com.wsf_lp.mapapp.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;
import com.wsf_lp.android.CookieSyncManager;
import com.wsf_lp.oritsubushi.R;

@SuppressWarnings("deprecation")
public class HttpClientForSync extends AsyncHttpClient {
	private static final long COOKIE_PSEUDO_EXPIRED_MSEC = 365L * 24 * 3600 * 1000;
	public static final String CHECK_COOKIE_KEY = "geeklog";
	public static final String[] COOKIE_KEYS = {
		CHECK_COOKIE_KEY,
		"password",
		"gl_session"
	};
	private static final String COOKIE_KEYS_CONCAT;
	static {
		StringBuilder builder = new StringBuilder(" ");
		for(String key : COOKIE_KEYS) {
			builder.append(key);
			builder.append(' ');
		}
		COOKIE_KEYS_CONCAT = builder.toString();
	}

	private static String hostUrl;
	private static String domain;

	private PersistentCookieStore cookieStore;

	public static void initialize(Context context) {
		if(hostUrl == null) {
			hostUrl = context.getString(R.string.host_url);
			domain = context.getString(R.string.host_domain);
		}
	}

	public HttpClientForSync(Context context) {
		initialize(context);
		cookieStore = new PersistentCookieStore(context);
		setCookieStore(cookieStore);
	}

	private static void logout(PersistentCookieStore cookieStore) {
		CookieSyncManager.sync();
		List<Cookie> cookies = cookieStore.getCookies();
		if(cookies == null) {
			return;
		}
		ArrayList<Cookie> replacedCookies = new ArrayList<Cookie>(COOKIE_KEYS.length);
		for(Cookie cookie : cookies) {
			if(cookie.getDomain().equals(domain) && COOKIE_KEYS_CONCAT.indexOf(cookie.getName()) > 0) {
				BasicClientCookie newCookie = new BasicClientCookie(cookie.getName(), "");
				newCookie.setDomain(cookie.getDomain());
				newCookie.setExpiryDate(cookie.getExpiryDate());
				newCookie.setPath(cookie.getPath());
				newCookie.setSecure(cookie.isSecure());
				replacedCookies.add(newCookie);
			}
		}
		for(Cookie cookie : replacedCookies) {
			cookieStore.addCookie(cookie);
		}
	}

	public static void logout(Context context) {
		initialize(context);
		logout(new PersistentCookieStore(context));
	}

	public void logout() {
		logout(cookieStore);
	}

	private static boolean getLogin(PersistentCookieStore cookieStore) {
		List<cz.msebera.android.httpclient.cookie.Cookie> cookies = cookieStore.getCookies();
		if(cookies == null) {
			return false;
		}
		for(cz.msebera.android.httpclient.cookie.Cookie cookie : cookies) {
			if(cookie.getDomain().equals(domain) && cookie.getName().equals(CHECK_COOKIE_KEY)) {
				try {
					return Integer.parseInt(cookie.getValue()) > 0;
				} catch(NumberFormatException e) {
					return false;
				}
			}
		}
		return false;
	}

	public static boolean getLogin(Context context) {
		initialize(context);
		return getLogin(new PersistentCookieStore(context));
	}

	public boolean getLogin() {
		return getLogin(cookieStore);
	}

	private static void overrideCookies(PersistentCookieStore cookieStore, String cookiesString) {
		cookieStore.clear();
		if(cookiesString == null) {
			return;
		}
		for(String cookieString : cookiesString.split(";")) {
			String[] cookieKeyAndValue = cookieString.split("=", 2);
			BasicClientCookie cookie = new BasicClientCookie(cookieKeyAndValue[0].trim(), cookieKeyAndValue[1].trim());
			cookie.setDomain(domain);
			cookie.setPath("/");
			cookie.setExpiryDate(new Date(System.currentTimeMillis() + COOKIE_PSEUDO_EXPIRED_MSEC));
			cookieStore.addCookie(cookie);
		}
	}

	public static void overrideCookies(Context context, String cookiesString) {
		initialize(context);
		overrideCookies(new PersistentCookieStore(context), cookiesString);
	}

	public void overrideCookies(String cookiesString) {
		overrideCookies(cookieStore, cookiesString);
	}
}
