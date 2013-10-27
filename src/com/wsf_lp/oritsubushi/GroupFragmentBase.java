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

	private ViewFlipper mFlipper;
	private Animation mInAnimation;
	private Animation mOutAnimation;
	private Animation mNonAnimation;

	protected static class Panel {
		public TextView mTitle;
		public TextView mDescription;
		public Button mMapFilterButton;
		public ListView mListView;
		public View mWrapper;
		public CellAdapter mCellAdapter;
		public Group mHeaderGroup;
		public ArrayList<Group> mGroups = new ArrayList<Group>();
		public long mRecentRequestSequence;
		public long mRecentRequestLimit = Long.MAX_VALUE;
		public long mRecentHeaderRequestSequence;
		public long mRecentHeaderRequestLimit = Long.MAX_VALUE;
	}
	private Panel[] mPanels;

	//for retain fragment
	private int mCurrentPanelIndex;

	public int getCurrentPanelIndex() { return mCurrentPanelIndex; }

	protected Group getHeaderGroup(int panelIndex) { return mPanels[panelIndex].mHeaderGroup; }
	protected void setHeaderGroup(int panelIndex, Group headerGroup) {
		mPanels[panelIndex].mHeaderGroup = headerGroup;
	}
	protected ArrayList<Group> getGroups(int panelIndex) { return mPanels[panelIndex].mGroups; }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int panelCount = getPanelCount();
		mPanels = new Panel[panelCount];
		for(int index = 0; index < panelCount; ++index) {
			mPanels[index] = new Panel();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = getActivity();
		View view = inflater.inflate(R.layout.group, container, false);

		mFlipper = (ViewFlipper)view.findViewById(R.id.group_flipper);
		mInAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_in_right);
		mOutAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_right);
		mNonAnimation = AnimationUtils.loadAnimation(activity, R.anim.none);
		TypedValue value = new TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
		int panelCount = getPanelCount();
		for(int index = 0; index < panelCount; ++index) {
			Panel panel = mPanels[index];
			final int panelIndex = index;	//for closure
			View panelView = inflater.inflate(R.layout.list, null);
			panelView.setBackgroundResource(value.resourceId);
			panel.mTitle = (TextView)panelView.findViewById(R.id.title);
			panel.mDescription = (TextView)panelView.findViewById(R.id.description);
			panel.mMapFilterButton = (Button)panelView.findViewById(R.id.list_button_map_filter);
			panel.mListView = (ListView)panelView.findViewById(R.id.list);
			panel.mWrapper = panelView.findViewById(R.id.wrapper);
			panel.mWrapper.setBackgroundResource(value.resourceId);
			panel.mWrapper.setVisibility(View.VISIBLE);
			panel.mMapFilterButton.setVisibility(getMapFilterButtonVisibility(panelIndex));
			panel.mMapFilterButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onClickMapFilterButton(panelIndex);
				}
			});
			panel.mCellAdapter = new CellAdapter(panel.mGroups, activity);
			panel.mListView.setAdapter(panel.mCellAdapter);
			panel.mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if(getCurrentPanelIndex() == panelIndex && onSelectGroup(panelIndex, mPanels[panelIndex].mGroups.get(position))) {
						updateText(panelIndex + 1);
						mPanels[panelIndex + 1].mWrapper.setVisibility(View.VISIBLE);
						mFlipper.setInAnimation(mInAnimation);
						mFlipper.setOutAnimation(mNonAnimation);
						++mCurrentPanelIndex;
						mFlipper.showNext();
					}
				}
			});
			mFlipper.addView(panelView);
			mPanels[index] = panel;
		}

		return view;
	}

	protected boolean restoreInstance(Bundle savedInstanceState) {
		boolean recreated = mPanels[0].mGroups.isEmpty();
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
			mCurrentPanelIndex = displayedChild;
			for(int index = displayedChild; index >= 0; --index) {
				mPanels[index].mHeaderGroup = (Group)parcelableArray[index];
				if(isDatabaseEnabled()) {
					reloadHeaderGroup(index);
					loadGroup(index);
				}
			}
		} else {
			//TODO: restore listview position
			for(int index = 0; index <= mCurrentPanelIndex; ++index) {
				mPanels[index].mCellAdapter.notifyDataSetChanged();
				mPanels[index].mWrapper.setVisibility(View.GONE);
			}
		}
		mFlipper.setInAnimation(null);
		mFlipper.setOutAnimation(null);
		mFlipper.setDisplayedChild(getCurrentPanelIndex());
		return true;
		//return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(!restoreInstance(savedInstanceState)) {
			mPanels[0].mHeaderGroup = createDefaultTopHeaderGroup();
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
			headerGroups[index] = mPanels[index].mHeaderGroup;
		}
		outState.putParcelableArray(STATE_TAG, headerGroups);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mFlipper = null;
		for(Panel panel : mPanels) {
			panel.mTitle = null;
			panel.mDescription = null;
			panel.mMapFilterButton = null;
			panel.mListView = null;
			panel.mWrapper = null;
			panel.mCellAdapter = null;
		}
	}

	@Override
	public boolean onBackPressed(MainActivity activity) {
		if(mFlipper != null && mFlipper.isShown() && getCurrentPanelIndex() > 0) {
			mFlipper.setInAnimation(mNonAnimation);
			mFlipper.setOutAnimation(mOutAnimation);
			--mCurrentPanelIndex;
			mFlipper.showPrevious();
			return true;
		} else {
			return false;
		}
	}

	protected boolean update1(int panelIndex, Group group) {
		Panel panel = mPanels[panelIndex];
		int code = group.getCode();
		if(code != 0) {
			for(int index = 0; index < panel.mGroups.size(); ++index) {
				if(panel.mGroups.get(index).getCode() == code) {
					panel.mGroups.set(index, group);
					return true;
				}
			}
			return false;
		} else {
			panel.mHeaderGroup.setTotal(group.getTotal());
			panel.mHeaderGroup.setCompletions(group.getCompletions());
			return false;
		}
	}

	protected void updateAll(int panelIndex, List<Group> newGroups) {
		Panel panel = mPanels[panelIndex];
		panel.mGroups.clear();
		panel.mGroups.ensureCapacity(newGroups.size());
		panel.mGroups.addAll(newGroups);
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
		mCurrentPanelIndex = 0;
		if(mFlipper != null) {
			mFlipper.setInAnimation(null);
			mFlipper.setOutAnimation(null);
			mFlipper.setDisplayedChild(0);
		}
		loadGroup(0);
	}

	/* must be override */
	protected void updateText(int panelIndex) {
		if(!isAlive()) {
			return;
		}
		Resources resources = getResources();
		Panel panel = mPanels[panelIndex];
		panel.mTitle.setText(panel.mHeaderGroup.getHeaderTitle(resources));
		panel.mDescription.setText(panel.mHeaderGroup.getDescription(resources));
	}

	protected void updateAllTexts() {
		int count = getCurrentPanelIndex();
		for(int index = 0; index <= count; ++index) {
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
		mPanels[panelIndex].mRecentRequestSequence = callDatabase(methodName, args);
		mPanels[panelIndex].mRecentRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
	}
	protected void callDatabaseForHeader(int panelIndex, String methodName, Object... args) {
		mPanels[panelIndex].mRecentHeaderRequestSequence = callDatabase(methodName, args);
		mPanels[panelIndex].mRecentHeaderRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
	}

	@Override
	protected void onQueryFinished(String methodName, Object result, long sequence) {
		int count = mPanels.length;
		int updateIndex = -1;
		for(int index = 0; index < count; ++index) {
			Panel panel = mPanels[index];
			if(panel.mRecentRequestSequence == sequence) {
				panel.mRecentRequestLimit = Long.MAX_VALUE;
				if(updateGroups(index, methodName, result)) {
					updateIndex = index;
				}
				break;
			} else if(panel.mRecentHeaderRequestSequence == sequence) {
				panel.mRecentHeaderRequestLimit = Long.MAX_VALUE;
				if(updateHeader(index, methodName, result)) {
					updateIndex = index - 1;
				}
				break;
			}
		}
		if(mFlipper != null && updateIndex >= 0) {
			Panel panel = mPanels[updateIndex];
			if(panel != null) {
				panel.mWrapper.setVisibility(View.GONE);
				panel.mCellAdapter.notifyDataSetChanged();
				updateAllTexts();
			}
		}
	}

	@Override
	protected void onDatabaseConnected(boolean isEnabled, boolean forceReload, List<Station> updatedStations) {
		if(isEnabled) {
			for(int index = getCurrentPanelIndex(); index >= 0; --index) {
				reloadHeaderGroup(index);
				loadGroup(index);
			}
		}
	}

	@Override
	protected void onDatabaseUpdated(boolean isFirst) {
		reset();
	}

	@Override
	protected void onStationUpdated(Station station) {
		int panelIndex = updateStation(station);
		if(panelIndex >= 0 && mFlipper != null) {
			mPanels[panelIndex].mCellAdapter.notifyDataSetChanged();
		}
	}
}
