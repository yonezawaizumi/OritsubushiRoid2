package com.wsf_lp.oritsubushi;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;

import com.wsf_lp.android.PreferenceFragment;

public class OritsubushiPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	public OritsubushiPreferenceFragment() {
	}

	@Override
	protected int getXmlId() {
		return R.xml.preference;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.preference_list_content;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		MenuableFragmentBase.notifyVisible(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		setListValue(preferenceScreen.getSharedPreferences(), PreferenceKey.MAX_STATIONS);
	    preferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		if(key.equals(PreferenceKey.MAX_STATIONS)) {
			setListValue(preferences, PreferenceKey.MAX_STATIONS);
		}
	}

	private void setListValue(SharedPreferences preferences, String key) {
    	ListPreference preference = (ListPreference)findPreference(key);
    	preference.setSummary(preference.getEntry());
	}
}
