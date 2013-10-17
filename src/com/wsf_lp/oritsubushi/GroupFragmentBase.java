package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

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

public abstract class GroupFragmentBase extends DBAccessFragmentBase implements FragmentManager.OnBackStackChangedListener, OritsubushiFragmentTraits {
	public static final String STATE_NUM_PANELS = "numPanels";
	public static final String STATE_PANEL_INDEX = "panelIndex";
	public static final String STATE_STATION = "station";
	public static final String STATE_HEADER_GROUP = "group";
	public static final String STACK_FIRST = "first";

	private int numPanels;
	private Station station;

	public abstract static class PanelFragment extends DBAccessFragmentBase
			implements View.OnClickListener, ListView.OnItemClickListener {
		protected int panelIndex;
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
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Bundle arguments = getArguments();
			panelIndex = arguments.getInt(STATE_PANEL_INDEX);
			headerGroup = arguments.getParcelable(STATE_HEADER_GROUP);
		}

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

			if(groups.isEmpty() && isDatabaseReady()) {
				loadGroups();
			}

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
			Station station = getStationFromGroup(targetGroup);
			if(station != null) {
				groupFragment.showVerbose(station);
			} else {
				groupFragment.showChild(targetGroup);
			}
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			updateHeaderText();
		}

		// MapFragmentの検索フィルタボタンの表示可否を返す
		public abstract int getMapFilterButtonVisibility();
		// MapFragmentの検索フィルタボタンがクリックされた
		public abstract void onMapFilterButtonClicked();
		// 指定されたGroupがStationを表している場合にそのStationを返し、それ以外はnull
		public abstract Station getStationFromGroup(Group group);
		// 指定されたStationを含むGroupがヘッダまたはリスト内に存在する場合にそのGroupを返し、それ以外はnull
		// ヘッダに合致する場合はGroupの code が 0 になる
		public abstract Group getGroupFromStation(Station station);
		// リストに関わるDBクエリが完了した
		protected abstract void onQueryFinished(String methodName, Object result);
		// ヘッダに関わるDBクエリが完了した
		protected abstract boolean onQueryForHeaderFinished(String methodName, Object result);

		// ヘッダをリロードする
		protected abstract void reloadHeaderGroup();
		// リストをロードする
		protected abstract void loadGroups();

		private void updateArguments() {
			Bundle bundle = new Bundle();
			bundle.putInt(STATE_PANEL_INDEX, panelIndex);
			bundle.putParcelable(STATE_HEADER_GROUP, headerGroup);
			setArguments(bundle);
		}

		// 指定されたGroupインスタンスが更新された
		public void updateGroup(Group group) {
			int code = group.getCode();
			if(code != 0) {
				for(int index = groups.size() - 1; index >= 0; --index) {
					if(groups.get(index).getCode() == code) {
						groups.set(index, group);
						if(cellAdapter != null) {
							cellAdapter.notifyDataSetChanged();
						}
					}
				}
			} else {
				headerGroup.copyStatistics(group);
				updateArguments();
			}
		}

		// リストのGroupが指定された内容に更新された
		public void updateAllGroups(List<Group> newGroups) {
			groups.clear();
			groups.ensureCapacity(newGroups.size());
			groups.addAll(newGroups);
			if(cellAdapter != null) {
				cellAdapter.notifyDataSetChanged();
			}
		}

		// ヘッダビューの表示を更新する
		public void updateHeaderText() {
			Resources resources = getResources();
			title.setText(headerGroup.getHeaderTitle(resources));
			description.setText(headerGroup.getDescription(resources));
		}

		// リスト用のデータベースクエリを実行する
		protected void callDatabaseForGroups(String methodName, Object... args) {
			recentRequestSequence = callDatabase(methodName, args);
			recentRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
		}
		// ヘッダ用のデータベースクエリを実行する
		protected void callDatabaseForHeader(String methodName, Object... args) {
			recentHeaderRequestSequence = callDatabase(methodName, args);
			recentHeaderRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
		}

		@Override
		protected void onDatabaseConnected(boolean forceReload, List<Station> updatedStations) {
			//TODO: updatedStationsが存在する場合はリロードが必要かも
			if(forceReload || groups.isEmpty()) {
				loadGroups();
			}
		}

		@Override
		protected void onQueryFinished(String methodName, Object result, long sequence) {
			if(recentRequestSequence == sequence) {
				recentRequestSequence = Long.MAX_VALUE;
				onQueryFinished(methodName, result);
				if(cellAdapter != null) {
					cellAdapter.notifyDataSetChanged();
				}
			} else if(recentHeaderRequestSequence == sequence) {
				recentHeaderRequestSequence = Long.MAX_VALUE;
				if(onQueryForHeaderFinished(methodName, result)) {
					((GroupFragmentBase)getParentFragment()).updateParentListGroup(this);
				}
			}
		}

		// 親がハンドリングする
		@Override
		protected final void onDatabaseUpdated() {}

		@Override
		protected void onStationUpdated(Station station) {
			Group group = getGroupFromStation(station);
			if(group != null) {
				updateGroup(group);
			}
		}

	}

	protected abstract ArrayList<Class<? extends PanelFragment>> getFragmentClasses();

	protected static PanelFragment newPanelFragnemt(Class<? extends PanelFragment> clazz, int panelIndex, Group headerGroup) {
		try {
			PanelFragment fragment = clazz.newInstance();
			if(headerGroup != null) {
				Bundle bundle = new Bundle();
				bundle.putInt(STATE_PANEL_INDEX, panelIndex);
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

	private PanelFragment addFragment(int index, PanelFragment previousFragment, FragmentManager manager, boolean hasNormalAnimation, Group headerGroup) {
		ArrayList<Class<? extends PanelFragment>> fragmentClasses = getFragmentClasses();
		if(previousFragment == null) {
			previousFragment = (PanelFragment)manager.findFragmentByTag(fragmentClasses.get(index - 1).getCanonicalName());
		}
		PanelFragment fragment = newPanelFragnemt(fragmentClasses.get(index), index, headerGroup);
		manager.beginTransaction()
			.setCustomAnimations(hasNormalAnimation ? R.anim.slide_in_right : R.anim.none, R.anim.none, R.anim.none, R.anim.slide_out_right)
			.add(fragment, fragmentClasses.get(index).getCanonicalName())
			.hide(previousFragment)
			.addToBackStack(index == 1 ? STACK_FIRST : null)
			.commit();
		return fragment;
	}

	private void addStationFragment() {
		FragmentManager manager = getChildFragmentManager();
		ArrayList<Class<? extends PanelFragment>> fragmentClasses = getFragmentClasses();
		Fragment previousFragment = manager.findFragmentByTag(fragmentClasses.get(numPanels - 1).getCanonicalName());
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
		ArrayList<Class<? extends PanelFragment>> fragmentClasses = getFragmentClasses();
		//for debug
		if(fragmentClasses == null) {
			numPanels = 0;
			return;
		}

		numPanels = savedInstanceState != null ? savedInstanceState.getInt(STATE_NUM_PANELS) : 0;
		if(numPanels <= 0 || fragmentClasses.size() <= numPanels) {
			numPanels = 1;
		}
		int panelIndex = 0;
		FragmentManager manager = getChildFragmentManager();
		PanelFragment fragment = newPanelFragnemt(fragmentClasses.get(panelIndex), panelIndex, null);
		manager.beginTransaction()
			.add(R.id.group_flipper, fragment, fragmentClasses.get(panelIndex).getCanonicalName())
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

	@Override
	public void onDatabaseUpdated() {
		FragmentManager manager = getChildFragmentManager();
		if(manager != null) {
			manager.popBackStack(STACK_FIRST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	//NOTUSED
	@Override
	protected void onDatabaseConnected(boolean forceReload, List<Station> updatedStations) {}

	//NOTUSED
	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {}

	//NOTUSED
	@Override
	protected void onStationUpdated(Station station) {}

	public void showVerbose(Station station) {
		this.station = station;
		addStationFragment();
	}

	public void showChild(Group group) {
		addFragment(++numPanels, null, getChildFragmentManager(), true, group);
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

	public void updateParentListGroup(PanelFragment panelFragment) {
		Bundle arguments = panelFragment.getArguments();
		int panelIndex = arguments.getInt(STATE_PANEL_INDEX);
		if(panelIndex > 0) {
			ArrayList<Class<? extends PanelFragment>> fragmentClasses = getFragmentClasses();
			FragmentManager manager = getChildFragmentManager();
			PanelFragment parentFragment = (PanelFragment)manager.findFragmentByTag(fragmentClasses.get(panelIndex - 1).getCanonicalName());
			if(parentFragment != null) {
				parentFragment.updateGroup((Group)arguments.getParcelable(STATE_HEADER_GROUP));
			}
		}
	}
}
