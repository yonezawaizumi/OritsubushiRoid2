package com.wsf_lp.oritsubushi.customs;

import com.wsf_lp.android.AndroidUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * サイズ自動調整TextView
 *
 */
public class FontFitTextView extends TextView {
    /** 最小のテキストサイズ(ほぼ横幅raw pixel) */
    public static final int DEFAULT_MIN_TEXT_SIZE = 10;

    private float minTextSize;

    private static Class<?> resourceClass;
    private static String myClassName;

    public float getMinTextSize() { return minTextSize; }
    public void setMinTextSize(float minTextSize) {
    	this.minTextSize = minTextSize;
    	resize();
    }

    /**
     * コンストラクタ
     * @param context
     */
    public FontFitTextView(Context context) {
        super(context);
        minTextSize = DEFAULT_MIN_TEXT_SIZE;
    }

    /**
     * コンストラクタ
     * @param context
     * @param attrs
     * @throws ClassNotFoundException 
     * @throws NoSuchFieldException 
     * @throws IllegalAccessException 
     * @throws SecurityException 
     * @throws IllegalArgumentException 
     */
    public FontFitTextView(Context context, AttributeSet attrs) throws ClassNotFoundException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
        super(context, attrs);
        //try {
	        if(resourceClass == null) {
	        	Class<?> clazz = getClass();
	        	resourceClass = Class.forName(AndroidUtils.getResourceClassName(clazz.getCanonicalName()) + "$styleable");
	        	myClassName = clazz.getSimpleName();
	        }
	        // styleable から TypedArray の取得
	        TypedArray tArray = context.obtainStyledAttributes(attrs, (int[]) resourceClass.getField(myClassName).get(null));

	        // TypedArray から String を取得
	        minTextSize = tArray.getDimensionPixelSize(resourceClass.getField(myClassName + "_minTextSize").getInt(null), DEFAULT_MIN_TEXT_SIZE);
        /*} catch(ClassNotFoundException e) {
        	e.printStackTrace();
        } catch(NoSuchFieldException e) {
        	e.printStackTrace();
        } catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
        throw new RuntimeException("bad or illegal resource class R for class: " + myClassName);*/
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        resize();
    }

    /**
     * テキストサイズ調整
     */
    private void resize() {

        Paint paint = new Paint();

        // Viewの幅
        int viewWidth = getWidth();
        // テキストサイズ
        float textSize = getTextSize();

        // Paintにテキストサイズ設定
        paint.setTextSize(textSize);
        // テキストの横幅取得
        float textWidth = paint.measureText(this.getText().toString());

        while (viewWidth <  textWidth) {
            // 横幅に収まるまでループ

            if (minTextSize >= textSize) {
                // 最小サイズ以下になる場合は最小サイズ
                textSize = minTextSize;
                break;
            }

            // テキストサイズをデクリメント
            --textSize;

            // Paintにテキストサイズ設定
            paint.setTextSize(textSize);
            // テキストの横幅を再取得
            textWidth = paint.measureText(getText().toString());

        }

        // テキストサイズ設定
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

}
