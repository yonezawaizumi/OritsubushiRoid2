package com.wsf_lp.android;

import java.lang.ref.WeakReference;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class Geocoder {
	public static final int OK = 0;
	public static final int ZERO_RESULTS = 1;
	public static final int REQUEST_DENIED = 2;
	public static final int NETWORK_ERROR = 3;
	public static final int FATAL_ERROR = 4;

	public static interface OnResultListener {
		public void onGeocoderAddressSelect(String place, LatLng latLng);
		public void onGeocoderAddressNotFound();
		public void onGeocoderError(int reason);
		public void onGeocoderCancel();
	}
	
	private WeakReference<Fragment> mFragment;
	private final int SELECT_DIALOG_TITLE_ID;
	private String[] mPlaces;
	private LatLngF[] mLocations;
	
	public Geocoder(Fragment fragment, int selectDialogTitleId) {
		SELECT_DIALOG_TITLE_ID = selectDialogTitleId;
		if(!fragment.getRetainInstance()) {
			throw new IllegalStateException("fragment " + fragment.getClass().getCanonicalName() + " instance isn't retained");
		} else if(!(fragment instanceof OnResultListener)) {
			throw new IllegalStateException("fragment " + fragment.getClass().getCanonicalName() + " instance don't implements OnResultListener");
		}
		mFragment = new WeakReference<Fragment>(fragment);
	}
	
	public void request(String place) {
		final String url = "https://maps.google.com/maps/api/geocode/json";
		//?address=%@&sensor=false&language=%@";
		final AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		params.put("address", place);
		params.put("sensor", "false");
		params.put("region", "jp");
		params.put("language", Locale.getDefault().getLanguage());
		client.get(url, params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject json) {
				Fragment fragment = mFragment.get();
				if(fragment == null) {
					return;
				}
				OnResultListener listener = (OnResultListener)fragment;
				try {
					String status = json.getString("status");
					if(status.equals("OK")) {
						switch(parseResults(json.getJSONArray("results"))) {
						case 0:
							listener.onGeocoderAddressNotFound();
							break;
						case 1:
							listener.onGeocoderAddressSelect(mPlaces[0], mLocations[0].getLatLng());
							break;
						default:
							SelectDialogFragment.newInstance(Geocoder.this, fragment)
								.show(fragment.getActivity().getSupportFragmentManager(), Geocoder.class.getCanonicalName());
							break;
						}
					} else if(status.equals("ZERO_RESULTS")) {
						listener.onGeocoderAddressNotFound();
					} else if(status.equals("REQUEST_DENIED")) {
						listener.onGeocoderError(REQUEST_DENIED);
					} else {
						listener.onGeocoderError(FATAL_ERROR);
					}
				} catch (JSONException e) {
					e.printStackTrace();
					listener.onGeocoderError(FATAL_ERROR);
				}
			}
			@Override
			public void onFailure(Throwable e, String content) {
				OnResultListener listener = (OnResultListener)mFragment.get();
				if(listener != null) {
					listener.onGeocoderError(NETWORK_ERROR);
				}
			}
			@Override
			public void onFailure(Throwable e, JSONArray errorResponse) {
				OnResultListener listener = (OnResultListener)mFragment.get();
				if(listener != null) {
					listener.onGeocoderError(NETWORK_ERROR);
				}
			}
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				OnResultListener listener = (OnResultListener)mFragment.get();
				if(listener != null) {
					listener.onGeocoderError(NETWORK_ERROR);
				}
			}
		});
	}

	private int parseResults(JSONArray locations) throws JSONException {
		final int length = locations.length();
		if(length > 0) {
			mPlaces = new String[length];
			this.mLocations = new LatLngF[length];
			for(int index = 0; index < length; ++index) {
				JSONObject result = locations.getJSONObject(index);
				mPlaces[index] = result.getString("formatted_address");
				result = result.getJSONObject("geometry").getJSONObject("location");
				this.mLocations[index] = new LatLngF(result.getDouble("lat"), result.getDouble("lng"));
			}
		} else {
			mPlaces = null;
			this.mLocations = null;
		}
		return length;
	}

	public static class LatLngF implements Parcelable {
		float lat;
		float lng;
		public LatLngF() {}
		public LatLngF(double latD, double lngD) {
			lat = (float)latD;
			lng = (float)lngD;
		}
		private LatLngF(Parcel in) {
			lat = in.readFloat();
			lng = in.readFloat();
		}
		public LatLng getLatLng() {
			return new LatLng(lat, lng);
		}
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeFloat(lat);
			dest.writeFloat(lng);
		}
		public static final Parcelable.Creator<LatLngF> CREATOR = new Parcelable.Creator<LatLngF>() {
			@Override
			public LatLngF createFromParcel(Parcel source) {
				return new LatLngF(source);
			}
			@Override
			public LatLngF[] newArray(int size) {
				return new LatLngF[size];
			}
		};
	}
	
	public static class SelectDialogFragment extends DialogFragment {
		public static final String STATE_TITLE_ID = "titleId";
		public static final String STATE_PLACES = "mPlaces";
		public static final String STATE_LOCATIONS = "mLocations";

		private static SelectDialogFragment newInstance(Geocoder geocoder, Fragment parentFragment) {
			SelectDialogFragment fragment = new SelectDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putInt(STATE_TITLE_ID, geocoder.SELECT_DIALOG_TITLE_ID);
			bundle.putStringArray(STATE_PLACES, geocoder.mPlaces);
			bundle.putParcelableArray(STATE_LOCATIONS, geocoder.mLocations);
			fragment.setArguments(bundle);
			fragment.setTargetFragment(parentFragment, 0);
			return fragment;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle arguments = getArguments();
			FragmentActivity activity = getActivity();
			return (new AlertDialog.Builder(activity))
				.setTitle(getString(arguments.getInt(STATE_TITLE_ID)))
				.setItems(arguments.getStringArray(STATE_PLACES), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Bundle arguments = getArguments();
						((OnResultListener)getTargetFragment()).onGeocoderAddressSelect(
								arguments.getStringArray(STATE_PLACES)[which],
								((LatLngF[])arguments.getParcelableArray(STATE_LOCATIONS))[which].getLatLng()
						);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
		}
		
		@Override
		public void onCancel(DialogInterface dialogInterface) {
			((OnResultListener)getTargetFragment()).onGeocoderCancel();
		}
	}

}
