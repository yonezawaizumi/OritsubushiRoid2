package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;

import com.wsf_lp.android.PreferenceFragment.OnPreferenceAttachedListener;

import android.content.res.Configuration;
import android.os.Bundle;
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

public class MainActivity extends ActionBarActivity implements OnPreferenceAttachedListener, AdapterView.OnItemClickListener, FragmentManager.OnBackStackChangedListener {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    //DIRTY hack
    private WeakReference<Fragment> mCurrentFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_ab);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // DrawerList button
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.yomi,
                R.string.yomi
        ) {
        	@Override
        	public void onDrawerOpened(View view) {
        		supportInvalidateOptionsMenu();
        	}
        	@Override
        	public void onDrawerClosed(View view) {
        		execFragment(0);
        		supportInvalidateOptionsMenu();
        	}
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // DrawerList
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(FragmentEnum.getMenuAdapter(this, R.layout.drawable_menu));
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setItemChecked(0, true);
        mDrawerList.setOnItemClickListener(this);
    }

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
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
    	FragmentEnum.addActionItems(menu);
    	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        FragmentEnum.enableActionItems(menu, mDrawerLayout.isDrawerOpen(mDrawerList) ? AdapterView.INVALID_POSITION : mDrawerList.getCheckedItemPosition());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if(execFragment(item.getItemId())) {
    		supportInvalidateOptionsMenu();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean execFragment(int id) {
        Class<? extends Fragment> fragmentClass;
        int position;
    	if(id != 0) {
            fragmentClass = FragmentEnum.getFragmentClassById(id);
            position = FragmentEnum.getMenuFragmentPosition(fragmentClass);
    	} else {
    		position = mDrawerList.getCheckedItemPosition();
    		fragmentClass = FragmentEnum.getFragmentClassByMenuPosition(position);
    	}
    	if(fragmentClass != null) {
        	String tag = fragmentClass.getCanonicalName();
        	FragmentManager fragmentManager = getSupportFragmentManager();
        	Fragment fragment = fragmentManager.findFragmentByTag(tag);
        	if(fragment == null) {
        		try {
					fragment = fragmentClass.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	FragmentTransaction transaction = fragmentManager.beginTransaction();
        	transaction.replace(R.id.content_frame, fragment, tag);
        	transaction.addToBackStack(null);
        	transaction.commit();
        	mDrawerList.setItemChecked(position, true);
        	return true;
        }
    	return false;
    }

	@Override
	public void onBackStackChanged() {
		
	}

	@Override
    public boolean onBackPressed() {

    }


	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
		;
	}

}
