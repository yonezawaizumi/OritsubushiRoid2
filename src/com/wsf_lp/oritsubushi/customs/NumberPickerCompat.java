package com.wsf_lp.oritsubushi.customs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wsf_lp.oritsubushi.R;

public class NumberPickerCompat extends LinearLayout implements OnClickListener, OnFocusChangeListener,
        OnLongClickListener {

    private static final String TAG = "NumberPicker";

    private static final String NUMBER_PICKER_CLASS_NAME;
    private static final Method SET_BACKGROUND_METHOD;

    static {
        final int sdkVersion = Build.VERSION.SDK_INT;
        // 8=Build.VERSION_CODES.FROYO
        if (sdkVersion < 8) {
            NUMBER_PICKER_CLASS_NAME = "com.android.internal.widget.NumberPicker";
        } else {
            NUMBER_PICKER_CLASS_NAME = "android.widget.NumberPicker";
        }
        String methodName;
        if (sdkVersion < 16) {
        	methodName = "setBackgroundDrawable";
        } else {
        	methodName = "setBackground";
        }
    	try {
			SET_BACKGROUND_METHOD = View.class.getMethod(methodName, new Class[]{ Drawable.class });
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e.toString());
		}
    }

    public interface OnValueChangeListener {
        void onValueChange(NumberPickerCompat picker, int oldVal, int newVal);
    }

    public interface Formatter {
        String toString(int value);
    }

    /*
     * Use a custom NumberPicker formatting callback to use two-digit minutes strings like "01". Keeping a static
     * formatter etc. is the most efficient way to do this; it avoids creating temporary objects on every call to
     * format().
     */
    public static final NumberPickerCompat.Formatter TWO_DIGIT_FORMATTER = new NumberPickerCompat.Formatter() {
        final StringBuilder mBuilder = new StringBuilder();
        final java.util.Formatter mFmt = new java.util.Formatter(mBuilder);
        final Object[] mArgs = new Object[1];

        public String toString(final int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }
    };

    private final Handler mHandler;
    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mIncrement) {
                changeCurrent(mCurrent + calcIncrementValue(mCurrent, mIncrementValue));
                mHandler.postDelayed(this, mSpeed);
            } else if (mDecrement) {
                changeCurrent(mCurrent - calcDecrementValue(mCurrent, mIncrementValue));
                mHandler.postDelayed(this, mSpeed);
            }
        }
    };

    private final EditText mText;
    private final InputFilter mNumberInputFilter;

    private String[] mDisplayedValues;
    private int mStart;
    private int mEnd;
    private int mCurrent;
    private int mPrevious;
    private OnValueChangeListener mListener;
    private Formatter mFormatter;
    private long mSpeed = 300;

    private boolean mIncrement;
    private boolean mDecrement;

    private int mIncrementValue = 1;

    private boolean mButtonsBackgroundInitilized;

    public NumberPickerCompat(final Context context) {
        this(context, null);
    }

    public NumberPickerCompat(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPickerCompat(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs);
        setOrientation(VERTICAL);
        final LayoutInflater inflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.number_picker, this, true);
        mHandler = new Handler();
        final InputFilter inputFilter = new NumberPickerInputFilter();
        mNumberInputFilter = new NumberRangeKeyListener();
        mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(this);
        mIncrementButton.setOnLongClickListener(this);
        mIncrementButton.setNumberPicker(this);
        mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(this);
        mDecrementButton.setOnLongClickListener(this);
        mDecrementButton.setNumberPicker(this);

        mText = (EditText) findViewById(R.id.timepicker_input);
        mText.setOnFocusChangeListener(this);
        mText.setFilters(new InputFilter[] { inputFilter });
        mText.setRawInputType(InputType.TYPE_CLASS_NUMBER);

        if (!isEnabled()) {
            setEnabled(false);
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r,
            final int b) {
        super.onLayout(changed, l, t, r, b);
        setWidgetResource();
    }

    /**
     * Android標準のNumberPickerのシステムリソースをカスタムNumberPickerに適用する。
     */
    protected void setWidgetResource() {
        if (mButtonsBackgroundInitilized) {
            return;
        }

        try {
            final Context context = getContext();
            final ClassLoader cl = context.getClassLoader();
            final Class<?> clazz = cl.loadClass(NUMBER_PICKER_CLASS_NAME);
            final Constructor<?> constructor = clazz.getConstructor(Context.class);
            final Object obj = constructor.newInstance(context);
            final Class<?> c = obj.getClass();

            {
                final Field field = c.getDeclaredField("mIncrementButton");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                final ImageButton internalIncrementButton = (ImageButton) field.get(obj);
                SET_BACKGROUND_METHOD.invoke(mIncrementButton, internalIncrementButton.getBackground());
            }

            {
            	//TODO: APIバージョン見て切り分けるべし
                Field field = null;
                try {
                    field = c.getDeclaredField("mText");
                } catch (final Exception eee) {
                    field = c.getDeclaredField("mInputText");
                }
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                final EditText internalText = (EditText) field.get(obj);
                SET_BACKGROUND_METHOD.invoke(mText, internalText.getBackground());
                mText.setTextColor(internalText.getTextColors());
                // TextSizeを適用すると Android 2.2 800x480(hdpi) で、テキストが大きくなってしまうので、コメントアウト。
                //mText.setTextSize(internalText.getTextSize());
            }

            {
                final Field field = c.getDeclaredField("mDecrementButton");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                final ImageButton internalDecrementButton = (ImageButton) field.get(obj);
                SET_BACKGROUND_METHOD.invoke(mDecrementButton, internalDecrementButton.getBackground());
            }
            mButtonsBackgroundInitilized = true;
        } catch (final Exception ex) {
            Log
                .e(
                    TAG,
                    "com.android.internal.widget.NumberPicker internal button background resource got not.",
                    ex);
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        mIncrementButton.setEnabled(enabled);
        mDecrementButton.setEnabled(enabled);
        mText.setEnabled(enabled);
    }

    public void setOnValueChangedListener(final OnValueChangeListener listener) {
        mListener = listener;
    }

    public void setFormatter(final Formatter formatter) {
        mFormatter = formatter;
    }

    /**
     * Set the range of numbers allowed for the number picker. The current value will be automatically set to the start.
     *
     * @param start the start of the range (inclusive)
     * @param end the end of the range (inclusive)
     */
    public void setRange(final int start, final int end) {
        mStart = start;
        mEnd = end;
        mCurrent = start;
        updateView();
    }

    /**
     * Set the range of numbers allowed for the number picker. The current value will be automatically set to the start.
     * Also provide a mapping for values used to display to the user.
     *
     * @param start the start of the range (inclusive)
     * @param end the end of the range (inclusive)
     * @param displayedValues the values displayed to the user.
     */
    public void setRange(final int start, final int end, final String[] displayedValues) {
        mDisplayedValues = displayedValues;
        mStart = start;
        mEnd = end;
        mCurrent = start;
        updateView();
    }

    public void setValue(final int current) {
        mCurrent = current;
        updateView();
    }

    /**
     * The speed (in milliseconds) at which the numbers will scroll when the the +/- buttons are longpressed. Default is
     * 300ms.
     */
    public void setSpeed(final long speed) {
        mSpeed = speed;
    }

    public void onClick(final View v) {
        validateInput(mText);
        if (!mText.hasFocus()) {
            mText.requestFocus();
        }

        // now perform the increment/decrement
        if (R.id.increment == v.getId()) {
            changeCurrent(mCurrent + calcIncrementValue(mCurrent, mIncrementValue));
        } else if (R.id.decrement == v.getId()) {
            changeCurrent(mCurrent - calcDecrementValue(mCurrent, mIncrementValue));
        }
    }

    private static int calcIncrementValue(final int current, final int incrementValue) {
        if (current == 0) {
            return incrementValue;
        }
        final int v = current % incrementValue;
        if (v == 0) {
            return incrementValue;
        }
        return incrementValue - v;
    }

    private static int calcDecrementValue(final int current, final int incrementValue) {
        if (current == 0) {
            return incrementValue;
        }
        final int v = current % incrementValue;
        if (v == 0) {
            return incrementValue;
        }
        return v;
    }

    private String formatNumber(final int value) {
        return (mFormatter != null) ? mFormatter.toString(value) : String.valueOf(value);
    }

    private void changeCurrent(final int changedValue) {
        int current = changedValue;

        // Wrap around the values if we go past the start or end
        if (current > mEnd) {
            current = mStart;
        } else if (current < mStart) {
            current = mEnd - (mEnd % mIncrementValue);
        }
        mPrevious = mCurrent;
        mCurrent = current;
        notifyChange();
        updateView();
    }

    private void notifyChange() {
        if (mListener != null) {
            mListener.onValueChange(this, mPrevious, mCurrent);
        }
    }

    private void updateView() {

        /*
         * If we don't have displayed values then use the current number else find the correct value in the displayed
         * values for the current number.
         */
        if (mDisplayedValues == null) {
            mText.setText(formatNumber(mCurrent));
        } else {
            mText.setText(mDisplayedValues[mCurrent - mStart]);
        }
        mText.setSelection(mText.getText().length());
    }

    private void validateCurrentView(final CharSequence str) {
        final int val = getSelectedPos(str.toString());
        if ((val >= mStart) && (val <= mEnd)) {
            if (mCurrent != val) {
                mPrevious = mCurrent;
                mCurrent = val;
                notifyChange();
            }
        }
        updateView();
    }

    public void onFocusChange(final View v, final boolean hasFocus) {

        /*
         * When focus is lost check that the text field has valid values.
         */
        if (!hasFocus) {
            validateInput(v);
            //add yizumi 20121121
			((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void validateInput(final View v) {
        final String str = String.valueOf(((TextView) v).getText());
        if ("".equals(str)) {

            // Restore to the old value as we don't allow empty values
            updateView();
        } else {

            // Check the new value and ensure it's in range
            validateCurrentView(str);
        }
    }

    /**
     * We start the long click here but rely on the {@link NumberPickerButton} to inform us when the long click has
     * ended.
     */
    public boolean onLongClick(final View v) {

        /*
         * The text view may still have focus so clear it's focus which will trigger the on focus changed and any typed
         * values to be pulled.
         */
        mText.clearFocus();

        if (R.id.increment == v.getId()) {
            mIncrement = true;
            mHandler.post(mRunnable);
        } else if (R.id.decrement == v.getId()) {
            mDecrement = true;
            mHandler.post(mRunnable);
        }
        return true;
    }

    public void cancelIncrement() {
        mIncrement = false;
    }

    public void cancelDecrement() {
        mDecrement = false;
    }

    private static final char[] DIGIT_CHARACTERS = new char[] {
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9' };

    private final NumberPickerButton mIncrementButton;
    private final NumberPickerButton mDecrementButton;

    private class NumberPickerInputFilter implements InputFilter {
        @SuppressLint("DefaultLocale") public CharSequence filter(final CharSequence source, final int start, final int end,
                final Spanned dest, final int dstart, final int dend) {
            if (mDisplayedValues == null) {
                return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
            }
            final CharSequence filtered = String.valueOf(source.subSequence(start, end));
            final String result =
                String.valueOf(dest.subSequence(0, dstart))
                    + filtered
                    + dest.subSequence(dend, dest.length());
            final String str = String.valueOf(result).toLowerCase(Locale.ENGLISH);
            for (final String val : mDisplayedValues) {
                if (val.toLowerCase(Locale.ENGLISH).startsWith(str)) {
                    return filtered;
                }
            }
            return "";
        }
    }

    private class NumberRangeKeyListener extends NumberKeyListener {

        // XXX This doesn't allow for range limits when controlled by a
        // soft input method!
        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }

        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(final CharSequence source, final int start, final int end,
                final Spanned dest, final int dstart, final int dend) {

            CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }

            final String result =
                String.valueOf(dest.subSequence(0, dstart))
                    + filtered
                    + dest.subSequence(dend, dest.length());

            if ("".equals(result)) {
                return result;
            }
            final int val = getSelectedPos(result);

            /*
             * Ensure the user can't type in a value greater than the max allowed. We have to allow less than min as the
             * user might want to delete some numbers and then type a new number.
             */
            if (val > mEnd) {
                return "";
            } else {
                return filtered;
            }
        }
    }

    private int getSelectedPos(final String str) {
        if (mDisplayedValues == null) {
            return Integer.parseInt(str);
        } else {
            for (int i = 0; i < mDisplayedValues.length; i++) {

                /* Don't force the user to type in jan when ja will do */
                if (mDisplayedValues[i].toLowerCase(Locale.ENGLISH).startsWith(str.toLowerCase(Locale.ENGLISH))) {
                    return mStart + i;
                }
            }

            /*
             * The user might have typed in a number into the month field i.e. 10 instead of OCT so support that too.
             */
            try {
                return Integer.parseInt(str);
            } catch (final NumberFormatException e) {

                /* Ignore as if it's not a number we don't care */
            }
        }
        return mStart;
    }

    /**
     * @return the current value.
     */
    public int getValue() {
        return mCurrent;
    }

    public void setIncrementValue(final int incrementValue) {
        mIncrementValue = incrementValue;
    }
}