package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;

import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Station;
import com.wsf_lp.mapapp.data.YomiUtils;

public class YomiGroupActivity extends GroupActivityBase {
	public static final int ID = R.id.yomi;
	private static final int YOMI_INDEX = 0;
	private static final int YOMI_2_INDEX = 1;
	private static final int STATION_INDEX = 2;
	private static final int PANEL_COUNT = 3;

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
			return resources.getString(R.string.station_group_yomi_description_format,
					station.getCompletionDateShortString(resources),
					station.getYomi(),
					station.getOperator().getName());
		}
		@Override
		public Drawable getStatusIcon(Resources resource) {
			return station.getStatusIcon(resource);
		}
	}

	@Override
	public int updateStation(Station station) {
		final int panelIndex = getCurrentPanelIndex();
		final String yomi = station.getYomi();
		int result = -1;
		boolean found = false;
		switch(panelIndex) {
		case STATION_INDEX:
			final int stationCode = station.getCode();
			for(Group stationGroup : getGroups(STATION_INDEX)) {
				if(stationGroup.getCode() == stationCode) {
					((StationGroup)stationGroup).setStation(station);
					callDatabaseForHeader(STATION_INDEX, Database.MethodName.RELOAD_YOMI_2, getHeaderGroup(STATION_INDEX));
					result = STATION_INDEX;
					found = true;
					break;
				}
			}
			//FALLTHROUGH
		case YOMI_2_INDEX:
			if(found) {
				callDatabaseForHeader(YOMI_2_INDEX, Database.MethodName.RELOAD_YOMI, getHeaderGroup(YOMI_2_INDEX));
			} else {
				if(getHeaderGroup(YOMI_2_INDEX).getCode() == YomiUtils.getYomi1Code(yomi)) {
					final int yomi2 = YomiUtils.getYomi2Code(yomi);
					for(Group group : getGroups(YOMI_2_INDEX)) {
						if(group.getCode() == yomi2) {
							callDatabase(YOMI_2_INDEX, Database.MethodName.RELOAD_YOMI_2, getHeaderGroup(YOMI_2_INDEX));
							callDatabaseForHeader(STATION_INDEX, Database.MethodName.RELOAD_YOMI, getHeaderGroup(YOMI_2_INDEX));
							break;
						}
					}
				}
			}
			//FALLTHROUGH
		case YOMI_INDEX:
			loadGroup(YOMI_INDEX);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean updateGroups(int panelIndex, String methodName, Object result) {
		switch(panelIndex) {
		case YOMI_INDEX:
		{
			Pair<Group, List<Group>> pair = (Pair<Group, List<Group>>)result;
			getHeaderGroup(YOMI_INDEX).copyStatistics(pair.first);
			updateAll(YOMI_INDEX, pair.second);
			return true;
		}
		case YOMI_2_INDEX:
			if(Database.MethodName.GET_YOMI_2.equals(methodName)) {
				updateAll(YOMI_2_INDEX, (List<Group>)result);
			} else {
				update1(YOMI_2_INDEX, (Group)result);
			}
			return true;
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
	protected boolean updateHeader(int panelIndex, String methodName, Object result) {
		switch(panelIndex) {
		case YOMI_INDEX:
			getHeaderGroup(YOMI_INDEX).copyStatistics((Group)result);
			return false;
		case YOMI_2_INDEX:
			return processReloadHeaderGroup(YOMI_2_INDEX, (Group)result);
		case STATION_INDEX:
			return processReloadHeaderGroup(STATION_INDEX, (Group)result);
		}
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
		headerGroup.setTitle(getString(R.string.yomi));
		return headerGroup;
	}

	@Override
	protected void onClickMapFilterButton(int panelIndex) {
	}

	@Override
	protected boolean onSelectGroup(int panelIndex, Group group) {
		switch(panelIndex) {
		case YOMI_INDEX:
			setHeaderGroup(YOMI_2_INDEX, group);
			loadGroup(YOMI_2_INDEX);
			return true;
		case YOMI_2_INDEX:
			setHeaderGroup(STATION_INDEX, group);
			loadGroup(STATION_INDEX);
			return true;
		case STATION_INDEX:
			VerboseActivity.start(this, ((StationGroup)group).getStation());
			return false;
		}
		return false;
	}

	@Override
	protected void loadGroup(int panelIndex) {
		switch(panelIndex) {
		case YOMI_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_YOMI);
			break;
		case YOMI_2_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_YOMI_2, getHeaderGroup(YOMI_2_INDEX));
			break;
		case STATION_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_STATIONS_BY_YOMI, getHeaderGroup(STATION_INDEX).getTitle(getResources()));
			break;
		}
	}
	@Override
	protected void reloadHeaderGroup(int panelIndex) {
		switch(panelIndex) {
		case YOMI_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.GET_TOTAL);
			break;
		case YOMI_2_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_YOMI, getHeaderGroup(YOMI_2_INDEX));
			break;
		case STATION_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_YOMI_2, getHeaderGroup(STATION_INDEX));
			break;
		}
	}

}
