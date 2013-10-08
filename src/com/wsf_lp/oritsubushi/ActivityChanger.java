package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.AdapterView;
//import android.widget.AdapterView.OnItemClickListener;
//import android.widget.BaseAdapter;
//import android.widget.ListView;
//import android.widget.TextView;

public class ActivityChanger {
	private static final int MAX_MENUES = 5;
	private static class MenuProperty {
		public int id;
		public int icon;
		public int title;
		public MenuProperty(int id, int icon, int title) {
			this.id = id;
			this.icon = icon;
			this.title = title;
		}
	}
	private static final MenuProperty[] FRONTS = new MenuProperty[] {
		new MenuProperty(R.id.main, R.drawable.map, R.string.map),
		new MenuProperty(R.id.operator_type, R.drawable.operator_type, R.string.operator_type),
	};
	private static final int FRONTS_LENGTH = FRONTS.length;
	private static final MenuProperty[] FLOATS = new MenuProperty[] {
		new MenuProperty(R.id.pref, R.drawable.pref, R.string.pref),
		new MenuProperty(R.id.comp_date, R.drawable.comp_date, R.string.comp_date),
		new MenuProperty(R.id.yomi, R.drawable.yomi, R.string.yomi),
		new MenuProperty(R.id.info, R.drawable.info, R.string.info),
		new MenuProperty(R.id.sync, R.drawable.sync, R.string.sync),
		new MenuProperty(R.id.preferences, R.drawable.preferences, R.string.preferences),
	};
	private final static int FLOATS_LENGTH = FLOATS.length;
	private static final MenuProperty OTHER = new MenuProperty(R.id.more, R.drawable.other, R.string.other);

	private long recentOrder = 0;
	private ArrayList<MenuProperty> submenues = new ArrayList<MenuProperty>();
	private final int ID;
	private final WeakReference<Activity> activity;

	public ActivityChanger(Activity activity, int id) {
		ID = id;
		this.activity = new WeakReference<Activity>(activity);
	}

	private void addToMenu(Menu menu, MenuProperty property) {
		menu.add(Menu.NONE, property.id, Menu.NONE, property.title).setIcon(property.icon);
	}

	private long readFromPreferences(SharedPreferences preferences) {
		return preferences.getLong(PreferenceKey.MENU_ORDER, 0);
	}

	private void writeToPreferences(SharedPreferences preferences, long order) {
		preferences.edit().putLong(PreferenceKey.MENU_ORDER, order).commit();
	}

	private int[] parseOrders() {
		int[] orders = new int[FLOATS_LENGTH];
		long order = recentOrder;
		for(int index = 0; index < FLOATS_LENGTH; ++index) {
			final int orderIndex = (int)(order % 100);
			order /= 100;
			if(orderIndex < 0 || FLOATS_LENGTH <= orderIndex) {
				return null;
			}
			orders[index] = orderIndex;
		}
		return orders;
	}

	private void reorderMenu(Menu menu, SharedPreferences preferences) {
		menu.clear();
		ArrayList<MenuProperty> properties = new ArrayList<MenuProperty>(FRONTS_LENGTH + FLOATS_LENGTH);
		for(MenuProperty property : FRONTS) {
			if(property.id != ID) {
				properties.add(property);
			}
		}
		if(recentOrder != 0) {
			int[] orders = parseOrders();
			if(orders != null) {
				for(int orderIndex : orders) {
					if(FLOATS[orderIndex].id != ID) {
						properties.add(FLOATS[orderIndex]);
					}
				}
			} else {
				recentOrder = 0;
			}
		}
		if(recentOrder == 0) {
			for(MenuProperty property : FLOATS) {
				if(property.id != ID) {
					properties.add(property);
				}
			}
			for(int index = FLOATS_LENGTH - 1; index >= 0; --index) {
				recentOrder = recentOrder * 100 + index;
			}
			writeToPreferences(preferences, recentOrder);
		}
		int added = 0;
		for(int index = 0; index < MAX_MENUES && index < FRONTS_LENGTH + FLOATS_LENGTH; ++index, ++added) {
			addToMenu(menu, properties.get(index));
		}
		addToMenu(menu, OTHER);
		final int length = properties.size();
		submenues.clear();
		submenues.ensureCapacity(length - added);
		for(; added < length; ++added) {
			submenues.add(properties.get(added));
		}
	}

