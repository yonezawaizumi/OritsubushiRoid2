package com.wsf_lp.oritsubushi;

import java.util.WeakHashMap;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.MapsInitializer;
import com.wsf_lp.android.PreferenceFragment.OnPreferenceAttachedListener;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity implements
		OnPreferenceAttachedListener, AdapterView.OnItemClickListener {
	public static final int CONTENT_VIEW_ID = R.id.content_frame;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private boolean mDrawerMenuIsSelected;

	private WeakHashMap<OnBackPressedListener, String> mOnBackPressedListeners = new WeakHashMap<OnBackPressedListener, String>();


	public void registerOnBackPressedListener(OnBackPressedListener listener) {
		mOnBackPressedListeners.put(listener,  "");
	}

	public void unregisterOnBackPressedListener(OnBackPressedListener listener) {
		mOnBackPressedListeners.remove(listener);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //初期化値を先に設定しておかないとデータベース生成時の初期化が完全に行われない
        PreferenceManager.setDefaultValues(this, R.xml.preference, true);

		int recentFragmentPosition = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKey.RECENT_FRAGMENT_POSITION, 0);

        try {
			MapsInitializer.initialize(this);
		} catch (GooglePlayServicesNotAvailableException e) {
			e.printStackTrace();
		}

		setContentView(R.layout.main_ab);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setHomeButtonEnabled(true);

		// DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		// DrawerList button
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open_desc, R.string.drawer_close_desc) {
			@Override
			public void onDrawerOpened(View view) {
				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View view) {
				supportInvalidateOptionsMenu();
				if(mDrawerMenuIsSelected) {
					mDrawerMenuIsSelected = false;
					execFragment(0, false);
				}
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// DrawerList
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setAdapter(FragmentEnum.getInstance().getMenuAdapter(this, R.layout.drawable_menu));
		mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mDrawerList.setItemChecked(recentFragmentPosition, true);
		mDrawerList.setOnItemClickListener(this);
		execFragment(0, true);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		mDrawerMenuIsSelected = true;
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		FragmentEnum.getInstance().addActionItems(menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		FragmentEnum.getInstance().enableActionItems(
				menu,
				mDrawerLayout.isDrawerOpen(mDrawerList) ? AdapterView.INVALID_POSITION : mDrawerList.getCheckedItemPosition())
		;
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if(item.getItemId() == android.R.id.home){
        	getSupportFragmentManager().popBackStack();
        } else if(execFragment(item.getItemId(), false)) {
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

	private boolean execFragment(int id, boolean isFirst) {
		FragmentEnum fragmentEnum = FragmentEnum.getInstance();
		Class<? extends Fragment> fragmentClass;
		int position;
		if (id != 0) {
			fragmentClass = fragmentEnum.getFragmentClassById(id);
			position = fragmentEnum.getMenuFragmentPosition(fragmentClass);
		} else {
			position = mDrawerList.getCheckedItemPosition();
			fragmentClass = fragmentEnum.getFragmentClassByMenuPosition(position);
		}
		if (fragmentClass != null) {
			String tag = fragmentClass.getCanonicalName();
			FragmentManager fragmentManager = getSupportFragmentManager();
			Fragment fragment = fragmentManager.findFragmentByTag(tag);
			if (fragment == null) {
				try {
					fragment = fragmentClass.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if(fragment.getView() != null && fragment.getView().isShown()) {
				return true;
			}
			FragmentTransaction transaction = fragmentManager
					.beginTransaction();
			transaction.replace(CONTENT_VIEW_ID, fragment, tag);
			if(!isFirst) {
				transaction.addToBackStack(null);
			}
			transaction.commit();
			return true;
		}
		return false;
	}

	public void onFragmentStart(Fragment fragment) {
		FragmentEnum fragmentEnum = FragmentEnum.getInstance();
		int position = fragmentEnum.getMenuFragmentPosition(fragment.getClass());
		if(position >= 0) {
			if(mDrawerList.getSelectedItemPosition() != position) {
				mDrawerList.setItemChecked(position, true);
			}
			if(fragmentEnum.isActionPosition(position)) {
				PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(PreferenceKey.RECENT_FRAGMENT_POSITION, position).commit();
			}
			supportInvalidateOptionsMenu();
		}
	}
	
	public void enableUpButton(boolean enabled) {
		mDrawerToggle.setDrawerIndicatorEnabled(!enabled);
	}

	@Override
	public void onBackPressed() {
    	for(OnBackPressedListener listener : mOnBackPressedListeners.keySet()) {
    		if(listener.onBackPressed(this)) {
    			return;
    		}
    	}
		super.onBackPressed();
	}

	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
		;
	}

	
}
