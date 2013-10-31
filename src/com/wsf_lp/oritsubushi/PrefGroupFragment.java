package com.wsf_lp.oritsubushi;


import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;

import com.wsf_lp.android.Prefs;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Station;

public class PrefGroupFragment extends GroupFragmentBase {
	private static final int PREF_INDEX = 0;
	private static final int STATION_INDEX = 1;
	private static final int PANEL_COUNT = 2;

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
			return resources.getString(R.string.station_group_description_format, station.getCompletionDateShortString(resources), station.getAddress());
		}
		@Override
		public Drawable getStatusIcon(Resources resource) {
			return station.getStatusIcon(resource);
		}
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
	protected void onClickMapFilterButton(int panelIndex) {
	}

	@Override
	protected boolean onSelectGroup(int panelIndex, Group group) {
		switch(panelIndex) {
		case PREF_INDEX:
			setHeaderGroup(STATION_INDEX, group);
			loadGroup(STATION_INDEX);
			return true;
		case STATION_INDEX:
			StationFragment.show(this, ((StationGroup)group).getStation(), false);
			return false;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean updateGroups(int panelIndex, String MethodName, Object result) {
		switch(panelIndex) {
		case PREF_INDEX:
		{
			Pair<Group, List<Group>> pair = (Pair<Group, List<Group>>)result;
			getHeaderGroup(PREF_INDEX).copyStatistics(pair.first);
			ArrayList<Group> groups = getGroups(PREF_INDEX);
			groups.clear();
			groups.ensureCapacity(pair.second.size());
			String[] prefs = Prefs.getValues(getResources());
			for(Group group : pair.second) {
				final String title = prefs[group.getCode()];
				group.setTitle(title);
				group.setHeaderTitle(title);
				groups.add(group);
			}
			return true;
		}
		case STATION_INDEX:
		{
			List<Station> newStations = (List<Station>)result;
			ArrayList<Group> groups = getGroups(STATION_INDEX);
			groups.clear();
			groups.ensureCapacity(newStations.size());
			for(Station station : newStations) {
				groups.add(new StationGroup(station));
			}
			return true;
		}
		}
		return false;
	}

	@Override
	protected boolean updateHeader(int panelIndex, String MethodName, Object result) {
		switch(panelIndex) {
		case PREF_INDEX:
			getHeaderGroup(PREF_INDEX).copyStatistics((Group)result);
			return false;
		case STATION_INDEX:
			return processReloadHeaderGroup(STATION_INDEX, (Group)result);
		}
		return false;
	}

	@Override
	protected void reloadHeaderGroup(int panelIndex) {
		switch(panelIndex) {
		case PREF_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.GET_TOTAL);
			break;
		case STATION_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_PREF, getHeaderGroup(STATION_INDEX));
			break;
		}
	}

	@Override
	protected void loadGroup(int panelIndex) {
		switch(panelIndex) {
		case PREF_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_PREFS);
			break;
		case STATION_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_STATIONS_BY_PREF, getHeaderGroup(STATION_INDEX).getCode());
			break;
		}
	}

	@Override
	protected Group createDefaultTopHeaderGroup() {
		Group headerGroup = new Group();
		headerGroup.setCode(0);
		headerGroup.setTitle(getString(R.string.pref));
		return headerGroup;
	}

	@Override
	protected int updateStation(Station station) {
		final int panelIndex = getCurrentPanelIndex();
		int result = -1;
		switch(panelIndex) {
		case STATION_INDEX:
			final int stationCode = station.getCode();
			for(Group stationGroup : getGroups(STATION_INDEX)) {
				if(stationGroup.getCode() == stationCode) {
					((StationGroup)stationGroup).setStation(station);
					callDatabaseForHeader(STATION_INDEX, Database.MethodName.RELOAD_PREF, getHeaderGroup(STATION_INDEX));
					result = STATION_INDEX;
					break;
				}
			}
			//FALLTHROUGH
		case PREF_INDEX:
			loadGroup(PREF_INDEX);
		}
		return result;
	}

}
