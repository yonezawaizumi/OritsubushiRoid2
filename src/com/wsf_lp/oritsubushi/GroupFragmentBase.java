package com.wsf_lp.oritsubushi;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Station;

public abstract class GroupFragmentBase extends Fragment implements FragmentManager.OnBackStackChangedListener {
	public static final String STATE_NUM_PANELS = "numPanels";
	public static final String STATE_STATION = "station";
	public static final String STATE_HEADER_GROUP = "group";
	public static final String STACK_FIRST = "first";

	private int numPanels;
	private Station station;

	public abstract static class PanelFragment extends DBAccessFragmentBase
			implements View.OnClickListener, ListView.OnItemClickListener {
		protected TextView title;
		protected TextView description;
		protected Button mapFilterButton;
		protected ListView listView;
		protected View wrapper;
		protected CellAdapter cellAdapter;
		protected Group headerGroup;
		protected ArrayList<Group> groups = new ArrayList<Group>();
		protected long recentRequestSequence = Long.MIN_VALUE;
		protected long recentRequestLimit = Long.MAX_VALUE;
		protected long recentHeaderRequestSequence = Long.MIN_VALUE;
		protected long recentHeaderRequestLimit = Long.MAX_VALUE;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			Activity activity = getActivity();
			TypedValue value = new TypedValue();
			activity.getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
			View panelView = inflater.inflate(R.layout.list, container, false);
			panelView.setBackgroundResource(value.resourceId);
			title = (TextView)panelView.findViewById(R.id.title);
			description = (TextView)panelView.findViewById(R.id.description);
			mapFilterButton = (Button)panelView.findViewById(R.id.list_button_map_filter);
			listView = (ListView)panelView.findViewById(R.id.list);
			wrapper = panelView.findViewById(R.id.wrapper);
			wrapper.setBackgroundResource(value.resourceId);
			wrapper.setVisibility(View.VISIBLE);
			mapFilterButton.setVisibility(getMapFilterButtonVisibility());
			mapFilterButton.setOnClickListener(this);
			cellAdapter = new CellAdapter(groups, activity);
			listView.setAdapter(cellAdapter);
			listView.setOnItemClickListener(this);

			return panelView;
		}

		@Override
		public void onClick(View v) {
			onMapFilterButtonClicked();
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			GroupFragmentBase groupFragment = (GroupFragmentBase)getParentFragment();
			Group targetGroup = groups.get(position);
			Station station = getStatinFronGroup(targetGroup);
			if(station != null) {
				groupFragment.showVerbose(station);
			} else {
				groupFragment.showChild(targetGroup);
			}
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			headerGroup = savedInstanceState != null ? (Group)savedInstanceState.getParcelable(STATE_HEADER_GROUP) : null;
			if(headerGroup != null) {
				headerGroup = (Group)getArguments().get(STATE_HEADER_GROUP);
			}
			updateHeaderText();
		}

		public abstract int getMapFilterButtonVisibility();
		public abstract void onMapFilterButtonClicked();
		public abstract Station getStatinFronGroup(Group group);
		public abstract void reload();
		protected abstract void onQueryFinished(String methodName, Object result);
		protected abstract void onQueryForHeaderFinished(String methodName, Object result);

		public void updateHeaderText() {
			Resources resources = getResources();
			title.setText(headerGroup.getHeaderTitle(resources));
			description.setText(headerGroup.getDescription(resources));
		}

		protected void callDatabaseForGroups(String methodName, Object... args) {
			recentRequestSequence = callDatabase(methodName, args);
			recentRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
		}
		protected void callDatabaseForHeader(String methodName, Object... args) {
			recentHeaderRequestSequence = callDatabase(methodName, args);
			recentHeaderRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
		}
		

		@Override
		protected void onQueryFinished(String methodName, Object result, long sequence) {
			if(recentRequestSequence == sequence) {
				onQueryFinished(methodName, result);
			} else if(recentHeaderRequestSequence == sequence) {
				onQueryForHeaderFinished(methodName, result);
			}
		}

		@Override
		protected void onStationUpdated(Station station) {
			
		}
		
