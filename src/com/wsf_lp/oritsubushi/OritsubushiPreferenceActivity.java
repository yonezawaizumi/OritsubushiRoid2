package com.wsf_lp.oritsubushi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class OritsubushiPreferenceActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	public static final int ID = R.id.preferences;

	private ActivityChanger activityChanger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        activityChanger = new ActivityChanger(this, ID);
		addPreferencesFromResource(R.xml.preference);
		SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
		setListValue(preferences, PreferenceKey.MAX_STATIONS);
	}
	@Override
	protected void onResume() {
		super.onResume();
	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	protected void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(PreferenceKey.MAX_STATIONS)) {
			setListValue(sharedPreferences, PreferenceKey.MAX_STATIONS);
		}
	}

	protected void setListValue(SharedPreferences preferences, String key) {
    	ListPreference preference = (ListPreference)findPreference(key);
    	preference.setSummary(preference.getEntry());
	}
}
