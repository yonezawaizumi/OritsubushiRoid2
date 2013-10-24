package com.wsf_lp.oritsubushi;

import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.wsf_lp.android.Prefs;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

public class StationFragment extends DBAccessFragmentBase {

	public static final String STATE_STATION = "station";

	private TextView title;
	private TextView yomi;
	private TextView lines;
	private TextView address;
	private TextView completionDate;
	private TextView memo;
	private Button editDate;
	private Button completionOnToday;
	private Button editMemo;

	private Station station;
	public Station getStation() { return station; }

	public static void show(Fragment currentFragment, Station station) {
		FragmentManager manager = ((FragmentActivity)currentFragment.getActivity()).getSupportFragmentManager();
		StationFragment fragment = new StationFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(STATE_STATION, station);
		fragment.setArguments(bundle);
		manager.beginTransaction()
			.setCustomAnimations(R.anim.slide_in_right, R.anim.none, R.anim.none, R.anim.slide_out_right)
			.hide(currentFragment)
			.add(MainActivity.CONTENT_VIEW_ID, fragment, currentFragment.getClass().getCanonicalName() + '@' + StationFragment.class.getCanonicalName())
			.addToBackStack(null)
			.commit();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.verbose, container, false);
		title = (TextView)view.findViewById(R.id.verbose_title);
		yomi = (TextView)view.findViewById(R.id.verbose_yomi);
		lines = (TextView)view.findViewById(R.id.verbose_lines);
		address = (TextView)view.findViewById(R.id.verbose_address);
		completionDate = (TextView)view.findViewById(R.id.verbose_comp_date);
		memo = (TextView)view.findViewById(R.id.verbose_memo);
		editDate = (Button)view.findViewById(R.id.verbose_button_edit_date);
		completionOnToday = (Button)view.findViewById(R.id.verbose_button_comp_today);
		editMemo = (Button)view.findViewById(R.id.verbose_button_edit_memo);

		editDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditDateDialogFragment.newInstance(StationFragment.this).show(getFragmentManager(), EditDateDialogFragment.class.getCanonicalName());
			}
		});
		completionOnToday.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateCompletionDate(Station.calcCompletionDateInt(new Date()));
			}
		});
		editMemo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditMemoDialogFragment.newInstance(StationFragment.this).show(getChildFragmentManager(), EditMemoDialogFragment.class.getCanonicalName());
			}
		});
		view.findViewById(R.id.verbose_button_wikipedia).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://ja.m.wikipedia.org/wiki/" + Uri.encode(station.getWiki()))));
			}
		});
		view.findViewById(R.id.verbose_button_move_to).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().sendBroadcast(new OritsubushiNotificationIntent().setMapMoveTo(station));
			}
		});

        return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(station == null && savedInstanceState != null) {
			station = savedInstanceState.getParcelable(STATE_STATION);
		}
		if(station == null) {
			station = getArguments().getParcelable(STATE_STATION);
		}
		loadStation();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_STATION, station);
	}

	@Override
	protected void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations) {
		//TODO:パラメータ使え
		if(station == null/* || forceReload || updateStations.find(station)*/) {
			station = (Station)getArguments().getParcelable(STATE_STATION);
			if(isEnabled) {
				callDatabase(Database.MethodName.RELOAD_STATION, station);
			}
		} else {
			loadStation();
		}
	}

	@Override
	public void onDatabaseDisconnected(boolean dummy) {
		super.onDatabaseDisconnected();
		editDate.setEnabled(false);
		editMemo.setEnabled(false);
	}

	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		updateText();
	}

	@Override
	protected void onDatabaseUpdated(boolean isFirst) {
		callDatabase(Database.MethodName.RELOAD_STATION, station);
	}

	@Override
	protected void onStationUpdated(Station station) {
		if(this.station.equals(station)) {
			this.station = station;
			updateText();
		}
	}

	protected void loadStation() {
		if(!station.isReadyToCreateSubtitle() && isDatabaseEnabled()) {
			callDatabase(Database.MethodName.LOAD_LINES, station);
		}
		updateText();
	}

	protected void updateText() {
		if(!isAlive() || station.isExpired()) {
			return;
		}
		title.setText(station.getTitle());
		yomi.setText(station.getYomi());
		lines.setText(station.getSubtitle());
		address.setText(Prefs.getValue(station.getPref(), getResources()) + station.getAddress());
		completionDate.setText(station.getCompletionDateString(getResources()));
		memo.setText(station.getMemo());
		completionOnToday.setEnabled(!station.isCompleted());
		boolean enableEdit = station.isReadyToCreateSubtitle();
		editDate.setEnabled(enableEdit);
		editMemo.setEnabled(enableEdit);
	}

	public void updateCompletionDate(int newDate) {
		if(station.getCompletionDate() != newDate) {
			station.setCompletionDate(newDate);
			callDatabase(Database.MethodName.UPDATE_COMPLETION, station);
			updateText();
		}
	}

	public void updateMemo(String newMemo) {
		if(!station.getMemo().equals(newMemo)) {
			station.setMemo(newMemo);
			callDatabase(Database.MethodName.UPDATE_MEMO, station);
			updateText();
		}
	}
}
