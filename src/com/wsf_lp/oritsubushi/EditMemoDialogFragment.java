package com.wsf_lp.oritsubushi;

import com.wsf_lp.mapapp.data.Station;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;

public class EditMemoDialogFragment extends DialogFragment
	implements DialogInterface.OnClickListener {
	public static final String STATE_STATION = "Station";

	private Station station;
	private EditText memo;

	public static EditMemoDialogFragment newInstance(StationFragment stationFragment) {
		EditMemoDialogFragment fragment = new EditMemoDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(STATE_STATION, stationFragment.getStation());
		fragment.setArguments(bundle);
		fragment.setTargetFragment(stationFragment, 0);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		FragmentActivity activity = getActivity();
		memo = new EditText(activity);
		return (new AlertDialog.Builder(activity))
			.setTitle(R.string.verbose_edit_comp_date_title_loading)
			.setView(memo)
			.setPositiveButton(R.string.ok, this)
			.setNegativeButton(R.string.cancel, this)
			.create();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState != null) {
			station = new Station((Station)savedInstanceState.getParcelable(STATE_STATION));
		}
		if(station == null) {
			station = new Station((Station)getArguments().getParcelable(STATE_STATION));
		}
		getDialog().setTitle(getString(R.string.verbose_edit_memo_title, station.getTitle()));
		memo.setText(station.getMemo());
		memo.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				}
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_STATION, station);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch(which) {
		case DialogInterface.BUTTON_POSITIVE:
			station.setMemo(memo.getText().toString());
			((StationFragment)getTargetFragment()).updateStation(station);
			dialog.cancel();
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			dialog.cancel();
			break;
		}
	}
}
