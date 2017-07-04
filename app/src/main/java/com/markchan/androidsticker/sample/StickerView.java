package com.markchan.androidsticker.sample;

import static android.graphics.BitmapFactory.decodeResource;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.orhanobut.logger.Logger;

/**
 * 使用矩阵控制图片移动、缩放、旋转
 */
public class StickerView extends View {

    public static class Component {

        private Bitmap mBitmap;
        private int mBitmapWidth;
        private int mBitmapHeight;

        public Component() {
        }

        public Component(Bitmap bitmap) {
            mBitmap = bitmap;
            mBitmapWidth = bitmap.getWidth();
            mBitmapHeight = bitmap.getHeight();
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

        public int getHalfBitmapWidth() {
            return mBitmapWidth / 2;
        }

        public int getHalfBitmapHeight() {
            return mBitmapHeight / 2;
        }
    }

    private static final String TAG = "StickerView";

    /**
     * 操作
     */
    public static final int OPTION_DEFAULT = -1; // 默认
    public static final int OPTION_TRANSLATE = 0; // 移动
    public static final int OPTION_SCALE = 1; // 缩放
    public static final int OPTION_ROTATE = 2; // 旋转
    public static final int OPTION_SELECTED = 3; // 选择

    /**
     * 图片控制点
     * 0---------1
     * |         |
     * |    4    |
     * |         |
     * 3---------2
     */
    public static final int CONTROL_POINT_NONE = -1;
    public static final int CONTROL_POINT_LEFT_TOP = 0;
    public static final int CONTROL_POINT_RIGHT_BOTTOM = 2;
    public static final int CONTROL_POINT_CENTER = 4;

    private String mPhotoPath;

    private Component mStickerComponent;
    private Component mDeleteComponent;
    private Component mControllerComponent;

    /**
     * <pre>
     * 0, 1 ------ 2, 3
     *  |           |
     *  |    8, 9   |
     *  |           |
     * 6, 7 ------ 4, 5
     * </pre>
     */
    private float[] mSrcPointArr;
    private float[] mDstPointArr;

    private Matrix mMatrix;

    private Paint mPaint;
    private Paint mFramePaint;

    /** 位移值 */
    private float mDeltaX = 0.0F;
    private float mDeltaY = 0.0F;

    /** 贴图素材缩放值 */
    private float mScale = 1.0F;

    private float mDefaultDegrees;
    private float mPreDegrees;
    private float mLastDegrees;

    /** 上一次触摸点位置 */
    private Point mLastTouchPoint;

    /** 当前操作点对称点 */
    private Point mSymmetricPoint = new Point();
    /** 中心点 */
    private Point mCenterPoint = new Point();
    /** 旋转缩放点 */
    private Point mRightBottomPoint = new Point();

    public int mCurrControlPoint = CONTROL_POINT_NONE;
    public int mLastOption = OPTION_SELECTED;

    /** 是否选中 */
    private boolean mSelected = true;
    /** 是否删除 */
    private boolean mActive = true;

    public StickerView(Context context, String photoPath) {
        super(context);
        mPhotoPath = photoPath;
        init();
    }

    private void init() {
        mStickerComponent = new Component(BitmapFactory.decodeFile(mPhotoPath));
        mDeleteComponent = new Component(
                decodeResource(getResources(), R.drawable.ic_f_delete_normal));
        mControllerComponent = new Component(
                decodeResource(getResources(), R.drawable.ic_f_rotate_normal));

        int photoBitmapWidth = mStickerComponent.getBitmapWidth();
        int photoBitmapHeight = mStickerComponent.getBitmapHeight();
        int halfPhotoBitmapWidth = mStickerComponent.getHalfBitmapWidth();
        int halfPhotoBitmapHeight = mStickerComponent.getHalfBitmapHeight();
        mSrcPointArr = new float[]{
                0, 0,
                photoBitmapWidth, 0,
                photoBitmapWidth, photoBitmapHeight,
                0, photoBitmapHeight,
                halfPhotoBitmapWidth, halfPhotoBitmapHeight
        };
        mDstPointArr = mSrcPointArr.clone();

        mMatrix = new Matrix();

        mLastTouchPoint = new Point(0, 0);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(Color.WHITE);

        mDefaultDegrees = mLastDegrees = computeDegrees(
                new Point(photoBitmapWidth, photoBitmapHeight),
                new Point(halfPhotoBitmapWidth, halfPhotoBitmapHeight)
        );
        Logger.d("Default/Last degrees is %f", mDefaultDegrees);

        setMatrix(OPTION_DEFAULT);
    }

