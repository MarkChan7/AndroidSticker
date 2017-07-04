package com.markchan.andrroidsticker;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Mark on 2017/6/29.
 */
public class StickerView extends View implements Sticker {

    public StickerView(Context context) {
        this(context, null);
    }

    public StickerView(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public StickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {

    }

    @Override
    public void drag(float dx, float dy) {

    }

    @Override
    public void scale(float scaleFactor, float focusX, float focusY) {

    }

    @Override
    public void rotate(float degrees) {

    }

    @Override
    public void delete() {

    }
}
