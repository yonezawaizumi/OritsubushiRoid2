package com.wsf_lp.android;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wsf_lp.oritsubushi.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import cz.msebera.android.httpclient.Header;

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
	private LatLngS[] mLocations;
	
	public Geocoder(Fragment fragment, int selectDialogTitleId) {
		SELECT_DIALOG_TITLE_ID = selectDialogTitleId;
		if(!fragment.getRetainInstance()) {
			throw new IllegalStateException("fragment " + fragment.getClass().getCanonicalName() + " instance isn't retained");
		} else if(!(fragment instanceof OnResultListener)) {
			throw new IllegalStateException("fragment " + fragment.getClass().getCanonicalName() + " instance don't implements OnResultListener");
		}
		mFragment = new WeakReference<Fragment>(fragment);
	}
	
	public void request(String place, Context context) {
		final String url = "https://oritsubushi.net/oritsubushi/yahoomap.php";
		//?k=%@
		final AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();
		params.put("k", place);
		client.get(url, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {
				String response;
				try {
					response = new String(bytes, "utf-8");
				} catch (java.io.UnsupportedEncodingException e) {
					response = new String(bytes);
				}
				Fragment fragment = mFragment.get();
				if(fragment == null) {
					return;
				}
				OnResultListener listener = (OnResultListener)fragment;
				switch(parseResults(response.split("\n"))) {
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
			}
			@Override
			public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
				OnResultListener listener = (OnResultListener)mFragment.get();
				if(listener != null) {
					listener.onGeocoderError(NETWORK_ERROR);
				}
			}
		});
	}

	private int parseResults(String[] locations) {
		String[][] locs = null;
		if (locations.length > 0) {
			List<String[]> ls = new ArrayList<String[]>(locations.length);
			for (String loc : locations) {
				String[] chunks = loc.split("\t");
				if (chunks.length == 3) {
					ls.add(chunks);
				}
			}
			if (!ls.isEmpty()) {
				locs = ls.toArray(new String[0][0]);
			}
		}
		if(locs != null && locs.length > 0) {
			mPlaces = new String[locs.length];
			this.mLocations = new LatLngS[locs.length];
			for(int index = 0; index < locs.length; ++index) {
				mPlaces[index] = locs[index][0];
				this.mLocations[index] = new LatLngS(locs[index][1], locs[index][2]);
			}
			return locs.length;
		} else {
			mPlaces = null;
			this.mLocations = null;
			return 0;
		}
	}

	public static class LatLngS implements Parcelable {
		float lat;
		float lng;
		public LatLngS() {}
		public LatLngS(int latI, int lngI) {
			lat = latI / 1000000.0f;
			lng = lngI / 1000000.0f;
		}
		public LatLngS(String latS, String lngS) {
			try {
				lat = Integer.parseInt(latS) / 1000000.0f;
				lng = Integer.parseInt(lngS) / 1000000.0f;
			} catch (NumberFormatException e) {
				lat = 0.0f;
				lng = 0.0f;
			}
		}
		private LatLngS(Parcel in) {
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
		public static final Parcelable.Creator<LatLngS> CREATOR = new Parcelable.Creator<LatLngS>() {
			@Override
			public LatLngS createFromParcel(Parcel source) {
				return new LatLngS(source);
			}
			@Override
			public LatLngS[] newArray(int size) {
				return new LatLngS[size];
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
								((LatLngS[])arguments.getParcelableArray(STATE_LOCATIONS))[which].getLatLng()
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
