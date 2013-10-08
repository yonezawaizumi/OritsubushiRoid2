package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import com.wsf_lp.mapapp.data.CompletionDateGroup;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Station;

public class CompletionDateGroupActivity extends GroupActivityBase {
	public static final int ID = R.id.comp_date;
	private static final int YEAR_INDEX = 0;
	private static final int MONTH_INDEX = 1;
	private static final int DAY_INDEX = 2;
	private static final int DEFAULT_STATION_INDEX = 3;
	private static final int PANEL_COUNT = 4;
	//private static final int INCOMP_STATION_INDEX = YEAR_INDEX + 1;
	//private static final int AMBIGUOUS_YEAR_STATION_INDEX = YEAR_INDEX + 1;
	//private static final int AMBIGUOUS_MONTH_STATION_INDEX = MONTH_INDEX + 1;

	//UGLY re-mix-in ..(TOT)
	protected static class StationGroup extends Group {
		private Station station;
		public StationGroup(Station station) {
			this.station = station;
			setCode(station.getCode());
		}
		public StationGroup(Group group) {
			super(group);
			if(group instanceof StationGroup) {
				this.station = ((StationGroup)group).station;
			}
		}
		public Station getStation() { return station; }
		public void setStation(Station station) {
			this.station = station;
		}
		@Override
		public String getTitle(Resources resources) {
			return station.getName();
		}
		@Override
		public String getDescription(Resources resources) {
			return resources.getString(R.string.station_group_comp_date_description_format,
					station.getOperator().getName(),
					station.getAddress());
		}
		@Override
		public Drawable getStatusIcon(Resources resource) {
			return station.getStatusIcon(resource);
		}
	}

	@Override
	public int updateStation(Station station) {
		//以前のステートがわからないためオールリロードするしかない
		reset();
		return -1;
	}

	private void updateStations(int panelIndex, List<Station> newStations) {
		ArrayList<Group> groups = getGroups(panelIndex);
		groups.clear();
		groups.ensureCapacity(newStations.size());
		for(Station station : newStations) {
			groups.add(new StationGroup(station));
		}
	}

	//20130221
	//常にリセットしないと、アクティビティが存在しない間にDB更新されたとき矛盾表示の恐れ
	@Override
	protected boolean restoreInstance(final Bundle savedInstanceState) {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean updateGroups(int panelIndex, String methodName, Object result) {
		switch(panelIndex) {
		case YEAR_INDEX:
			Pair<Group, List<Group>> pair = (Pair<Group, List<Group>>)result;
			getHeaderGroup(YEAR_INDEX).copyStatistics(pair.first);
			updateAll(YEAR_INDEX, pair.second);
			return true;
		case MONTH_INDEX:
			if(Database.MethodName.GET_MONTH.equals(methodName)) {
				updateAll(MONTH_INDEX, (List<Group>)result);
			} else {
				updateStations(panelIndex, (List<Station>)result);
			}
			return true;
		case DAY_INDEX:
			if(Database.MethodName.GET_DAY.equals(methodName)) {
				updateAll(DAY_INDEX, (List<Group>)result);
			} else {
				updateStations(panelIndex, (List<Station>)result);
			}
			return true;
		case DEFAULT_STATION_INDEX:
			updateStations(panelIndex, (List<Station>)result);
			return true;
		}
		return false;
	}

	@Override
	protected boolean updateHeader(int panleIndex, String methodName, Object result) {
		return false;
	}

	@Override
	protected int getIdForMenu() {
		return ID;
	}

	@Override
	protected int getPanelCount() {
		return PANEL_COUNT;
	}

	@Override
	protected int getMapFilterButtonVisibility(int panelIndex) {
		return View.GONE;
	}
	@Override
	protected Group createDefaultTopHeaderGroup() {
		Group headerGroup = new Group();
		headerGroup.setCode(0);
		headerGroup.setTitle(getString(R.string.comp_date));
		return headerGroup;
	}

	@Override
	protected void onClickMapFilterButton(int panelIndex) {
	}

	@Override
	protected boolean onSelectGroup(int panelIndex, Group group) {
		if(group instanceof StationGroup) {
			VerboseActivity.start(this, ((StationGroup)group).getStation());
			return false;
		}
		switch(panelIndex) {
		case YEAR_INDEX:
			setHeaderGroup(MONTH_INDEX, group);
			loadGroup(MONTH_INDEX);
			return true;
		case MONTH_INDEX:
			setHeaderGroup(DAY_INDEX, group);
			loadGroup(DAY_INDEX);
			return true;
		case DAY_INDEX:
			setHeaderGroup(DEFAULT_STATION_INDEX, group);
			loadGroup(DEFAULT_STATION_INDEX);
			return true;
		}
		return false;
	}

	@Override
	protected void loadGroup(int panelIndex) {
		switch(panelIndex) {
		case YEAR_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_YEAR);
			break;
		case MONTH_INDEX:
			callDatabase(panelIndex,
					CompletionDateGroup.nextIsStationPanel(getHeaderGroup(MONTH_INDEX)) ? Database.MethodName.GET_STATIONS_BY_COMP_DATE : Database.MethodName.GET_MONTH,
					getHeaderGroup(MONTH_INDEX));
			break;
		case DAY_INDEX:
			callDatabase(panelIndex,
					CompletionDateGroup.nextIsStationPanel(getHeaderGroup(DAY_INDEX)) ? Database.MethodName.GET_STATIONS_BY_COMP_DATE : Database.MethodName.GET_DAY,
					getHeaderGroup(DAY_INDEX));
			break;
		case DEFAULT_STATION_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_STATIONS_BY_COMP_DATE, getHeaderGroup(DEFAULT_STATION_INDEX));
			break;
		}
	}

	@Override
	protected void reloadHeaderGroup(int panelIndex) {
		switch(panelIndex) {
		case YEAR_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.GET_TOTAL);
			break;
		case MONTH_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_YEAR, getHeaderGroup(MONTH_INDEX));
			break;
		case DAY_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_MONTH, getHeaderGroup(DAY_INDEX));
			break;
		case DEFAULT_STATION_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_DAY, getHeaderGroup(DEFAULT_STATION_INDEX));
			break;
		}
	}

}
