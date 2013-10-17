package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;

import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.OperatorTypes;
import com.wsf_lp.mapapp.data.Station;

public class OperatorTypeContainerFragment extends GroupFragmentBase {

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

	public static class OperatorTypeFragment extends PanelFragment {

		@Override
		public int getMapFilterButtonVisibility() {
			return View.GONE;
		}

		@Override
		public void onMapFilterButtonClicked() {
		}

		@Override
		public Station getStationFromGroup(Group group) {
			return null;
		}

		@Override
		public Group getGroupFromStation(Station station) {
			int operatorType = station.getOperator().getOperatorType();
			for(Group group : groups) {
				if(group.getCode() == operatorType) {
					return group;
				}
			}
			return null;
		}

		@Override
		protected void onQueryFinished(String methodName, Object result) {
			@SuppressWarnings("unchecked")
			Pair<Group, List<Group>> pair = (Pair<Group, List<Group>>)result;
			headerGroup.copyStatistics(pair.first);
			groups.clear();
			groups.ensureCapacity(pair.second.size());
			String[] operatorTypes = OperatorTypes.getOperatorTypes(getResources());
			for(Group group : pair.second) {
				final String title = operatorTypes[group.getCode()];
				group.setTitle(title);
				group.setHeaderTitle(title);
				groups.add(group);
			}
		}

		@Override
		protected boolean onQueryForHeaderFinished(String methodName, Object result) {
			headerGroup.copyStatistics((Group)result);
			return false;
		}

		@Override
		protected void reloadHeaderGroup() {
			callDatabaseForHeader(Database.MethodName.GET_TOTAL);
		}

		@Override
		protected void loadGroups() {
			callDatabaseForGroups(Database.MethodName.GET_OPERATOR_TYPES);
		}

	}

	private static final ArrayList<Class<? extends PanelFragment>> CLASSES = new ArrayList<Class<? extends PanelFragment>>();
	static {
		CLASSES.add(OperatorTypeFragment.class);
	}

	@Override
	protected ArrayList<Class<? extends PanelFragment>> getFragmentClasses() { return CLASSES; }
}
