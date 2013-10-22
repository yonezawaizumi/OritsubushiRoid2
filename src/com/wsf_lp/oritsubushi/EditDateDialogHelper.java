package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.Station;
import com.wsf_lp.oritsubushi.customs.NumberPickerCompat;

public class EditDateDialogHelper
	implements ToggleButton.OnCheckedChangeListener,
		NumberPickerCompat.OnValueChangeListener,
		DialogInterface.OnClickListener,
		DialogInterface.OnCancelListener {

	private WeakReference<VerboseActivity> activity;
	private Station station;
	private int date;
	private TextView completionDate;
	private NumberPickerCompat yearPicker;
	private NumberPickerCompat monthPicker;
	private NumberPickerCompat dayPicker;
	private ToggleButton year0Button;
	private ToggleButton month0Button;
	private ToggleButton day0Button;
	private CheckBox completedCheck;
	public Dialog createDialog(final VerboseActivity activity_) {
		activity = new WeakReference<VerboseActivity>(activity_);
		final View containerView = ((LayoutInflater)activity_.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.comp_date_edit_dialog, null, false);
		completionDate = (TextView)containerView.findViewById(R.id.comp_date_date);
		yearPicker = (NumberPickerCompat)containerView.findViewById(R.id.comp_date_edit_year);
		monthPicker = (NumberPickerCompat)containerView.findViewById(R.id.comp_date_edit_month);
		dayPicker = (NumberPickerCompat)containerView.findViewById(R.id.comp_date_edit_day);
		year0Button = (ToggleButton)containerView.findViewById(R.id.comp_date_edit_0year);
		month0Button = (ToggleButton)containerView.findViewById(R.id.comp_date_edit_0month);
		day0Button = (ToggleButton)containerView.findViewById(R.id.comp_date_edit_0day);
		completedCheck = (CheckBox)containerView.findViewById(R.id.comp_date_edit_incomp_button);
		yearPicker.setOnValueChangedListener(this);
		monthPicker.setOnValueChangedListener(this);
		dayPicker.setOnValueChangedListener(this);
		year0Button.setOnCheckedChangeListener(this);
		month0Button.setOnCheckedChangeListener(this);
		day0Button.setOnCheckedChangeListener(this);
		completedCheck.setOnCheckedChangeListener(this);
		return new AlertDialog.Builder(activity_)
			.setTitle(R.string.verbose_edit_comp_date_title)
			.setView(containerView)
			.setPositiveButton(R.string.ok, this)
			.setNegativeButton(R.string.cancel, this)
			.setOnCancelListener(this)
			.create();
	}
	@Override
	public void onClick(DialogInterface dialog, int whichButton) {
		final VerboseActivity activity = this.activity.get();
		if(activity == null) {
			return;
		}
		switch(whichButton) {
		case DialogInterface.BUTTON_POSITIVE:
			activity.getStation().setCompletionDate(completedCheck.isChecked() ? station.getCompletionDate() : Station.INCOMPLETE);
			activity.getDatabaseService().callDatabase(activity, Database.MethodName.UPDATE_COMPLETION, activity.getStation());
			activity.updateText();
			updatePreference();
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			dialog.cancel();
			return;
		}
	}
	@Override
	public void onCancel(DialogInterface dialog) {
		updatePreference();
	}

	private boolean initializing;

	private void setDate() {
		if(station.isCompleted()) {
			date = station.getCompletionDate();
		} else {
			date = PreferenceManager.getDefaultSharedPreferences(activity.get()).getInt(PreferenceKey.RECENT_COMPLETION_DATE, 0);
			if(date == 0) {
				date = Station.calcCompletionDateInt(new Date());
			}
		}
	}
	public void prepareDialog(Dialog dialog) {
		final VerboseActivity activity = this.activity.get();
		if(activity == null) {
			return;
		}
		initializing = true;
		station = new Station(activity.getStation());
		setDate();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		yearPicker.setRange(1900, calendar.get(Calendar.YEAR));
		monthPicker.setRange(1, 12);
		completedCheck.setChecked(station.isCompleted());
		int value = Station.calcDay(date);
		boolean disabled = value == Station.UNANBIGUOUS_DAY;
		dayPicker.setValue(disabled ? 1 : value);
		dayPicker.setEnabled(!disabled);
		day0Button.setChecked(disabled);
		value = Station.calcMonth(date);
		disabled = value == Station.UNANBIGUOUS_MONTH;
		monthPicker.setValue(disabled ? 1 : value);
		monthPicker.setEnabled(!disabled);
		month0Button.setChecked(disabled);
		value = Station.calcYear(date);
		disabled = value == Station.UNANBIGUOUS_YEAR;
		yearPicker.setValue(disabled ? calendar.get(Calendar.YEAR) : value);
		yearPicker.setEnabled(!disabled);
		year0Button.setChecked(disabled);
		initializing = false;
		setDayRange();
		updateVisual();
		dialog.setTitle(activity.getString(R.string.verbose_edit_comp_date_title, station.getTitle()));
	}
	private void updatePreference() {
		final VerboseActivity activity = this.activity.get();
		if(activity != null) {
			PreferenceManager.getDefaultSharedPreferences(activity).edit().putInt(PreferenceKey.RECENT_COMPLETION_DATE, date).commit();
		}
	}
	private void setDayRange() {
		int year = yearPicker.getValue();
		int month = monthPicker.getValue();
		int day = dayPicker.getValue();
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
		dayPicker.setRange(1, maxDay);
		dayPicker.setValue(day <= maxDay ? day : maxDay);
		updateCompletionDate();
	}

	private void updateCompletionDate() {
		final int year = yearPicker.getValue();
		final int month = monthPicker.getValue();
		final int day = dayPicker.getValue();
		date = Station.calcCompletionDateInt(year, month, day);
		station.setCompletionDate(completedCheck.isChecked() ? Station.calcCompletionDateInt(
				year0Button.isChecked() ? Station.UNANBIGUOUS_YEAR : year,
				month0Button.isChecked() ? Station.UNANBIGUOUS_MONTH : month,
				day0Button.isChecked() ? Station.UNANBIGUOUS_DAY : day) : Station.INCOMPLETE);
		updateVisual();
	}
	private void updateVisual() {
		final VerboseActivity activity = this.activity.get();
		if(activity != null) {
			completionDate.setText(station.getCompletionDateString(activity.getResources()));
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
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(initializing) {
			return;
		}
		if(buttonView.equals(year0Button)) {
			yearPicker.setEnabled(!isChecked);
			if(isChecked) {
				completionDate.requestFocus();
				month0Button.setChecked(true);
				day0Button.setChecked(true);
			}
		} else if(buttonView.equals(month0Button)) {
			monthPicker.setEnabled(!isChecked);
			if(isChecked) {
				completionDate.requestFocus();
				day0Button.setChecked(true);
			} else {
				year0Button.setChecked(false);
			}
		} else if(buttonView.equals(day0Button)) {
			dayPicker.setEnabled(!isChecked);
			if(isChecked) {
				completionDate.requestFocus();
			} else {
				year0Button.setChecked(false);
				month0Button.setChecked(false);
			}
		}
		updateCompletionDate();
	}
}
