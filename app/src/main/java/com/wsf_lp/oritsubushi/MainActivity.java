package com.wsf_lp.oritsubushi;

import java.util.Arrays;
import java.util.WeakHashMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.wsf_lp.android.PreferenceFragment.OnPreferenceAttachedListener;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity implements
		OnPreferenceAttachedListener, AdapterView.OnItemClickListener {
	public static final int CONTENT_VIEW_ID = R.id.content_frame;

	public static final String ARG_FRAGMENT = "fragment";

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private boolean mDrawerMenuIsSelected;

	private WeakHashMap<OnBackPressedListener, String> mOnBackPressedListeners = new WeakHashMap<OnBackPressedListener, String>();

	private static final String[] PERMISSIONS = {
			android.Manifest.permission.ACCESS_FINE_LOCATION,
			android.Manifest.permission.ACCESS_COARSE_LOCATION
	};

	interface OnAcceptMyLocationListener {
		void onAcceptMyLocation();
	}

	private OnAcceptMyLocationListener mMyLocationListener;

	public static final int MAP_IS_DISABLED = 0;
	public static final int MY_LOCATION_IS_DISABLED = 1;
	public static final int MY_LOCATION_IS_ENABLED = 2;
	private int mMapStatus = MAP_IS_DISABLED;

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

		FirebaseMessaging.getInstance().subscribeToTopic("oritsubushiroid");
		int fragmentId = onIntent(getIntent(	));
		int fragmentPosition;
		if (fragmentId == 0) {
			fragmentPosition = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKey.RECENT_FRAGMENT_POSITION, 0);
		} else {
			fragmentPosition = -1;
		}

		GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
		int result = googleApiAvailability.isGooglePlayServicesAvailable(this);
		if (result == ConnectionResult.SUCCESS) {
			if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED) {
				setMapStatus(MY_LOCATION_IS_DISABLED);
				ActivityCompat.requestPermissions(this, PERMISSIONS, 0);
			} else {
				setMapStatus(MY_LOCATION_IS_ENABLED);
			}
		} else if (googleApiAvailability.isUserResolvableError(result)) {
			googleApiAvailability.getErrorDialog(this, result, 1, new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
				}
			}).show();
		}

		setContentView(R.layout.main_ab);

		Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
		//toolbar.setLogo(R.drawable.icon);
		toolbar.setTitle(R.string.app_name);
		setSupportActionBar(toolbar);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setHomeButtonEnabled(true);

		// DrawerLayout
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		// DrawerList button
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open_desc, R.string.drawer_close_desc) {
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
		mDrawerLayout.addDrawerListener(mDrawerToggle);
		mDrawerToggle.setDrawerIndicatorEnabled(true);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		// DrawerList
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setAdapter(FragmentEnum.getInstance().getMenuAdapter(this, R.layout.drawable_menu));
		mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mDrawerList.setOnItemClickListener(this);
		if (fragmentPosition >= 0) {
			mDrawerList.setItemChecked(fragmentPosition, true);
			execFragment(0, true);
		} else {
			execFragment(fragmentId, true);
		}
	}

	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		int[] granted2 = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};
		if (Arrays.equals(permissions, PERMISSIONS) && Arrays.equals(grantResults, granted2)) {
			setMapStatus(MY_LOCATION_IS_ENABLED);
		}
	}

	public void setOnAcceptMyLocationListener(OnAcceptMyLocationListener listener) {
		mMyLocationListener = listener;
		if (getMapStatus() == MY_LOCATION_IS_ENABLED) {
			listener.onAcceptMyLocation();
		}
	}

	public int getMapStatus() {
		return mMapStatus;
	}

	private void setMapStatus(int mapStatus) {
		int prevMapStatus = mMapStatus;
		mMapStatus = mapStatus;
		if (mapStatus == MY_LOCATION_IS_ENABLED && prevMapStatus != MY_LOCATION_IS_ENABLED && mMyLocationListener != null) {
			mMyLocationListener.onAcceptMyLocation();
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		int id = onIntent(intent);
		if (id != 0) {
			execFragment(id, false);
		}
	}

	private int onIntent(Intent intent) {
		if (intent != null && "info".equals(intent.getStringExtra(ARG_FRAGMENT))) {
			return R.id.info;
		} else {
			return 0;
		}
	}

	public static class FinishDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
					.setTitle(getString(R.string.map_update_title))
					.setMessage(getString(R.string.map_update_message))
					.setPositiveButton(getString(R.string.map_update_ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getActivity().finish();
						}
					})
					.setNegativeButton(getString(R.string.map_update_cancel), null)
					.create();
		}

		@Override
		public void onPause() {
			super.onPause();
			dismiss();
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
		int result = googleApiAvailability.isGooglePlayServicesAvailable(this);
		if (result == ConnectionResult.SUCCESS) {
			FinishDialogFragment dialog = new FinishDialogFragment();
			dialog.show(getSupportFragmentManager(), "finish_dialog");
		}
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
				PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(PreferenceKey.RECENT_FRAGMENT_POSITION, position).apply();
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