		@Override
		protected void onDatabaseUpdated() {
			
		}
	}

	protected abstract Class<? extends PanelFragment>[] getFragmentClasses();
	
	protected static PanelFragment newPanelFragnemt(Class<? extends PanelFragment> clazz, Group headerGroup) {
		try {
			PanelFragment fragment = clazz.newInstance();
			if(headerGroup != null) {
				Bundle bundle = new Bundle();
				bundle.putParcelable(STATE_HEADER_GROUP, headerGroup);
				fragment.setArguments(bundle);
			}
			return fragment;
		} catch (java.lang.InstantiationException e) {
			e.printStackTrace();
			throw new IllegalStateException("failed to create " + clazz.getCanonicalName());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new IllegalStateException("failed to create " + clazz.getCanonicalName());
		}
	}

	protected int getFragmentPosition(PanelFragment fragment) {
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		for(int position = fragmentClasses.length - 1; position >= 0; --position) {
			if(fragmentClasses[position].isInstance(fragment)) {
				return position;
			}
		}
		throw new IllegalStateException("bad fragment " + fragment.getClass().getCanonicalName());
	}
	
	private PanelFragment addFragment(int index, PanelFragment previousFragment, FragmentManager manager, boolean hasNormalAnimation, Group headerGroup) {
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		if(previousFragment == null) {
			previousFragment = (PanelFragment)manager.findFragmentByTag(fragmentClasses[index - 1].getCanonicalName());
		}
		PanelFragment fragment = newPanelFragnemt(fragmentClasses[index], headerGroup);
		manager.beginTransaction()
			.setCustomAnimations(hasNormalAnimation ? R.anim.slide_in_right : R.anim.none, R.anim.none, R.anim.none, R.anim.slide_out_right)
			.add(fragment, fragmentClasses[index].getCanonicalName())
			.hide(previousFragment)
			.addToBackStack(index == 1 ? STACK_FIRST : null)
			.commit();
		return fragment;
	}
	
	private void addStationFragment() {
		FragmentManager manager = getChildFragmentManager();
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		Fragment previousFragment = manager.findFragmentByTag(fragmentClasses[numPanels - 1].getCanonicalName());
		StationFragment fragment = new StationFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(StationFragment.STATE_STATION, station);
		fragment.setArguments(bundle);
		manager.beginTransaction()
			.setCustomAnimations(R.anim.slide_in_right, R.anim.none, R.anim.none, R.anim.slide_out_right)
			.add(fragment, StationFragment.class.getCanonicalName())
			.hide(previousFragment)
			.addToBackStack(null)
			.commit();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getChildFragmentManager().addOnBackStackChangedListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		//for debug
		if(fragmentClasses == null) {
			numPanels = 0;
			return;
		}
		
		numPanels = savedInstanceState != null ? savedInstanceState.getInt(STATE_NUM_PANELS) : 0;
		if(numPanels <= 0 || fragmentClasses.length <= numPanels) {
			numPanels = 1;
		}
		int panelIndex = 0;
		FragmentManager manager = getChildFragmentManager();
		PanelFragment fragment = newPanelFragnemt(fragmentClasses[panelIndex], null);
		manager.beginTransaction()
			.add(R.id.group_flipper, fragment, fragmentClasses[panelIndex].getCanonicalName())
			.commit();
		while(++panelIndex < numPanels) {
			fragment = addFragment(panelIndex, fragment, manager, false, null);
		}
		station = savedInstanceState != null ? (Station)savedInstanceState.getParcelable(STATE_STATION) : null;
		if(station != null) {
			addStationFragment();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_NUM_PANELS, numPanels);
		outState.putParcelable(STATE_STATION, station);
	}
	
	@Override
	public void onBackStackChanged() {
		FragmentManager manager = getChildFragmentManager();
		((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(manager.getBackStackEntryCount() > 0);
		if(station != null && manager.findFragmentByTag(StationFragment.class.getCanonicalName()) == null) {
			station = null;
		}
	}
	
	public void showVerbose(Station station) {
		this.station = station;
		addStationFragment();
	}
	
	public void showChild(Group group) {
		addFragment(++numPanels, null, getChildFragmentManager(), true, group);
	}

	public void updateSubListText(PanelFragment panelFragment) {
		int position = getFragmentPosition(panelFragment);
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		if(position < fragmentClasses.length - 1) {
			++position;
			FragmentManager manager = getChildFragmentManager();
			PanelFragment subFragment = (PanelFragment)manager.findFragmentByTag(fragmentClasses[position].getCanonicalName());
			if(subFragment != null) {
				subFragment.updateHeaderText();
			}
		}
	}
}
