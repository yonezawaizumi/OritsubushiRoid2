package com.wsf_lp.oritsubushi.customs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.ImageButton;


import com.wsf_lp.oritsubushi.R;

public class NumberPickerButton extends ImageButton {

    private NumberPickerCompat mNumberPicker;

    public NumberPickerButton(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public NumberPickerButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPickerButton(final Context context) {
        super(context);
    }

    public void setNumberPicker(final NumberPickerCompat picker) {
        mNumberPicker = picker;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        cancelLongpressIfRequired(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTrackballEvent(final MotionEvent event) {
        cancelLongpressIfRequired(event);
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER) || (keyCode == KeyEvent.KEYCODE_ENTER)) {
            cancelLongpress();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void cancelLongpressIfRequired(final MotionEvent event) {
        if ((event.getAction() == MotionEvent.ACTION_CANCEL)
            || (event.getAction() == MotionEvent.ACTION_UP)) {
            cancelLongpress();
        }
    }

    private void cancelLongpress() {
        if (R.id.increment == getId()) {
            mNumberPicker.cancelIncrement();
        } else if (R.id.decrement == getId()) {
            mNumberPicker.cancelDecrement();
        }
    }
}
