package com.wsf_lp.oritsubushi;

import java.util.ArrayList;
import java.util.List;

import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public abstract class GroupActivityBase extends Activity
		implements DatabaseResultReceiver,
		DatabaseServiceConnector.Listener,
		OritsubushiBroadcastReceiver.UpdateListener {

	protected static final String STATE_TAG = "group";

	protected static final long RETRY_MSEC = 5000;

	private boolean isAlive;

	private DatabaseServiceConnector connector;
	private DatabaseService databaseService;
	private OritsubushiBroadcastReceiver broadcastReceiver;

	private ViewFlipper flipper;
	private Animation inAnimation;
	private Animation outAnimation;
	private Animation nonAnimation;

	private ActivityChanger activityChanger;

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

	protected boolean isAlive() { return isAlive; }
	protected int getCurrentPanelIndex() { return flipper.getDisplayedChild(); }
	protected Group getHeaderGroup(int panelIndex) { return panels[panelIndex].headerGroup; }
	protected void setHeaderGroup(int panelIndex, Group headerGroup) {
		panels[panelIndex].headerGroup = headerGroup;
	}
	protected ArrayList<Group> getGroups(int panelIndex) { return panels[panelIndex].groups; }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isAlive = true;
        activityChanger = new ActivityChanger(this, getIdForMenu());

		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setContentView(R.layout.group);
		flipper = (ViewFlipper)findViewById(R.id.group_flipper);
		inAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
		outAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
		nonAnimation = AnimationUtils.loadAnimation(this, R.anim.none);
		TypedValue value = new TypedValue();
		getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
		final int panelCount = getPanelCount();
		panels = new Panel[panelCount];
		for(int index = 0; index < panelCount; ++index) {
			final Panel panel = new Panel();
			final int panelIndex = index;	//for closure
			final View panelView = inflater.inflate(R.layout.list, null);
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
			panel.cellAdapter = new CellAdapter(panel.groups, this);
			panel.listView.setAdapter(panel.cellAdapter);
			panel.listView.setOnItemClickListener(new ListView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if(flipper.getDisplayedChild() == panelIndex && onSelectGroup(panelIndex, panel.groups.get(position))) {
						updateText(panelIndex + 1);
						panels[panelIndex + 1].wrapper.setVisibility(View.VISIBLE);
						flipper.setInAnimation(inAnimation);
						flipper.setOutAnimation(nonAnimation);
						flipper.showNext();
					}
				}
			});
			flipper.addView(panelView);
			panels[index] = panel;
		}

		if(!restoreInstance(savedInstanceState)) {
			panels[0].headerGroup = createDefaultTopHeaderGroup();
		}
		updateAllTexts();

        broadcastReceiver = new OritsubushiBroadcastReceiver(this);
        broadcastReceiver.registerTo(this, OritsubushiNotificationIntent.getIntentFilter());
        connector = new DatabaseServiceConnector();
        connector.connect(this, this);
	}

	protected boolean restoreInstance(final Bundle savedInstanceState) {
		if(savedInstanceState == null) {
			return false;
		}
		Parcelable[] parcelableArray = savedInstanceState.getParcelableArray(STATE_TAG);
		if(parcelableArray == null) {
			return false;
		}
		final int displayedChild = parcelableArray.length - 1;
		if(displayedChild < 0) {
			return false;
		}
		for(int index = displayedChild; index >= 0; --index) {
			panels[index].headerGroup = (Group)parcelableArray[index];
			if(databaseService != null) {
				reloadHeaderGroup(index);
				loadGroup(index);
			}
		}
		flipper.setInAnimation(null);
		flipper.setOutAnimation(null);
		flipper.setDisplayedChild(displayedChild);
		return true;
		//return false;
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreInstance(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		final int count = flipper.getDisplayedChild() + 1;
		Group[] headerGroups = new Group[count];
		for(int index = 0; index < count; ++index) {
			headerGroups[index] = panels[index].headerGroup;
		}
		outState.putParcelableArray(STATE_TAG, headerGroups);
	}

	@Override
	public void onDestroy() {
		isAlive = false;
		broadcastReceiver.unregisterFrom(this);
		connector.disconnect();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(getCurrentPanelIndex() > 0) {
				flipper.setInAnimation(nonAnimation);
				flipper.setOutAnimation(outAnimation);
				flipper.showPrevious();
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        activityChanger.createMenu(menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		activityChanger.prepareMenu(menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        return activityChanger.onSelectActivityMenu(item.getItemId());
    }

	protected boolean update1(int panelIndex, Group group) {
		final Panel panel = panels[panelIndex];
		final int code = group.getCode();
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
		final Panel panel = panels[panelIndex];
		panel.groups.clear();
		panel.groups.ensureCapacity(newGroups.size());
		panel.groups.addAll(newGroups);
	}

	protected abstract int getIdForMenu();
	protected abstract int getPanelCount();
	protected abstract int getMapFilterButtonVisibility(int panelIndex);
	protected abstract void onClickMapFilterButton(int panelIndex);
	protected abstract boolean onSelectGroup(int panelIndex, Group group);
	protected abstract boolean updateGroups(int panelIndex, String MethodName, Object result);
	protected abstract boolean updateHeader(int panelIndex, String MethodName, Object result);
	protected abstract void reloadHeaderGroup(int panelIndex);
	protected abstract void loadGroup(int panelIndex);
	protected abstract Group createDefaultTopHeaderGroup();
	protected abstract int updateStation(Station station);

/*	protected void loadAllGroups() {
		final int count = flipper.getDisplayedChild() + 1;
		for(int index = 0; index < count; ++index) {
			loadGroup(index);
		}
	}*/
	protected void reset() {
		flipper.setInAnimation(null);
		flipper.setOutAnimation(null);
		flipper.setDisplayedChild(0);
		loadGroup(0);
	}

	/* must be override */
	protected void updateText(int panelIndex) {
		if(!isAlive()) {
			return;
		}
		Resources resources = getResources();
		final Panel panel = panels[panelIndex];
		panel.title.setText(panel.headerGroup.getHeaderTitle(resources));
		panel.description.setText(panel.headerGroup.getDescription(resources));
	}

	protected void updateAllTexts() {
		final int count = flipper.getDisplayedChild() + 1;
		for(int index = 0; index < count; ++index) {
			updateText(index);
		}
	}

	protected boolean processReloadHeaderGroup(final int headerPanelIndex, final Group group) {
		final int currentIndex = getCurrentPanelIndex();
		if(headerPanelIndex > currentIndex) {
			return false;
		}
		getHeaderGroup(headerPanelIndex).copyStatistics(group);
		return headerPanelIndex > 0 && update1(headerPanelIndex - 1, group);
	}

	protected void callDatabase(int panelIndex, String methodName, Object... args) {
		panels[panelIndex].recentRequestSequence = databaseService.callDatabase(this, methodName, args);
		panels[panelIndex].recentRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
	}
	protected void callDatabaseForHeader(int panelIndex, String methodName, Object... args) {
		panels[panelIndex].recentHeaderRequestSequence = databaseService.callDatabase(this, methodName, args);
		panels[panelIndex].recentHeaderRequestLimit = System.currentTimeMillis() + RETRY_MSEC;
	}

	@Override
	public void onDatabaseResult(long sequence, String methodName, Object result) {
		if(!isAlive()) {
			return;
		}
		final int count = panels.length;
		int updateIndex = -1;
		for(int index = 0; index < count; ++index) {
			final Panel panel = panels[index];
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
			final Panel panel = panels[updateIndex];
			panel.wrapper.setVisibility(View.GONE);
			panel.cellAdapter.notifyDataSetChanged();
			updateAllTexts();
		}
	}

	@Override
	public void onDatabaseUpdated(Station station, int sequence) {
		if(station == null) {
			reset();
		} else {
			int panelIndex = updateStation(station);
			if(panelIndex >= 0) {
				panels[panelIndex].cellAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onDatabaseConnected(DatabaseService service) {
		this.databaseService = service;
		for(int index = flipper.getDisplayedChild(); index >= 0; --index) {
			reloadHeaderGroup(index);
			loadGroup(index);
		}
	}

	@Override
	public void onDatabaseDisconnected() {
		this.databaseService = null;
	}
}
