package com.wsf_lp.oritsubushi;

import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.wsf_lp.android.Prefs;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Station;

public class StationFragment extends DBAccessFragmentBase {

	public static final String STATE_STATION = "station";

	private TextView mTitle;
	private TextView mYomi;
	private TextView mLines;
	private TextView mAddress;
	private TextView mCompletionDate;
	private TextView mMemo;
	private TextView mUpdatedDate;
	private Button mEditDate;
	private Button mCompletionOnToday;
	private Button mEditMemo;

	private Station mStation;
	public Station getStation() { return mStation; }

	public static void show(Fragment currentFragment, Station station, boolean useScaleAnimation) {
		FragmentManager manager = ((FragmentActivity)currentFragment.getActivity()).getSupportFragmentManager();
		StationFragment fragment = new StationFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(STATE_STATION, station);
		fragment.setArguments(bundle);
		FragmentTransaction transaction = manager.beginTransaction();
		if(useScaleAnimation) {
			transaction.setCustomAnimations(R.anim.station_in, 0, 0, R.anim.station_out);
		} else {
			transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
		}
		transaction.replace(MainActivity.CONTENT_VIEW_ID, fragment, currentFragment.getClass().getCanonicalName() + '@' + StationFragment.class.getCanonicalName())
			.addToBackStack(null)
			.commit();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.verbose, container, false);
		mTitle = (TextView)view.findViewById(R.id.verbose_title);
		mYomi = (TextView)view.findViewById(R.id.verbose_yomi);
		mLines = (TextView)view.findViewById(R.id.verbose_lines);
		mAddress = (TextView)view.findViewById(R.id.verbose_address);
		mCompletionDate = (TextView)view.findViewById(R.id.verbose_comp_date);
		mMemo = (TextView)view.findViewById(R.id.verbose_memo);
		mUpdatedDate = (TextView)view.findViewById(R.id.verbose_updated_date);
		mEditDate = (Button)view.findViewById(R.id.verbose_button_edit_date);
		mCompletionOnToday = (Button)view.findViewById(R.id.verbose_button_comp_today);
		mEditMemo = (Button)view.findViewById(R.id.verbose_button_edit_memo);

		mEditDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditDateDialogFragment.newInstance(StationFragment.this).show(getChildFragmentManager(), EditDateDialogFragment.class.getCanonicalName());
			}
		});
		mCompletionOnToday.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateCompletionDate(Station.calcCompletionDateInt(new Date()));
			}
		});
		mEditMemo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditMemoDialogFragment.newInstance(StationFragment.this).show(getChildFragmentManager(), EditMemoDialogFragment.class.getCanonicalName());
			}
		});
		view.findViewById(R.id.verbose_button_wikipedia).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://ja.m.wikipedia.org/wiki/" + Uri.encode(mStation.getWiki()))));
			}
		});
		view.findViewById(R.id.verbose_button_move_to).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//直接ブロードキャストしないでこれ呼ばないとMapFragmentインスタンスないときに困る
				MapFragment.moveTo(getActivity(), mStation);
			}
		});

        return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(mStation == null && savedInstanceState != null) {
			mStation = savedInstanceState.getParcelable(STATE_STATION);
		}
		if(mStation == null) {
			mStation = getArguments().getParcelable(STATE_STATION);
		}
		loadStation();
	}

	@Override
	public void onStart() {
		super.onStart();
		((MainActivity)getActivity()).enableUpButton(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_STATION, mStation);
	}

	@Override
	public void onStop() {
		super.onStop();
		((MainActivity)getActivity()).enableUpButton(false);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mTitle = null;
		mYomi = null;
		mLines = null;
		mAddress = null;
		mCompletionDate = null;
		mMemo = null;
		mUpdatedDate = null;
		mEditDate = null;
		mCompletionOnToday = null;
		mEditMemo = null;
	}

	@Override
	protected void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations) {
		//TODO:パラメータ使え
		if(mStation == null/* || forceReload || updateStations.find(station)*/) {
			mStation = (Station)getArguments().getParcelable(STATE_STATION);
			if(isEnabled) {
				callDatabase(Database.MethodName.RELOAD_STATION, mStation);
			}
		} else {
			loadStation();
		}
	}

	@Override
	public void onDatabaseDisconnected(boolean dummy) {
		super.onDatabaseDisconnected();
		mEditDate.setEnabled(false);
		mEditMemo.setEnabled(false);
	}

	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		updateText();
	}

	@Override
	protected void onDatabaseUpdated(boolean isFirst) {
		callDatabase(Database.MethodName.RELOAD_STATION, mStation);
	}

	@Override
	protected void onStationUpdated(Station station) {
		if(mStation.equals(station)) {
			mStation = station;
			updateText();
		}
	}

	protected void loadStation() {
		if(!mStation.isReadyToCreateSubtitle() && isDatabaseEnabled()) {
			callDatabase(Database.MethodName.LOAD_LINES, mStation);
		}
		updateText();
	}

	protected void updateText() {
		if(!isAlive() || mStation.isExpired() || mTitle == null) {
			return;
		}
		Resources resources = getResources();
		mTitle.setText(mStation.getTitle());
		mYomi.setText(mStation.getYomi());
		mLines.setText(mStation.getSubtitle());
		mAddress.setText(Prefs.getValue(mStation.getPref(), resources) + mStation.getAddress());
		mCompletionDate.setText(mStation.getCompletionDateString(getResources()));
		mMemo.setText(mStation.getMemo());
		mUpdatedDate.setText(mStation.getUpdatedDate(resources));
		mCompletionOnToday.setEnabled(!mStation.isCompleted());
		boolean enableEdit = mStation.isReadyToCreateSubtitle();
		mEditDate.setEnabled(enableEdit);
		mEditMemo.setEnabled(enableEdit);
	}

	public void updateCompletionDate(int newDate) {
		if(mStation.getCompletionDate() != newDate) {
			mStation.setCompletionDate(newDate);
			callDatabase(Database.MethodName.UPDATE_COMPLETION, mStation);
			updateText();
		}
	}

	public void updateMemo(String newMemo) {
		if(!mStation.getMemo().equals(newMemo)) {
			mStation.setMemo(newMemo);
			callDatabase(Database.MethodName.UPDATE_MEMO, mStation);
			updateText();
		}
	}
}
