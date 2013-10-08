package com.wsf_lp.android;

import java.lang.ref.WeakReference;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class GeocoderDialogUtil {
	public static final int OK = 0;
	public static final int ZERO_RESULTS = 1;
	public static final int REQUEST_DENIED = 2;
	public static final int NETWORK_ERROR = 3;
	public static final int FATAL_ERROR = 4;

	public static interface OnResultListener {
		public void onGeocoderAddressSelect(GeocoderDialogUtil dialogUtil, String title, int lat, int lng);
		public void onGeocoderAddressNotFound(GeocoderDialogUtil dialogUtil);
		public void onGeocoderError(GeocoderDialogUtil dialogUtil, int reason);
		public void onGeocoderCancel(GeocoderDialogUtil dialogUtil);
	}

	private static class LatLng {
		public int lat;
		public int lng;
		public LatLng(double lat, double lng) {
			this.lat = (int)(lat * 1000000);
			this.lng = (int)(lng * 1000000);
		}
	}
	private String[] items;
	private LatLng[] results;

	private final WeakReference<Context> context;
	private final int titleId;
	private final int cancelButtonId;
	private String location;
	private OnResultListener listener;

	public String getLocation() { return location; }
	public GeocoderDialogUtil setLocation(String location) {
		this.location = location;
		return this;
	}

	public OnResultListener getListener() { return listener; }
	public GeocoderDialogUtil setListener(OnResultListener listener) {
		this.listener = listener;
		return this;
	}

	public GeocoderDialogUtil(Context context, int titleId, int cancelButtonId) {
		this.context = new WeakReference<Context>(context);
		this.titleId = titleId;
		this.cancelButtonId = cancelButtonId;
	}

	public void request() {
		final String url = "http://maps.google.com/maps/api/geocode/json";
		//?address=%@&sensor=false&language=%@";
		final AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		params.put("address", getLocation());
		params.put("sensor", "false");
		params.put("region", "jp");
		params.put("language", Locale.getDefault().getLanguage());
		client.get(url, params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject json) {
				try {
					String status = json.getString("status");
					if(status.equals("OK")) {
						switch(parseResults(json.getJSONArray("results"))) {
						case 0:
							listener.onGeocoderAddressNotFound(GeocoderDialogUtil.this);
							break;
						case 1:
							callListener(0);
							break;
						default:
							showDialog();
							break;
						}
					} else if(status.equals("ZERO_RESULTS")) {
						listener.onGeocoderAddressNotFound(GeocoderDialogUtil.this);
					} else if(status.equals("REQUEST_DENIED")) {
						listener.onGeocoderError(GeocoderDialogUtil.this, REQUEST_DENIED);
					} else {
						listener.onGeocoderError(GeocoderDialogUtil.this, FATAL_ERROR);
					}
				} catch (JSONException e) {
					e.printStackTrace();
					listener.onGeocoderError(GeocoderDialogUtil.this, FATAL_ERROR);
				}
			}
			@Override
			public void onFailure(Throwable e, String content) {
				listener.onGeocoderError(GeocoderDialogUtil.this, NETWORK_ERROR);
			}
			@Override
			public void onFailure(Throwable e, JSONArray errorResponse) {
				listener.onGeocoderError(GeocoderDialogUtil.this, NETWORK_ERROR);
			}
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				listener.onGeocoderError(GeocoderDialogUtil.this, NETWORK_ERROR);
			}
		});
	}

	private int parseResults(JSONArray results) throws JSONException {
		final int length = results.length();
		if(length > 0) {
			items = new String[length];
			this.results = new LatLng[length];
			for(int index = 0; index < length; ++index) {
				JSONObject result = results.getJSONObject(index);
				items[index] = result.getString("formatted_address");
				result = result.getJSONObject("geometry").getJSONObject("location");
				this.results[index] = new LatLng(result.getDouble("lat"), result.getDouble("lng"));
			}
		} else {
			items = null;
			this.results = null;
		}
		return length;
	}

	private void callListener(int index) {
		listener.onGeocoderAddressSelect(this, items[index], results[index].lat, results[index].lng);
	}

	private void showDialog() {
		Context context = this.context.get();
		if(context == null) {
			return;
		}
		new AlertDialog.Builder(context)
			.setTitle(titleId)
			.setItems(items, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					callListener(which);
				}
			})
			.setNegativeButton(context.getString(cancelButtonId), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					listener.onGeocoderCancel(GeocoderDialogUtil.this);
				}
			})
			.create()
			.show();
	}
}
