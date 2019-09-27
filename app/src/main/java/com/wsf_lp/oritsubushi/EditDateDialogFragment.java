package com.wsf_lp.oritsubushi;

import java.util.Calendar;
import java.util.Date;

import com.wsf_lp.mapapp.data.Station;
import com.wsf_lp.oritsubushi.customs.NumberPickerCompat;
import com.wsf_lp.oritsubushi.customs.NumberPicker11;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

@SuppressLint("NewApi")
public class EditDateDialogFragment extends DialogFragment
	implements ToggleButton.OnCheckedChangeListener,
		NumberPickerCompat.OnValueChangeListener,
		NumberPicker11.OnValueChangeListener11,
		DialogInterface.OnClickListener,
		DialogInterface.OnCancelListener {
	public static final String STATE_STATION = "Station";
	public static final boolean use11 = Build.VERSION.SDK_INT >= 11;

	private Station station;
	private int date;
	private TextView completionDate;
	private NumberPickerCompat yearPicker;
	private NumberPickerCompat monthPicker;
	private NumberPickerCompat dayPicker;
	private NumberPicker11 yearPicker11;
	private NumberPicker11 monthPicker11;
	private NumberPicker11 dayPicker11;
	private ToggleButton year0Button;
	private ToggleButton month0Button;
	private ToggleButton day0Button;
	private CheckBox completedCheck;
	private boolean initializing;

	public static EditDateDialogFragment newInstance(StationFragment stationFragment) {
		EditDateDialogFragment fragment = new EditDateDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(STATE_STATION, new Station(stationFragment.getStation()));
		fragment.setArguments(bundle);
		fragment.setTargetFragment(stationFragment, 0);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		FragmentActivity activity = getActivity();
		View containerView = ((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(use11 ? R.layout.comp_date_edit_dialog_11 : R.layout.comp_date_edit_dialog, null, false);
		completionDate = (TextView)containerView.findViewById(R.id.comp_date_date);
		if(use11) {
			yearPicker11 = (NumberPicker11)containerView.findViewById(R.id.comp_date_edit_year);
			monthPicker11 = (NumberPicker11)containerView.findViewById(R.id.comp_date_edit_month);
			dayPicker11 = (NumberPicker11)containerView.findViewById(R.id.comp_date_edit_day);
			yearPicker11.setOnValueChangedListener(this);
			monthPicker11.setOnValueChangedListener(this);
			dayPicker11.setOnValueChangedListener(this);
		} else {
			yearPicker = (NumberPickerCompat)containerView.findViewById(R.id.comp_date_edit_year);
			monthPicker = (NumberPickerCompat)containerView.findViewById(R.id.comp_date_edit_month);
			dayPicker = (NumberPickerCompat)containerView.findViewById(R.id.comp_date_edit_day);
			yearPicker.setOnValueChangedListener(this);
			monthPicker.setOnValueChangedListener(this);
			dayPicker.setOnValueChangedListener(this);
		}
		year0Button = (ToggleButton)containerView.findViewById(R.id.comp_date_edit_0year);
		month0Button = (ToggleButton)containerView.findViewById(R.id.comp_date_edit_0month);
		day0Button = (ToggleButton)containerView.findViewById(R.id.comp_date_edit_0day);
		completedCheck = (CheckBox)containerView.findViewById(R.id.comp_date_edit_incomp_button);
		year0Button.setOnCheckedChangeListener(this);
		month0Button.setOnCheckedChangeListener(this);
		day0Button.setOnCheckedChangeListener(this);
		completedCheck.setOnCheckedChangeListener(this);
		return (new AlertDialog.Builder(activity))
			.setTitle(R.string.verbose_edit_comp_date_title)
			.setView(containerView)
			.setPositiveButton(R.string.ok, this)
			.setNegativeButton(R.string.cancel, this)
			.create();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initializing = true;
		if(savedInstanceState != null) {
			station = savedInstanceState.getParcelable(STATE_STATION);
		}
		if(station == null) {
			station = getArguments().getParcelable(STATE_STATION);
		}
		setDate();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		if(use11) {
			yearPicker11.setRange(1900, calendar.get(Calendar.YEAR));
			monthPicker11.setRange(1, 12);
		} else {
			yearPicker.setRange(1900, calendar.get(Calendar.YEAR));
			monthPicker.setRange(1, 12);
		}
		completedCheck.setChecked(station.isCompleted());
		int value = Station.calcDay(date);
		boolean disabled = value == Station.UNANBIGUOUS_DAY;
		if(use11) {
			dayPicker11.setRange(1, 31);
			dayPicker11.setValue(disabled ? 1 : value);
			dayPicker11.setEnabled(!disabled);
		} else {
			dayPicker.setRange(1, 31);
			dayPicker.setValue(disabled ? 1 : value);
			dayPicker.setEnabled(!disabled);
		}
		day0Button.setChecked(disabled);
		value = Station.calcMonth(date);
		disabled = value == Station.UNANBIGUOUS_MONTH;
		if(use11) {
			monthPicker11.setValue(disabled ? 1 : value);
			monthPicker11.setEnabled(!disabled);
		} else {
			monthPicker.setValue(disabled ? 1 : value);
			monthPicker.setEnabled(!disabled);
		}
		month0Button.setChecked(disabled);
		value = Station.calcYear(date);
		disabled = value == Station.UNANBIGUOUS_YEAR;
		if(use11) {
			yearPicker11.setValue(disabled ? calendar.get(Calendar.YEAR) : value);
			yearPicker11.setEnabled(!disabled);
		} else {
			yearPicker.setValue(disabled ? calendar.get(Calendar.YEAR) : value);
			yearPicker.setEnabled(!disabled);
		}
		year0Button.setChecked(disabled);
		initializing = false;
		setDayRange();
		updateVisual();
		getDialog().setTitle(getString(R.string.verbose_edit_comp_date_title, station.getTitle()));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_STATION, station);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		updatePreference();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch(which) {
		case DialogInterface.BUTTON_POSITIVE:
			station.setCompletionDate(completedCheck.isChecked() ? station.getCompletionDate() : Station.INCOMPLETE);
			((StationFragment)getTargetFragment()).updateCompletionDate(station.getCompletionDate());
			dialog.cancel();
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			dialog.cancel();
			break;
		}
	}

	@Override
	public void onValueChange(NumberPickerCompat picker, int oldVal, int newVal) {
		if(initializing) {
			return;
		}
		if(picker.equals(dayPicker)) {
			updateCompletionDate();
		} else {
			setDayRange();
		}
	}

	@Override
	public void onValueChange(NumberPicker11 picker, int oldVal, int newVal) {
		if(initializing) {
			return;
		}
		if(picker.equals(dayPicker11)) {
			updateCompletionDate();
		} else {
			setDayRange();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(initializing) {
			return;
		}
		if(buttonView.equals(year0Button)) {
			if(use11) {
				yearPicker11.setEnabled(!isChecked);
			} else {
				yearPicker.setEnabled(!isChecked);
			}
			if(isChecked) {
				completionDate.requestFocus();
				month0Button.setChecked(true);
				day0Button.setChecked(true);
			}
		} else if(buttonView.equals(month0Button)) {
			if(use11) {
				monthPicker11.setEnabled(!isChecked);
			} else {
				monthPicker.setEnabled(!isChecked);
			}
			if(isChecked) {
				completionDate.requestFocus();
				day0Button.setChecked(true);
			} else {
				year0Button.setChecked(false);
			}
		} else if(buttonView.equals(day0Button)) {
			if(use11) {
				dayPicker11.setEnabled(!isChecked);
			} else {
				dayPicker.setEnabled(!isChecked);
			}
			if(isChecked) {
				completionDate.requestFocus();
			} else {
				year0Button.setChecked(false);
				month0Button.setChecked(false);
			}
		}
		updateCompletionDate();
	}

	private void setDate() {
		if(station.isCompleted()) {
			date = station.getCompletionDate();
		} else {
			date = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(PreferenceKey.RECENT_COMPLETION_DATE, 0);
			if(date == 0) {
				date = Station.calcCompletionDateInt(new Date());
			}
		}
	}

	private void updatePreference() {
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt(PreferenceKey.RECENT_COMPLETION_DATE, date).apply();
	}
	private void setDayRange() {
		int year = use11 ? yearPicker11.getValue() : yearPicker.getValue();
		int month = use11 ? monthPicker11.getValue() : monthPicker.getValue();
		int day = use11 ? dayPicker11.getValue() : dayPicker.getValue();
		int maxDay;
		switch(month) {
		case 2:
			if(year % 4 != 0) {
				maxDay = 28;
			} else if(year % 100 != 0) {
				maxDay = 29;
			} else {
				maxDay = year % 400 != 0 ? 29 : 28;
			}
			break;
		case 4: case 6: case 9: case 11:
			maxDay = 30;
			break;
		default:
			maxDay = 31;
			break;
		}
		if(use11) {
			dayPicker11.setRange(1, maxDay);
			dayPicker11.setValue(day <= maxDay ? day : maxDay);
		} else {
			dayPicker.setRange(1, maxDay);
			dayPicker.setValue(day <= maxDay ? day : maxDay);
		}
		updateCompletionDate();
	}

	private void updateCompletionDate() {
		int year = use11 ? yearPicker11.getValue() : yearPicker.getValue();
		int month = use11 ? monthPicker11.getValue() : monthPicker.getValue();
		int day = use11 ? dayPicker11.getValue() : dayPicker.getValue();
		date = Station.calcCompletionDateInt(year, month, day);
		station.setCompletionDate(completedCheck.isChecked() ? Station.calcCompletionDateInt(
				year0Button.isChecked() ? Station.UNANBIGUOUS_YEAR : year,
				month0Button.isChecked() ? Station.UNANBIGUOUS_MONTH : month,
				day0Button.isChecked() ? Station.UNANBIGUOUS_DAY : day) : Station.INCOMPLETE);
		updateVisual();
	}
	private void updateVisual() {
		completionDate.setText(station.getCompletionDateString(getResources()));
	}

}
