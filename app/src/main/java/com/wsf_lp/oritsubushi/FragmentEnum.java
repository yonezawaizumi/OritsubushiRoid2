package com.wsf_lp.oritsubushi;

import android.content.Context;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class FragmentEnum {
	private static final FragmentEnum instance = new FragmentEnum();

	public static FragmentEnum getInstance() { return instance; }

	private static class MenuProperty {
		public final Class<? extends Fragment> fragmentClass;
		public final int id;
		public final int icon;
		public final int title;
		public final int action;
		public MenuProperty(Class<? extends Fragment> fragmentClass, int id, int icon, int title, int action) {
			this.fragmentClass = fragmentClass;
			this.id = id;
			this.icon = icon;
			this.title = title;
			this.action = action;
		}
	}

	private final MenuProperty[] MENUS = new MenuProperty[] {
		new MenuProperty(MapFragment.class, R.id.main, R.drawable.map, R.string.map, MenuItem.SHOW_AS_ACTION_NEVER),
		new MenuProperty(OperatorTypeGroupFragment.class, R.id.operator_type, R.drawable.operator_type, R.string.operator_type, MenuItem.SHOW_AS_ACTION_NEVER),
		new MenuProperty(PrefGroupFragment.class, R.id.pref, R.drawable.pref, R.string.pref, MenuItem.SHOW_AS_ACTION_NEVER),
		new MenuProperty(CompletionDateGroupFragment.class, R.id.comp_date, R.drawable.comp_date, R.string.comp_date, MenuItem.SHOW_AS_ACTION_NEVER),
		new MenuProperty(YomiGroupFragment.class, R.id.yomi, R.drawable.yomi, R.string.yomi, MenuItem.SHOW_AS_ACTION_NEVER),
		new MenuProperty(SyncFragment.class, R.id.sync, R.drawable.sync, R.string.sync, MenuItem.SHOW_AS_ACTION_ALWAYS),
		new MenuProperty(InformationFragment.class, R.id.info, R.drawable.info, R.string.info, MenuItem.SHOW_AS_ACTION_IF_ROOM),
		new MenuProperty(OritsubushiPreferenceFragment.class, R.id.preferences, R.drawable.preferences, R.string.preferences, MenuItem.SHOW_AS_ACTION_IF_ROOM),
	};

	private class Adapter extends ArrayAdapter<MenuProperty> {
		LayoutInflater inflater;
		int resourceId;
		public Adapter(Context context, int resourceId) {
			super(context, 0);
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.resourceId = resourceId;
		}
		@Override
		public int getCount() {
			return MENUS.length;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = inflater.inflate(resourceId, null);
			}
			ImageView icon = (ImageView)convertView.findViewById(R.id.menu_icon);
			icon.setImageResource(MENUS[position].icon);
			TextView text = (TextView)convertView.findViewById(R.id.menu_title);
			text.setText(MENUS[position].title);
			return convertView;
		}
	}

	public BaseAdapter getMenuAdapter(Context context, int resourceId) {
		return new Adapter(context, resourceId);
	}

	public int getMenuFragmentPosition(Class<? extends Fragment> fragmentClass) {
		for(int position = MENUS.length - 1; position >= 0; --position) {
			if(fragmentClass == MENUS[position].fragmentClass) {
				return position;
			}
		}
		return -1;
	}

	public Class<? extends Fragment> getFragmentClassByMenuPosition(int position) {
		if(position < 0 || MENUS.length <= position) {
			throw new IllegalArgumentException();
		} else {
			return MENUS[position].fragmentClass;
		}
	}

	public Class<? extends Fragment> getFragmentClassById(int id) {
		for(MenuProperty property : MENUS) {
			if(property.id == id) {
				return property.fragmentClass;
			}
		}
		throw new IllegalArgumentException("bad menu id");
	}

	public void addActionItems(Menu menu) {
		for(MenuProperty property : MENUS) {
			if(property.action == MenuItem.SHOW_AS_ACTION_NEVER) {
				continue;
			}
			MenuItem item = menu.add(Menu.NONE, property.id, Menu.NONE, property.title);
			item.setIcon(property.icon);
			item.setShowAsAction(property.action);
		}
	}

	public void enableActionItems(Menu menu, int position) {
		for(int index = MENUS.length - 1; index >= 0; --index) {
			if(MENUS[index].action == MenuItem.SHOW_AS_ACTION_NEVER) {
				continue;
			}
			menu.findItem(MENUS[index].id).setVisible(position != AdapterView.INVALID_POSITION && index != position);
		}
	}

	public boolean isActionPosition(int position) {
		return 0 <= position && position < MENUS.length && MENUS[position].action == MenuItem.SHOW_AS_ACTION_NEVER;
	}

}