	private void updateOrder(Activity activity, int activityId) {
		int[] orders = parseOrders();
		for(int index = 0; index < FLOATS_LENGTH; ++index) {
			final int orderIndex = orders[index];
			if(FLOATS[orderIndex].id == activityId) {
				for(int jndex = index - 1; jndex >= 0; --jndex) {
					orders[jndex + 1] = orders[jndex];
				}
				orders[0] = orderIndex;
				long order = 0;
				for(int jndex = FLOATS_LENGTH - 1; jndex >= 0; --jndex) {
					order = order * 100 + orders[jndex];
				}
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
				writeToPreferences(preferences, order);
				recentOrder = 0;
			}
		}
	}

	public void createMenu(Menu menu)
	{
		Activity activity = this.activity.get();
		if(activity != null) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
			recentOrder = readFromPreferences(preferences);
			reorderMenu(menu, preferences);
		}
	}

	public void prepareMenu(Menu menu) {
		Activity activity = this.activity.get();
		if(activity != null) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
			long order = readFromPreferences(preferences);
			if(order != recentOrder) {
				recentOrder = order;
				reorderMenu(menu, preferences);
			}
		}
	}

	public boolean onSelectActivityMenu(final int activityId) {
		Activity activity = this.activity.get();
		if(activity == null) {
			return false;
		}
		Class <?> clazz = null;
		if (activityId == R.id.main) {
			clazz = OritsubushiRoidActivity.class;
		} else if (activityId == R.id.preferences) {
			clazz = OritsubushiPreferenceActivity.class;
		} else if (activityId == R.id.operator_type) {
			clazz = OperatorTypeGroupActivity.class;
		} else if (activityId == R.id.pref) {
			clazz = PrefGroupActivity.class;
		} else if (activityId == R.id.yomi) {
			clazz = YomiGroupActivity.class;
		} else if (activityId == R.id.comp_date) {
			clazz = CompletionDateGroupActivity.class;
		} else if (activityId == R.id.info) {
			clazz = InformationActivity.class;
		} else if (activityId == R.id.sync) {
			clazz = SyncActivity.class;
		} else if (activityId == R.id.more) {
			Resources resources = activity.getResources();
			String[] titles = new String[submenues.size()];
			for(int index = titles.length - 1; index >= 0; --index) {
				titles[index] = resources.getString(submenues.get(index).title);
			}
			Dialog dialog = new AlertDialog.Builder(activity).setTitle(OTHER.title).setIcon(OTHER.icon).setItems(titles, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final int id = submenues.get(which).id;
					onSelectActivityMenu(id);
				}
			}).create();
			dialog.getWindow().setGravity(Gravity.BOTTOM);
			dialog.show();
			/*ListView listView = new ListView(activity);
			listView.setAdapter(new CellAdapter());
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					final int id = submenues.get(position).id;
					onSelectActivityMenu(id);
				}
			});
			Dialog dialog = new AlertDialog.Builder(activity).setView(listView).create();
			Window window = dialog.getWindow();
			window.setGravity(Gravity.BOTTOM);
			WindowManager.LayoutParams layoutParams = window.getAttributes();
			layoutParams.width = resources.getDisplayMetrics().widthPixels * resources.getInteger(R.integer.menu_dialog_width_ratio) / 100;
			window.setAttributes(layoutParams);
			dialog.show();*/
			return true;
		}
		updateOrder(activity, activityId);
		if(clazz != null) {
			activity.startActivity(new Intent(activity, clazz));
			return true;
		} else {
			return false;
		}
	}

	/*private class CellAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return submenues.size();
		}
		@Override
		public Object getItem(int position) {
			return submenues.get(position);
		}
		@Override
		public long getItemId(int position) {
			return submenues.get(position).id;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if(view == null) {
				view = new TextView(activity);
			}
			MenuProperty menu = (MenuProperty)getItem(position);
			if(menu != null) {
				final TextView textView = (TextView)view;
				textView.setCompoundDrawablesWithIntrinsicBounds(menu.icon, 0, 0, 0);
				textView.setText(menu.title);
			}
			return view;
		}

	}*/
}
