package com.wsf_lp.oritsubushi;

import androidx.fragment.app.Fragment;

public abstract class MenuableFragmentBase extends Fragment {

	@Override
	public void onStart() {
		super.onStart();
		notifyVisible(this);
	}
	
	public static void notifyVisible(Fragment fragment) {
		((MainActivity)fragment.getActivity()).onFragmentStart(fragment);
	}

	public void onReload() {

	}
}
