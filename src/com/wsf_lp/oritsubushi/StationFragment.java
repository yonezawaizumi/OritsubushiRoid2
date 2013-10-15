package com.wsf_lp.oritsubushi;

import java.util.List;

import android.os.Bundle;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		station = savedInstanceState != null ? (Station)savedInstanceState.getParcelable(STATE_STATION) : null;
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
			}
		});
		//completionOnToday.setOnClickListener(compTodayProc);
		editMemo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		//view.findViewById(R.id.verbose_button_wikipedia).setOnClickListener(wikiProc);
		//view.findViewById(R.id.verbose_button_move_to).setOnClickListener(moveToProc);

        loadStation();

        return view;
	}
	
	@Override
	protected void onDatabaseConnected(boolean forceReload, List<Station> updatedStations) {
		//TODO:パラメータ使え
		if(station == null/* || forceReload || updateStations.find(station)*/) {
			Station station = (Station)getArguments().getParcelable(STATE_STATION);
			callDatabase(Database.MethodName.RELOAD_STATION, station);
		} else {
			loadStation();
		}
	}
	
	@Override
	public void onDatabaseDisconnected() {
		super.onDatabaseDisconnected();
		editDate.setEnabled(false);
		editMemo.setEnabled(false);
	}
	
	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		updateText();
	}

	@Override
	protected void onDatabaseUpdated() {
		if(isDatabaseReady()) {
			callDatabase(Database.MethodName.RELOAD_STATION, station);
		}
	}

	@Override
	protected void onStationUpdated(Station station) {
		this.station = station;
		updateText();
	}

	protected void loadStation() {
		if(!station.isReadyToCreateSubtitle() && isDatabaseReady()) {
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

}
