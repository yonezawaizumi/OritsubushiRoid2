package com.wsf_lp.oritsubushi;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.wsf_lp.mapapp.data.Group;
import com.wsf_lp.mapapp.data.Station;

public abstract class ListFragmentBase extends Fragment {
	private Animation inAnimation;
	private Animation outAnimation;
	private Animation nonAnimation;

	public abstract static class PanelFragment extends DBAccessFragmentBase {
		protected TextView title;
		protected TextView description;
		protected Button mapFilterButton;
		protected ListView listView;
		protected View wrapper;
		protected CellAdapter cellAdapter;
		protected Group headerGroup;
		protected ArrayList<Group> groups = new ArrayList<Group>();
		protected long recentRequestSequence;
		protected long recentRequestLimit = Long.MAX_VALUE;
		protected long recentHeaderRequestSequence;
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
			mapFilterButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onClickMapFilterButton();
				}
			});
			cellAdapter = new CellAdapter(groups, activity);
			listView.setAdapter(cellAdapter);
			listView.setOnItemClickListener(new ListView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if(onSelectGroup(groups.get(position))) {
						ListFragmentBase listFragment = (ListFragmentBase)getParentFragment();
						listFragment.updateSubListText(PanelFragment.this);
					}
				}
			});

			return panelView;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
		}

		public abstract int getMapFilterButtonVisibility();
		public abstract void onClickMapFilterButton();
		public abstract boolean onSelectGroup(Group group);
		public void updateText() {
			Resources resources = getResources();
			title.setText(headerGroup.getHeaderTitle(resources));
			description.setText(headerGroup.getDescription(resources));
		}

		@Override
		public void onDatabaseResult(long sequence, String methodName, Object result) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onDatabaseUpdated(Station station) {
			// TODO 自動生成されたメソッド・スタブ

		}
	}

	protected abstract Class<? extends PanelFragment>[] getFragmentClasses();
	protected int getFragmentPosition(PanelFragment fragment) {
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		for(int position = fragmentClasses.length - 1; position >= 0; --position) {
			if(fragmentClasses[position].isInstance(fragment)) {
				return position;
			}
		}
		throw new IllegalStateException("bad fragment " + fragment.getClass().getCanonicalName());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Activity activity = getActivity();
		inAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_in_right);
		outAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_out_right);
		nonAnimation = AnimationUtils.loadAnimation(activity, R.anim.none);
	}

	public void updateSubListText(PanelFragment panelFragment) {
		int position = getFragmentPosition(panelFragment);
		Class<? extends PanelFragment>[] fragmentClasses = getFragmentClasses();
		if(position < fragmentClasses.length - 1) {
			++position;
			FragmentManager manager = getChildFragmentManager();
			PanelFragment subFragment = (PanelFragment)manager.findFragmentByTag(fragmentClasses[position].getCanonicalName());
			if(subFragment != null) {
				subFragment.updateText();
			}
		}
	}

}