    /**
     * 计算两点与垂直方向夹角
     */
    public float computeDegrees(Point p1, Point p2) {
        float tran_x = p1.x - p2.x;
        float tran_y = p1.y - p2.y;
        float degrees = 0.0F;
        float angle = (float) (
                Math.asin(tran_x / Math.sqrt(tran_x * tran_x + tran_y * tran_y)) * 180 / Math.PI);
        if (!Float.isNaN(angle)) {
            if (tran_x >= 0 && tran_y <= 0) { // 第一象限
                degrees = angle;
            } else if (tran_x <= 0 && tran_y <= 0) { // 第二象限
                degrees = angle;
            } else if (tran_x <= 0 && tran_y >= 0) { // 第三象限
                degrees = -180 - angle;
            } else if (tran_x >= 0 && tran_y >= 0) { // 第四象限
                degrees = 180 - angle;
            }
        }
        return degrees;
    }

    /**
     * 矩阵变换，达到图形平移的目的
     */
    private void setMatrix(int option) {
        switch (option) {
            case OPTION_TRANSLATE:
                mMatrix.postTranslate(mDeltaX, mDeltaY);
                break;
            case OPTION_SCALE:
                mMatrix.postScale(
                        mScale, mScale,
                        mDstPointArr[8],
                        mDstPointArr[9]
                );
                break;
            case OPTION_ROTATE:
                mMatrix.postRotate(
                        mPreDegrees - mLastDegrees,
                        mDstPointArr[8],
                        mDstPointArr[9]
                );
                break;
            case OPTION_DEFAULT:
            default:
                break;
        }

        mMatrix.mapPoints(mDstPointArr, mSrcPointArr);
    }

    /**
     * 判断触摸点是否在贴图上
     */
    private boolean isOnPhoto(int x, int y) {
        // 获取逆向矩阵, 这是由于坐标系的问题
        Matrix invertMatrix = new Matrix();
        mMatrix.invert(invertMatrix);

        float[] dstPointArr = new float[]{0, 0};
        invertMatrix.mapPoints(dstPointArr, new float[]{x, y});
        if (dstPointArr[0] > 0 && dstPointArr[0] < mStickerComponent.getBitmapWidth()
                && dstPointArr[1] > 0 && dstPointArr[1] < mStickerComponent.getBitmapHeight()) {
            return true;
        } else {
            return false;
        }
    }

    private int getOption(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int curOption = mLastOption;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrControlPoint = getControlPoint(x, y);
                if (mCurrControlPoint != CONTROL_POINT_NONE || isOnPhoto(x, y)) {
                    curOption = OPTION_SELECTED;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrControlPoint == CONTROL_POINT_LEFT_TOP) {
                    // 删除饰品
                } else if (mCurrControlPoint == CONTROL_POINT_RIGHT_BOTTOM) {
                    curOption = OPTION_ROTATE;
                } else if (mLastOption == OPTION_SELECTED) {
                    curOption = OPTION_TRANSLATE;
                }
                break;
            case MotionEvent.ACTION_UP:
                curOption = OPTION_SELECTED;
                break;
            default:
                break;
        }
        return curOption;
    }

    /**
     * 判断点所在的控制点
     */
    private int getControlPoint(int x, int y) {
        Rect rect = new Rect(
                x - mControllerComponent.getHalfBitmapWidth(),
                y - mControllerComponent.getHalfBitmapHeight(),
                x + mControllerComponent.getHalfBitmapWidth(),
                y + mControllerComponent.getHalfBitmapHeight()
        );
        int res = 0;
        for (int i = 0; i < mDstPointArr.length; i += 2) {
            if (rect.contains((int) mDstPointArr[i], (int) mDstPointArr[i + 1])) {
                return res;
            }
            ++res;
        }
        return CONTROL_POINT_NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int evX = (int) event.getX();
        int evY = (int) event.getY();

        if (!isOnPhoto(evX, evY) && getControlPoint(evX, evY) == CONTROL_POINT_NONE) {
            mSelected = false;
            invalidate();
        } else if (getControlPoint(evX, evY) == CONTROL_POINT_LEFT_TOP) {
            mActive = false;
            invalidate();
        } else {
            int option = getOption(event);
            switch (option) {
                case OPTION_TRANSLATE:
                    if (isOnPhoto(evX, evY)) {
                        translate(evX, evY);
                    }
                    break;
                case OPTION_ROTATE:
                    rotate(event);
                    scale(event);
                    break;
                case OPTION_DEFAULT:
                case OPTION_SCALE:
                case OPTION_SELECTED:
                default:
                    break;
            }

            mLastTouchPoint.x = evX;
            mLastTouchPoint.y = evY;

            mLastOption = option;
            mSelected = true;

            invalidate();
        }

        return true;
    }

    /**
     * 移动
     */
    private void translate(int evx, int evy) {
        mDeltaX = evx - mLastTouchPoint.x;
        mDeltaY = evy - mLastTouchPoint.y;
        setMatrix(OPTION_TRANSLATE);
    }

