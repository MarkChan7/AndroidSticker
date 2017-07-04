package com.markchan.andrroidsticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Mark on 2017/6/29.
 */
public class StickerAttacher implements Sticker, View.OnTouchListener {

    public static class Component {

        private Bitmap mBitmap;
        private int mBitmapWidth;
        private int mBitmapHeight;

        public Component() {
        }

        public Component(Bitmap bitmap, int bitmapWidth, int bitmapHeight) {
            mBitmap = bitmap;
            mBitmapWidth = bitmapWidth;
            mBitmapHeight = bitmapHeight;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public void setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public int getBitmapWidth() {
            return mBitmapWidth;
        }

        public void setBitmapWidth(int bitmapWidth) {
            mBitmapWidth = bitmapWidth;
        }

        public int getBitmapHeight() {
            return mBitmapHeight;
        }

        public void setBitmapHeight(int bitmapHeight) {
            mBitmapHeight = bitmapHeight;
        }
    }

    private static final int CTR_LEFT_TOP = 0;
    private static final int CTR_RIGHT_BOTTOM = 2;
    private static final int CTR_MID_MID = 4;
    private static final int CTR_NONE = -1;

    private static final int OPT_DEAULT = -1;
    private static final int OPT_DRAG = 0;
    private static final int OPT_SCALE = 1;
    private static final int OPT_ROTATE = 2;
    private static final int OPT_SELECT = 3;

    private Context mContext;

    private int mCurrCtr = CTR_NONE;

    private int mLastOpt = OPT_SELECT;

    private boolean mSelected = true;
    private boolean mActive = true;

    private final Component mStickerComponent;
    private final Component mDeleteComponent;
    private final Component mControllerComponent;

    private final float[] mSrcPointArr;
    private final float[] mDestPointArr;

    private final Matrix mMatrix;

    private final Point mPrePrivot;
    private final Point mPostPrivot;

    private final Point mLastPoint;

    private final Paint mPaint;
    private final Paint mFramePaint;

    public StickerAttacher(Context context, Component stickerComponent, Component deleteComponent,
            Component controllerComponent) {
        mContext = context;
        mStickerComponent = stickerComponent;
        mDeleteComponent = deleteComponent;
        mControllerComponent = controllerComponent;
        mSrcPointArr = new float[]{
                0, 0,
                stickerComponent.getBitmapWidth(), 0,
                stickerComponent.getBitmapWidth(), stickerComponent.getBitmapHeight(),
                0, stickerComponent.getBitmapHeight(),
                stickerComponent.getBitmapWidth() / 2, stickerComponent.getBitmapHeight() / 2
        };
        mDestPointArr = mSrcPointArr.clone();
        mMatrix = new Matrix();

        mPrePrivot = new Point(stickerComponent.getBitmapWidth() / 2,
                stickerComponent.getBitmapHeight() / 2);
        mPostPrivot = new Point(stickerComponent.getBitmapWidth() / 2,
                stickerComponent.getBitmapHeight() / 2);

        mLastPoint = new Point();

        mPaint = new Paint();
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(Color.WHITE);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mActive) {
            return;
        }
        canvas.drawBitmap(mStickerComponent.getBitmap(), mMatrix, mPaint);
        if (mSelected) {
            drawFrame(canvas);
            drawActionComponent(canvas);
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawLine(mDestPointArr[0], mDestPointArr[1], mDestPointArr[2], mDestPointArr[3],
                mFramePaint);
        canvas.drawLine(mDestPointArr[2], mDestPointArr[3], mDestPointArr[4], mDestPointArr[5],
                mFramePaint);
        canvas.drawLine(mDestPointArr[4], mDestPointArr[5], mDestPointArr[6], mDestPointArr[7],
                mFramePaint);
        canvas.drawLine(mDestPointArr[0], mDestPointArr[1], mDestPointArr[6], mDestPointArr[7],
                mFramePaint);
    }

    private void drawActionComponent(Canvas canvas) {
        canvas.drawBitmap(mDeleteComponent.getBitmap(),
                mDestPointArr[0] - mDeleteComponent.getBitmapWidth() / 2,
                mDestPointArr[1] - mDeleteComponent.getBitmapHeight() / 2, mPaint);
        canvas.drawBitmap(mControllerComponent.getBitmap(),
                mDestPointArr[4] - mControllerComponent.getBitmapWidth() / 2,
                mDestPointArr[5] - mControllerComponent.getBitmapHeight() / 2, mPaint);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (!isOnSticker(x, y) && getCurrCtr(x, y) == CTR_NONE) {
            mSelected = false;
            // 重绘
        } else if (getCurrCtr(x, y) == CTR_LEFT_TOP) {
            mActive = false;
            // 重绘
        } else {
            int option = OPT_DEAULT;
            option = getCurrOpt(event);
            switch (option) {
                case OPT_DRAG:
                    if (isOnSticker(x, y)) {
                        drag(x, y);
                    }
                    break;
                case OPT_ROTATE:
                    rotate(event);
                    scale(event);
                    break;
                default:
                    break;
            }

            mLastPoint.x = x;
            mLastPoint.y = y;
            mLastOpt = option;
            mSelected = true;
            // 重绘
        }

        return true;
    }

    private void scale(MotionEvent event) {

    }

    private void rotate(MotionEvent event) {

    }

    private boolean isOnSticker(int x, int y) {
        Matrix inMatrix = new Matrix();
        mMatrix.invert(inMatrix);

        float[] tempPointArr = new float[]{0, 0};
        inMatrix.mapPoints(tempPointArr, new float[]{x, y});
        if (tempPointArr[0] > 0 && tempPointArr[0] < mStickerComponent.getBitmapWidth()
                && tempPointArr[1] > 0 && tempPointArr[1] < mStickerComponent.getBitmapHeight()) {
            return true;
        } else {
            return false;
        }
    }

    private int getCurrCtr(int x, int y) {
        Rect rect = new Rect(x - mControllerComponent.getBitmapWidth() / 2,
                y - mControllerComponent.getBitmapHeight()
                , x + mControllerComponent.getBitmapWidth() / 2,
                y + mControllerComponent.getBitmapHeight());
        int index = 0;
        for (int i = 0; i < mDestPointArr.length; i += 2) {
            if (rect.contains((int) mDestPointArr[i], (int) mDestPointArr[i + 1])) {
                return index;
            }
            ++index;
        }
        return CTR_NONE;
    }

    private int getCurrOpt(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int currOpt = mLastOpt;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrCtr = getCurrCtr(x, y);
                if (mCurrCtr != CTR_NONE || isOnSticker(x, y)) {
                    currOpt = OPT_SELECT;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrCtr == CTR_LEFT_TOP) {
                    delete();
                } else if (mCurrCtr == CTR_RIGHT_BOTTOM) {
                    currOpt = OPT_DRAG;
                } else if (mLastOpt == OPT_SELECT) {
                    currOpt = OPT_DRAG;
                }
                break;
            case MotionEvent.ACTION_UP:
                currOpt = OPT_SELECT;
                break;
            default:
                break;
        }
        return currOpt;
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
