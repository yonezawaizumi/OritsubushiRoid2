package com.wsf_lp.oritsubushi.customs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.NumberPicker;

@SuppressLint("NewApi")
public class NumberPicker11 extends NumberPicker {

	public NumberPicker11(Context context) {
		super(context);
	}

	public NumberPicker11(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NumberPicker11(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public interface OnValueChangeListener11 {
		public void onValueChange(NumberPicker11 picker, int oldVal, int newVal);
	}

	private static class OnValueChangeListerImpl implements NumberPicker.OnValueChangeListener {
		private OnValueChangeListener11 listener;
		public OnValueChangeListerImpl(OnValueChangeListener11 listener) { this.listener = listener; }
		@Override
		public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
			listener.onValueChange((NumberPicker11)picker, oldVal, newVal);
		}
	}

	public void setOnValueChangedListener(OnValueChangeListener11 listener) {
		super.setOnValueChangedListener(new OnValueChangeListerImpl(listener));
	}

	public void setRange(int start, int end) {
		setMinValue(start);
		setMaxValue(end);
	}
}
