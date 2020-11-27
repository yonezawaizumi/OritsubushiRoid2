package com.wsf_lp.oritsubushi;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.app.BundleCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceFragmentCompat;

public class OritsubushiPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

	//@Override
	//protected int getXmlId() {
	//	return R.xml.preference;
	//}

	//@Override
	//protected int getLayoutId() {
	//	return R.layout.preference_list_content;
	//}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preference, rootKey);
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
