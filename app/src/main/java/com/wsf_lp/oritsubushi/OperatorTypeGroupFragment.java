package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;

import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Operator;
import com.wsf_lp.mapapp.data.OperatorTypes;
import com.wsf_lp.mapapp.data.Station;


public class OperatorTypeGroupFragment extends GroupFragmentBase {
	private static final int OPERATOR_TYPE_INDEX = 0;
	private static final int OPERATOR_INDEX = 1;
	private static final int LINE_INDEX = 2;
	private static final int STATION_INDEX = 3;
	private static final int PANEL_COUNT = 4;

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
			return station.getCompletionDateString(resources);
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
		case OPERATOR_TYPE_INDEX:
			setHeaderGroup(OPERATOR_INDEX, group);
			loadGroup(OPERATOR_INDEX);
			return true;
		case OPERATOR_INDEX:
			setHeaderGroup(LINE_INDEX, group);
			loadGroup(LINE_INDEX);
			return true;
		case LINE_INDEX:
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
	protected boolean updateGroups(int panelIndex, String methodName, Object result) {
		switch(panelIndex) {
		case OPERATOR_TYPE_INDEX:
		{
			Pair<Group, List<Group>> pair = (Pair<Group, List<Group>>)result;
			getHeaderGroup(OPERATOR_TYPE_INDEX).copyStatistics(pair.first);
			ArrayList<Group> groups = getGroups(OPERATOR_TYPE_INDEX);
			groups.clear();
			groups.ensureCapacity(pair.second.size());
			String[] operatorTypes = OperatorTypes.getOperatorTypes(getResources());
			for(Group group : pair.second) {
				String title = operatorTypes[group.getCode()];
				group.setTitle(title);
				group.setHeaderTitle(title);
				groups.add(group);
			}
			return true;
		}
		case OPERATOR_INDEX:
			if(Database.MethodName.GET_OPERATORS.equals(methodName)) {
				Pair<Boolean, List<Group>> pair = (Pair<Boolean, List<Group>>)result;
				updateAll(OPERATOR_INDEX, pair.second);
				if(!pair.first) {
					callDatabase(OPERATOR_INDEX, Database.MethodName.GET_OPERATORS_STATISTICS, getHeaderGroup(OPERATOR_INDEX).getCode());
				}
			} else if(Database.MethodName.GET_OPERATORS_STATISTICS.equals(methodName)){
				updateAll(OPERATOR_INDEX, (List<Group>)result);
			} else {
				update1(OPERATOR_INDEX, (Group)result);
			}
			return true;
		case LINE_INDEX:
			updateAll(LINE_INDEX, (List<Group>)result);
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
		case OPERATOR_TYPE_INDEX:
			getHeaderGroup(OPERATOR_TYPE_INDEX).copyStatistics((Group)result);
			return false;
		case OPERATOR_INDEX:
			return processReloadHeaderGroup(OPERATOR_INDEX, (Group)result);
		case LINE_INDEX:
			return processReloadHeaderGroup(LINE_INDEX, (Group)result);
		case STATION_INDEX:
			return processReloadHeaderGroup(STATION_INDEX, (Group)result);
		}
		return false;
	}

	@Override
	protected void reloadHeaderGroup(int panelIndex) {
		switch(panelIndex) {
		case OPERATOR_TYPE_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.GET_TOTAL);
			break;
		case OPERATOR_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_OPERATOR_TYPE, getHeaderGroup(OPERATOR_INDEX));
			break;
		case LINE_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_OPERATOR, getHeaderGroup(LINE_INDEX));
			break;
		case STATION_INDEX:
			callDatabaseForHeader(panelIndex, Database.MethodName.RELOAD_LINE, getHeaderGroup(STATION_INDEX));
			break;
		}
	}

	@Override
	protected void loadGroup(int panelIndex) {
		switch(panelIndex) {
		case OPERATOR_TYPE_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_OPERATOR_TYPES);
			break;
		case OPERATOR_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_OPERATORS, getHeaderGroup(OPERATOR_INDEX).getCode());
			break;
		case LINE_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_LINES, getHeaderGroup(LINE_INDEX).getCode());
			break;
		case STATION_INDEX:
			callDatabase(panelIndex, Database.MethodName.GET_STATIONS_BY_LINE, getHeaderGroup(STATION_INDEX).getCode());
			break;
		}
	}

	@Override
	protected Group createDefaultTopHeaderGroup() {
		Group headerGroup = new Group();
		headerGroup.setCode(0);
		headerGroup.setTitle(getString(R.string.operator_type));
		return headerGroup;
	}

	@Override
	protected int updateStation(Station station) {
		Operator operator = station.getOperator();
		int panelIndex = getCurrentPanelIndex();
		int result = -1;
		boolean found = false;
		switch(panelIndex) {
		case STATION_INDEX:
			for(Group stationGroup : getGroups(STATION_INDEX)) {
				if(stationGroup.getCode() == station.getCode()) {
					((StationGroup)stationGroup).setStation(station);
					result = STATION_INDEX;
					found = true;
					break;
				}
			}
			if(found) {
				callDatabaseForHeader(STATION_INDEX, Database.MethodName.RELOAD_LINE, getHeaderGroup(STATION_INDEX));
			}
			//FALLTHROUGH
		case LINE_INDEX:
			if(found) {
				callDatabaseForHeader(LINE_INDEX, Database.MethodName.RELOAD_OPERATOR, getHeaderGroup(LINE_INDEX));
			} else if(operator.getCode() == getHeaderGroup(LINE_INDEX).getCode()) {
				loadGroup(LINE_INDEX);
				callDatabaseForHeader(LINE_INDEX, Database.MethodName.RELOAD_LINE, getHeaderGroup(LINE_INDEX));
				found = true;
			}
			//FALLTHROUGH
		case OPERATOR_INDEX:
			if(found) {
				callDatabaseForHeader(OPERATOR_INDEX, Database.MethodName.RELOAD_OPERATOR_TYPE, getHeaderGroup(OPERATOR_INDEX));
			} else if(operator.getOperatorType() == getHeaderGroup(OPERATOR_INDEX).getCode()) {
				int operatorCode = operator.getCode();
				for(Group group : getGroups(OPERATOR_INDEX)) {
					if(group.getCode() == operatorCode) {
						callDatabase(OPERATOR_INDEX, Database.MethodName.RELOAD_OPERATOR, group);
						found = true;
						break;
					}
				}
			}
			//FALLTHROUGH
		case OPERATOR_TYPE_INDEX:
			loadGroup(OPERATOR_TYPE_INDEX);
		}
		return result;
	}

}
