package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Station;

public abstract class GroupFragmentBase extends DBAccessFragmentBase implements OnBackPressedListener {
	protected static final String STATE_TAG = "group";

	protected static final long RETRY_MSEC = 5000;

	private ViewFlipper flipper;
	private Animation inAnimation;
	private Animation outAnimation;
	private Animation nonAnimation;

	protected static class Panel {
		public TextView title;
		public TextView description;
		public Button mapFilterButton;
		public ListView listView;
		public View wrapper;
		public CellAdapter cellAdapter;
		public Group headerGroup;
		public ArrayList<Group> groups = new ArrayList<Group>();
		public long recentRequestSequence;
		public long recentRequestLimit = Long.MAX_VALUE;
		public long recentHeaderRequestSequence;
		public long recentHeaderRequestLimit = Long.MAX_VALUE;
	}
	private Panel[] panels;

	//for retain fragment
	private int currentPanelIndex;

	public int getCurrentPanelIndex() { return currentPanelIndex; }

	protected Group getHeaderGroup(int panelIndex) { return panels[panelIndex].headerGroup; }
	protected void setHeaderGroup(int panelIndex, Group headerGroup) {
		panels[panelIndex].headerGroup = headerGroup;
	}
	protected ArrayList<Group> getGroups(int panelIndex) { return panels[panelIndex].groups; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int panelCount = getPanelCount();
		panels = new Panel[panelCount];
		for(int index = 0; index < panelCount; ++index) {
			panels[index] = new Panel();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = getActivity();
		View view = inflater.inflate(R.layout.group, container, false);

		flipper = (ViewFlipper)view.findViewById(R.id.group_flipper);
		inAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_in_right);
		outAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_right);
		nonAnimation = AnimationUtils.loadAnimation(activity, R.anim.none);
		TypedValue value = new TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
		int panelCount = getPanelCount();
		for(int index = 0; index < panelCount; ++index) {
			Panel panel = panels[index];
			final int panelIndex = index;	//for closure
			View panelView = inflater.inflate(R.layout.list, null);
			panelView.setBackgroundResource(value.resourceId);
			panel.title = (TextView)panelView.findViewById(R.id.title);
			panel.description = (TextView)panelView.findViewById(R.id.description);
			panel.mapFilterButton = (Button)panelView.findViewById(R.id.list_button_map_filter);
			panel.listView = (ListView)panelView.findViewById(R.id.list);
			panel.wrapper = panelView.findViewById(R.id.wrapper);
			panel.wrapper.setBackgroundResource(value.resourceId);
			panel.wrapper.setVisibility(View.VISIBLE);
			panel.mapFilterButton.setVisibility(getMapFilterButtonVisibility(panelIndex));
			panel.mapFilterButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onClickMapFilterButton(panelIndex);
				}
			});
			panel.cellAdapter = new CellAdapter(panel.groups, activity);
			panel.listView.setAdapter(panel.cellAdapter);
			panel.listView.setOnItemClickListener(new ListView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if(getCurrentPanelIndex() == panelIndex && onSelectGroup(panelIndex, panels[panelIndex].groups.get(position))) {
						updateText(panelIndex + 1);
						panels[panelIndex + 1].wrapper.setVisibility(View.VISIBLE);
						flipper.setInAnimation(inAnimation);
						flipper.setOutAnimation(nonAnimation);
						++currentPanelIndex;
						flipper.showNext();
					}
				}
			});
			flipper.addView(panelView);
			panels[index] = panel;
		}

		return view;
	}

	protected boolean restoreInstance(Bundle savedInstanceState) {
		boolean recreated = panels[0].groups.isEmpty();
		if(savedInstanceState == null && recreated) {
			return false;
		}
		if(recreated) {
			Parcelable[] parcelableArray = savedInstanceState.getParcelableArray(STATE_TAG);
			if(parcelableArray == null) {
				return false;
			}
			int displayedChild = parcelableArray.length - 1;
			if(displayedChild < 0) {
				return false;
			}
			currentPanelIndex = displayedChild;
			for(int index = displayedChild; index >= 0; --index) {
				panels[index].headerGroup = (Group)parcelableArray[index];
				if(isDatabaseReady()) {
					reloadHeaderGroup(index);
					loadGroup(index);
				}
			}
		} else {
			//TODO: restore listview position
			for(int index = 0; index <= currentPanelIndex; ++index) {
				panels[index].cellAdapter.notifyDataSetChanged();
				panels[index].wrapper.setVisibility(View.GONE);
			}
		}
		flipper.setInAnimation(null);
		flipper.setOutAnimation(null);
		flipper.setDisplayedChild(getCurrentPanelIndex());
		return true;
		//return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(!restoreInstance(savedInstanceState)) {
			panels[0].headerGroup = createDefaultTopHeaderGroup();
		}
		updateAllTexts();
	}

	@Override
	public void onResume() {
		super.onResume();
		((MainActivity)getActivity()).registerOnBackPressedListener(this);
	}

	@Override
	public void onPause() {
		((MainActivity)getActivity()).unregisterOnBackPressedListener(this);
		super.onPause();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		int count = getCurrentPanelIndex() + 1;
		Group[] headerGroups = new Group[count];
		for(int index = 0; index < count; ++index) {
			//TODO: save listview position for not calling onCreate()
			headerGroups[index] = panels[index].headerGroup;
		}
		outState.putParcelableArray(STATE_TAG, headerGroups);
	}

	@Override
	public boolean onBackPressed(MainActivity activity) {
		if(flipper != null && flipper.isShown() && getCurrentPanelIndex() > 0) {
			flipper.setInAnimation(nonAnimation);
			flipper.setOutAnimation(outAnimation);
			--currentPanelIndex;
			flipper.showPrevious();
			return true;
		} else {
			return false;
		}
	}

	protected boolean update1(int panelIndex, Group group) {
		Panel panel = panels[panelIndex];
		int code = group.getCode();
		if(code != 0) {
			for(int index = 0; index < panel.groups.size(); ++index) {
				if(panel.groups.get(index).getCode() == code) {
					panel.groups.set(index, group);
					return true;
				}
			}
			return false;
		} else {
			panel.headerGroup.setTotal(group.getTotal());
			panel.headerGroup.setCompletions(group.getCompletions());
			return false;
		}
	}

	protected void updateAll(int panelIndex, List<Group> newGroups) {
		Panel panel = panels[panelIndex];
		panel.groups.clear();
		panel.groups.ensureCapacity(newGroups.size());
		panel.groups.addAll(newGroups);
	}

	protected abstract int getPanelCount();
	protected abstract int getMapFilterButtonVisibility(int panelIndex);
	protected abstract void onClickMapFilterButton(int panelIndex);
	protected abstract boolean onSelectGroup(int panelIndex, Group group);
	protected abstract boolean updateGroups(int panelIndex, String methodName, Object result);
	protected abstract boolean updateHeader(int panelIndex, String methodName, Object result);
	protected abstract void reloadHeaderGroup(int panelIndex);
	protected abstract void loadGroup(int panelIndex);
	protected abstract Group createDefaultTopHeaderGroup();
	protected abstract int updateStation(Station station);

	protected void reset() {
		flipper.setInAnimation(null);
		flipper.setOutAnimation(null);
		currentPanelIndex = 0;
		flipper.setDisplayedChild(0);
		loadGroup(0);
	}

	/* must be override */
	protected void updateText(int panelIndex) {
		if(!isAlive()) {
			return;
		}
		Resources resources = getResources();
		Panel panel = panels[panelIndex];
		panel.title.setText(panel.headerGroup.getHeaderTitle(resources));
		panel.description.setText(panel.headerGroup.getDescription(resources));
	}

	protected void updateAllTexts() {
		int count = getCurrentPanelIndex() + 1;
		for(int index = 0; index < count; ++index) {
			updateText(index);
		}
	}

	protected boolean processReloadHeaderGroup(int headerPanelIndex, Group group) {
		int currentIndex = getCurrentPanelIndex();
		if(headerPanelIndex > currentIndex) {
			return false;
		}
		getHeaderGroup(headerPanelIndex).copyStatistics(group);
		return headerPanelIndex > 0 && update1(headerPanelIndex - 1, group);
	}

	protected void callDatabase(int panelIndex, String methodName, Object... args) {
		panels[panelIndex].recentRequestSequence = callDatabase(methodName, args);
		panels[panelIndex].recentRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
	}
	protected void callDatabaseForHeader(int panelIndex, String methodName, Object... args) {
		panels[panelIndex].recentHeaderRequestSequence = callDatabase(methodName, args);
		panels[panelIndex].recentHeaderRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
	}

	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		int count = panels.length;
		int updateIndex = -1;
		for(int index = 0; index < count; ++index) {
			Panel panel = panels[index];
			if(panel.recentRequestSequence == sequence) {
				panel.recentRequestLimit = Long.MAX_VALUE;
				if(updateGroups(index, methodName, result)) {
					updateIndex = index;
				}
				break;
			} else if(panel.recentHeaderRequestSequence == sequence) {
				panel.recentHeaderRequestLimit = Long.MAX_VALUE;
				if(updateHeader(index, methodName, result)) {
					updateIndex = index - 1;
				}
				break;
			}
		}
		if(updateIndex >= 0) {
			Panel panel = panels[updateIndex];
			panel.wrapper.setVisibility(View.GONE);
			panel.cellAdapter.notifyDataSetChanged();
			updateAllTexts();
		}
	}

	@Override
	protected void onDatabaseConnected(boolean forceReload, List<Station> updatedStations) {
		for(int index = getCurrentPanelIndex(); index >= 0; --index) {
			reloadHeaderGroup(index);
			loadGroup(index);
		}
	}

	@Override
	protected void onDatabaseUpdated() {
		reset();
	}

	@Override
	protected void onStationUpdated(Station station) {
		int panelIndex = updateStation(station);
		if(panelIndex >= 0) {
			panels[panelIndex].cellAdapter.notifyDataSetChanged();
		}
	}

/*	public void updateAllTexts() {
		FragmentManager manager = getChildFragmentManager();
		for(Class<? extends PanelFragment> fragmentClass : getFragmentClasses()) {
			PanelFragment fragment = (PanelFragment)manager.findFragmentByTag(fragmentClass.getCanonicalName());
			if(fragment != null) {
				fragment.updateHeaderText();
			}
		}
	}*/
}
