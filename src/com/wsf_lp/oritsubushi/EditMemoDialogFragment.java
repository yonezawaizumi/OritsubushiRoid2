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
	public static final String STATE_MEMO = "Memo";
	public static final String STATE_TITLE = "Title";

	private EditText memo;

	public static EditMemoDialogFragment newInstance(StationFragment stationFragment) {
		EditMemoDialogFragment fragment = new EditMemoDialogFragment();
		Station station = stationFragment.getStation();
		Bundle bundle = new Bundle();
		bundle.putString(STATE_MEMO, station.getMemo());
		bundle.putString(STATE_TITLE, station.getTitle());
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
		String memoString = null;
		if(savedInstanceState != null) {
			memoString = savedInstanceState.getString(STATE_MEMO);
		}
		Bundle argumentsBundle = getArguments();
		if(memoString == null) {
			memoString = argumentsBundle.getString(STATE_MEMO);
		}
		getDialog().setTitle(getString(R.string.verbose_edit_memo_title, argumentsBundle.getString(STATE_TITLE)));
		memo.setText(memoString != null ? memoString : "");
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
		outState.putString(STATE_MEMO, memo.getText().toString());
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch(which) {
		case DialogInterface.BUTTON_POSITIVE:
			((StationFragment)getTargetFragment()).updateMemo(memo.getText().toString());
			dialog.cancel();
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			dialog.cancel();
			break;
		}
	}
}
