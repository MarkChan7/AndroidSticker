package com.markchan.andrroidsticker;

import android.graphics.Canvas;

/**
 * Created by Mark on 2017/6/29.
 */
public interface Sticker {

    void draw(Canvas canvas);

    void drag(float dx, float dy);

    void scale(float scaleFactor, float focusX, float focusY);

    void rotate(float degrees);

    void delete();
}