    /**
     * 缩放
     * 0---1---2
     * |       |
     * 7   8   3
     * |       |
     * 6---5---4
     */
    private void scale(MotionEvent event) {
        int pointIndex = mCurrControlPoint * 2;

        float px = mDstPointArr[pointIndex];
        float py = mDstPointArr[pointIndex + 1];

        float evX = event.getX();
        float evY = event.getY();

        float oppositeX = mDstPointArr[pointIndex - 4];
        float oppositeY = mDstPointArr[pointIndex - 3];

        float temp1 = getDistanceOfTwoPoints(px, py, oppositeX, oppositeY);
        float temp2 = getDistanceOfTwoPoints(evX, evY, oppositeX, oppositeY);

        mScale = temp2 / temp1;
        mSymmetricPoint.x = (int) oppositeX;
        mSymmetricPoint.y = (int) oppositeY;
        mCenterPoint.x = (int) (mSymmetricPoint.x + px) / 2;
        mCenterPoint.y = (int) (mSymmetricPoint.y + py) / 2;
        mRightBottomPoint.x = (int) mDstPointArr[8];
        mRightBottomPoint.y = (int) mDstPointArr[9];
        Log.i(TAG, "Scale is " + mScale);
        if (getScale() < 0.3F && mScale < 1.0F) {
            // 限定最小缩放比为0.3
        } else {
            setMatrix(OPTION_SCALE);
        }
    }

    /**
     * 旋转图片
     * 0---1---2
     * |       |
     * 7   8   3
     * |       |
     * 6---5---4
     */
    private void rotate(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            mPreDegrees = computeDegrees(
                    new Point((int) event.getX(0), (int) event.getY(0)),
                    new Point((int) event.getX(1), (int) event.getY(1))
            );
        } else {
            mPreDegrees = computeDegrees(
                    new Point((int) event.getX(), (int) event.getY()),
                    new Point((int) mDstPointArr[8], (int) mDstPointArr[9])
            );
        }
        setMatrix(OPTION_ROTATE);
        mLastDegrees = mPreDegrees;
    }

    /**
     * 计算两个点之间的距离
     */
    private float getDistanceOfTwoPoints(Point p1, Point p2) {
        return (float) (Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
    }

    private float getDistanceOfTwoPoints(float x1, float y1, float x2, float y2) {
        return (float) (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!mActive) {
            return;
        }
        canvas.drawBitmap(mStickerComponent.getBitmap(), mMatrix, mPaint); // 绘制主图片
        if (mSelected) {
            drawFrame(canvas); // 绘制边框,以便测试点的映射
            drawFunctionComponent(canvas); // 绘制控制点图片
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawLine(mDstPointArr[0], mDstPointArr[1], mDstPointArr[2], mDstPointArr[3],
                mFramePaint);
        canvas.drawLine(mDstPointArr[2], mDstPointArr[3], mDstPointArr[4], mDstPointArr[5],
                mFramePaint);
        canvas.drawLine(mDstPointArr[4], mDstPointArr[5], mDstPointArr[6], mDstPointArr[7],
                mFramePaint);
        canvas.drawLine(mDstPointArr[0], mDstPointArr[1], mDstPointArr[6], mDstPointArr[7],
                mFramePaint);
    }

    private void drawFunctionComponent(Canvas canvas) {
        canvas.drawBitmap(
                mDeleteComponent.getBitmap(),
                mDstPointArr[0] - mDeleteComponent.getHalfBitmapWidth(),
                mDstPointArr[1] - mDeleteComponent.getHalfBitmapHeight(),
                mPaint
        );

        canvas.drawBitmap(
                mControllerComponent.getBitmap(),
                mDstPointArr[4] - mControllerComponent.getHalfBitmapWidth(),
                mDstPointArr[5] - mControllerComponent.getHalfBitmapHeight(),
                mPaint
        );
    }

    /**
     * 获取饰品旋转角度
     */
    public float getDegree() {
        return mLastDegrees - mDefaultDegrees;
    }

    /**
     * 获取饰品中心点坐标
     */
    public float[] getCenterPoint() {
        float[] centerPoint = new float[2];
        centerPoint[0] = mDstPointArr[8];
        centerPoint[1] = mDstPointArr[9];
        return centerPoint;
    }

    /**
     * 获取贴纸缩放比例(与原图相比)
     */
    /**
     * <pre>
     * 0, 1 ------ 2, 3
     *  |           |
     *  |    8, 9   |
     *  |           |
     * 6, 7 ------ 4, 5
     * </pre>
     */
    public float getScale() {
        float preDistance =
                (mSrcPointArr[8] - mSrcPointArr[0]) * (mSrcPointArr[8] - mSrcPointArr[0])
                        +
                        (mSrcPointArr[9] - mSrcPointArr[1]) * (mSrcPointArr[9] - mSrcPointArr[1]);

        float lastDistance =
                (mDstPointArr[8] - mDstPointArr[0]) * (mDstPointArr[8] - mDstPointArr[0])
                        +
                        (mDstPointArr[9] - mDstPointArr[1]) * (mDstPointArr[9] - mDstPointArr[1]);

        return (float) Math.sqrt(lastDistance / preDistance);
    }

    /**
     * 判断饰品是否已被移除
     */
    public boolean isActive() {
        return mActive;
    }

    /**
     * 获取素材图片路径
     */
    public String getPhotoPath() {
        return mPhotoPath;
    }
}  
