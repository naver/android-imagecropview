/*
 * Copyright (c) 2015 Naver Corp.
 * @Author Ohkyun Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.naver.android.helloyako.imagecrop.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.naver.android.helloyako.imagecrop.R;
import com.naver.android.helloyako.imagecrop.model.CropInfo;
import com.naver.android.helloyako.imagecrop.util.BitmapLoadUtils;
import com.naver.android.helloyako.imagecrop.view.graphics.FastBitmapDrawable;

import java.io.File;

import it.sephiroth.android.library.easing.Cubic;
import it.sephiroth.android.library.easing.Easing;


public class ImageCropView extends ImageView {

    public static final String LOG_TAG = "ImageCropView";
    protected static final boolean LOG_ENABLED = false;

    public static final float ZOOM_INVALID = -1f;

    public static final int DEFAULT_ASPECT_RATIO_WIDTH = 1;
    public static final int DEFAULT_ASPECT_RATIO_HEIGHT = 1;

    public static final int GRID_OFF = 0;
    public static final int GRID_ON = 1;

    protected Easing mEasing = new Cubic();
    protected Matrix mBaseMatrix = new Matrix();
    protected Matrix mSuppMatrix = new Matrix();
    protected final Matrix mDisplayMatrix = new Matrix();
    protected Handler mHandler = new Handler();
    protected Runnable mLayoutRunnable = null;
    protected boolean mUserScaled = false;

    private float mMaxZoom = ZOOM_INVALID;
    private float mMinZoom = ZOOM_INVALID;

    // true when min and max zoom are explicitly defined
    private boolean mMaxZoomDefined;
    private boolean mMinZoomDefined;

    protected final float[] mMatrixValues = new float[9];

    private int mThisWidth = -1;
    private int mThisHeight = -1;
    private PointF mCenter = new PointF();

    private boolean mBitmapChanged;
    private boolean mRestoreRequest;

    final protected int DEFAULT_ANIMATION_DURATION = 200;
    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";

    protected RectF mBitmapRect = new RectF();
    protected RectF mCenterRect = new RectF();
    protected RectF mScrollRect = new RectF();
    protected RectF mCropRect = new RectF();

    private Paint mTransparentLayerPaint;

    private int mAspectRatioWidth = DEFAULT_ASPECT_RATIO_WIDTH;
    private int mAspectRatioHeight = DEFAULT_ASPECT_RATIO_HEIGHT;

    private float mTargetAspectRatio = mAspectRatioHeight / mAspectRatioWidth;

    private float[] mPts;
    private final int GRID_ROW_COUNT = 3;
    private final int GRID_COLUMN_COUNT = 3;
    private Paint mGridInnerLinePaint;
    private Paint mGridOuterLinePaint;
    private int gridInnerMode;
    private int gridOuterMode;
    private float gridLeftRightMargin;
    private float gridTopBottomMargin;

    private String imageFilePath;

    protected ScaleGestureDetector mScaleDetector;
    protected GestureDetector mGestureDetector;
    protected int mTouchSlop;
    protected float mScaleFactor;
    protected int mDoubleTapDirection;
    protected GestureDetector.OnGestureListener mGestureListener;
    protected ScaleGestureDetector.OnScaleGestureListener mScaleListener;
    protected boolean mDoubleTapEnabled = false;
    protected boolean mScaleEnabled = true;
    protected boolean mScrollEnabled = true;
    private ImageCropView.OnImageViewTouchDoubleTapListener mDoubleTapListener;
    private ImageCropView.OnImageViewTouchSingleTapListener mSingleTapListener;

    private boolean isChangingScale = false;

    private int savedAspectRatioWidth;
    private int savedAspectRatioHeight;
    private float[] suppMatrixValues = new float[9];

    public ImageCropView(Context context) {
        this(context, null);
    }

    public ImageCropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageCropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ImageCropView);

        mTransparentLayerPaint = new Paint();
        mTransparentLayerPaint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));

        setScaleType(ImageView.ScaleType.MATRIX);

        mGridInnerLinePaint = new Paint();
        float gridInnerStrokeWidth = a.getDimension(R.styleable.ImageCropView_gridInnerStroke, 1);
        mGridInnerLinePaint.setStrokeWidth(gridInnerStrokeWidth);
        int gridInnerColor = a.getColor(R.styleable.ImageCropView_gridInnerColor, Color.WHITE);
        mGridInnerLinePaint.setColor(gridInnerColor);

        mGridOuterLinePaint = new Paint();
        float gridOuterStrokeWidth = a.getDimension(R.styleable.ImageCropView_gridOuterStroke, 1);
        mGridOuterLinePaint.setStrokeWidth(gridOuterStrokeWidth);
        int gridOuterColor = a.getColor(R.styleable.ImageCropView_gridOuterColor, Color.WHITE);
        mGridOuterLinePaint.setColor(gridOuterColor);
        mGridOuterLinePaint.setStyle(Paint.Style.STROKE);

        gridInnerMode = a.getInt(R.styleable.ImageCropView_setInnerGridMode, GRID_OFF);
        gridOuterMode = a.getInt(R.styleable.ImageCropView_setOuterGridMode, GRID_OFF);

        gridLeftRightMargin = a.getDimension(R.styleable.ImageCropView_gridLeftRightMargin, 0);
        gridTopBottomMargin = a.getDimension(R.styleable.ImageCropView_gridTopBottomMargin, 0);

        int rowLineCount = (GRID_ROW_COUNT - 1) * 4;
        int columnLineCount = (GRID_COLUMN_COUNT - 1) * 4;
        mPts = new float[rowLineCount + columnLineCount];

        a.recycle();

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mGestureListener = new GestureListener();
        mScaleListener = new ScaleListener();

        mScaleDetector = new ScaleGestureDetector(getContext(), mScaleListener);
        mGestureDetector = new GestureDetector(getContext(), mGestureListener, null, true);

        mDoubleTapDirection = 1;

        mBitmapChanged = false;
        mRestoreRequest = false;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        } else {
            throw new IllegalArgumentException("Unsupported scaleType. Only ScaleType.MATRIX can be used");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTransparentLayer(canvas);
        drawGrid(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (LOG_ENABLED) {
            Log.d(LOG_TAG, "onLayout: " + changed + ", bitmapChanged: " + mBitmapChanged);
        }

        super.onLayout(changed, left, top, right, bottom);

        int deltaX = 0;
        int deltaY = 0;

        if (changed) {
            int oldW = mThisWidth;
            int oldH = mThisHeight;

            mThisWidth = right - left;
            mThisHeight = bottom - top;

            deltaX = mThisWidth - oldW;
            deltaY = mThisHeight - oldH;

            // update center point
            mCenter.x = mThisWidth / 2f;
            mCenter.y = mThisHeight / 2f;
        }

        int height = (int) (mThisWidth * mTargetAspectRatio);
        if (height > mThisHeight) {
            int width = (int) ((mThisHeight - (gridTopBottomMargin * 2)) / mTargetAspectRatio);
            int halfDiff = (mThisWidth - width) / 2;
            mCropRect.set(left + halfDiff, top + gridTopBottomMargin, right - halfDiff, bottom - gridTopBottomMargin);
        } else {
            height = (int) ((mThisWidth - (gridLeftRightMargin * 2)) * mTargetAspectRatio);
            int halfDiff = (mThisHeight - height) / 2;
            mCropRect.set(left + gridLeftRightMargin, halfDiff - top, right - gridLeftRightMargin, height + halfDiff);
        }

        Runnable r = mLayoutRunnable;

        if (r != null) {
            mLayoutRunnable = null;
            r.run();
        }

        final Drawable drawable = getDrawable();

        if (drawable != null) {

            if (changed || mBitmapChanged) {

                if (mBitmapChanged) {
                    mBaseMatrix.reset();
                    if (!mMinZoomDefined) mMinZoom = ZOOM_INVALID;
                    if (!mMaxZoomDefined) mMaxZoom = ZOOM_INVALID;
                }

                float scale = 1;

                // retrieve the old values
                float oldMatrixScale = getScale(mBaseMatrix);
                float oldScale = getScale();
                float oldMinScale = Math.min(1f, 1f / oldMatrixScale);

                getProperBaseMatrix(drawable, mBaseMatrix);

                float new_matrix_scale = getScale(mBaseMatrix);

                if (LOG_ENABLED) {
                    Log.d(LOG_TAG, "old matrix scale: " + oldMatrixScale);
                    Log.d(LOG_TAG, "new matrix scale: " + new_matrix_scale);
                    Log.d(LOG_TAG, "old min scale: " + oldMinScale);
                    Log.d(LOG_TAG, "old scale: " + oldScale);
                }

                // 1. bitmap changed or scaleType changed
                if (mBitmapChanged) {
                    setImageMatrix(getImageViewMatrix());
                } else if (changed) {

                    // 2. layout size changed

                    if (!mMinZoomDefined) mMinZoom = ZOOM_INVALID;
                    if (!mMaxZoomDefined) mMaxZoom = ZOOM_INVALID;

                    setImageMatrix(getImageViewMatrix());
                    postTranslate(-deltaX, -deltaY);


                    if (!mUserScaled) {
                        zoomTo(scale);
                    } else {
                        if (Math.abs(oldScale - oldMinScale) > 0.001) {
                            scale = (oldMatrixScale / new_matrix_scale) * oldScale;
                        }
                        if (LOG_ENABLED) {
                            Log.v(LOG_TAG, "userScaled. scale=" + scale);
                        }
                        zoomTo(scale);
                    }

                    if (LOG_ENABLED) {
                        Log.d(LOG_TAG, "old scale: " + oldScale);
                        Log.d(LOG_TAG, "new scale: " + scale);
                    }


                }

                mUserScaled = false;

                if (scale > getMaxScale() || scale < getMinScale()) {
                    // if current scale if outside the min/max bounds
                    // then restore the correct scale
                    zoomTo(scale);
                }

                if (!mRestoreRequest) {
                    center(true, true);
                }


                if (mBitmapChanged) mBitmapChanged = false;
                if (mRestoreRequest) mRestoreRequest = false;

                if (LOG_ENABLED) {
                    Log.d(LOG_TAG, "new scale: " + getScale());
                }
            }
        } else {
            if (mBitmapChanged) mBitmapChanged = false;
            if (mRestoreRequest) mRestoreRequest = false;
        }
    }

    public void resetDisplay() {
        mBitmapChanged = true;
        resetMatrix();
        requestLayout();
    }

    public void resetMatrix() {
        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "resetMatrix");
        }
        mSuppMatrix = new Matrix();

        setImageMatrix(getImageViewMatrix());

        zoomTo(1f);

        postInvalidate();
    }

    private void drawTransparentLayer(Canvas canvas) {
        /*-
          -------------------------------------
          |                top                |
          -------------------------------------
          |      |                    |       |
          |      |                    |       |
          | left |      mCropRect     | right |
          |      |                    |       |
          |      |                    |       |
          -------------------------------------
          |              bottom               |
          -------------------------------------
         */

        Rect r = new Rect();
        getLocalVisibleRect(r);

        canvas.drawRect(r.left, r.top, r.right, mCropRect.top, mTransparentLayerPaint);                          // top
        canvas.drawRect(r.left, mCropRect.bottom, r.right, r.bottom, mTransparentLayerPaint);                    // bottom
        canvas.drawRect(r.left, mCropRect.top, mCropRect.left, mCropRect.bottom, mTransparentLayerPaint);        // left
        canvas.drawRect(mCropRect.right, mCropRect.top, r.right, mCropRect.bottom, mTransparentLayerPaint);      // right
    }

    private void drawGrid(Canvas canvas) {
        int index = 0;
        for (int i = 0; i < GRID_ROW_COUNT - 1; i++) {
            mPts[index++] = mCropRect.left;                                                                             //start Xi
            mPts[index++] = (mCropRect.height() * (((float) i + 1.0f) / (float) GRID_ROW_COUNT)) + mCropRect.top;       //start Yi
            mPts[index++] = mCropRect.right;                                                                            //stop  Xi
            mPts[index++] = (mCropRect.height() * (((float) i + 1.0f) / (float) GRID_ROW_COUNT)) + mCropRect.top;       //stop  Yi
        }

        for (int i = 0; i < GRID_COLUMN_COUNT - 1; i++) {
            mPts[index++] = (mCropRect.width() * (((float) i + 1.0f) / (float) GRID_COLUMN_COUNT)) + mCropRect.left;    //start Xi
            mPts[index++] = mCropRect.top;                                                                              //start Yi
            mPts[index++] = (mCropRect.width() * (((float) i + 1.0f) / (float) GRID_COLUMN_COUNT)) + mCropRect.left;    //stop  Xi
            mPts[index++] = mCropRect.bottom;                                                                           //stop  Yi
        }

        if (gridInnerMode == GRID_ON) {
            canvas.drawLines(mPts, mGridInnerLinePaint);
        }

        if (gridOuterMode == GRID_ON) {
            float halfLineWidth = mGridOuterLinePaint.getStrokeWidth() * 0.5f;
            canvas.drawRect(mCropRect.left + halfLineWidth, mCropRect.top + halfLineWidth, mCropRect.right - halfLineWidth, mCropRect.bottom - halfLineWidth, mGridOuterLinePaint);
        }
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(getContext().getResources().getDrawable(resId));
    }

    public void setAspectRatio(int aspectRatioWidth, int aspectRatioHeight) {
        if (aspectRatioWidth <= 0 || aspectRatioHeight <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        } else {
            mAspectRatioWidth = aspectRatioWidth;
            mAspectRatioHeight = aspectRatioHeight;
            mTargetAspectRatio = (float) mAspectRatioHeight / (float) mAspectRatioWidth;
        }

        resetDisplay();
    }

    public void setImageFilePath(String imageFilePath) {
        File imageFile = new File(imageFilePath);
        if (!imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist");
        }
        this.imageFilePath = imageFilePath;
        int reqSize = 1000;
        Bitmap bitmap = BitmapLoadUtils.decode(imageFilePath, reqSize, reqSize, true);
        setImageBitmap(bitmap);
    }

    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        float minScale = 1f;
        float maxScale = 8f;
        setImageBitmap(bitmap, minScale, maxScale);
    }

    public void setImageBitmap(final Bitmap bitmap, final float min_zoom, final float max_zoom) {
        final int viewWidth = getWidth();
        if (viewWidth <= 0) {
            mLayoutRunnable = new Runnable() {

                @Override
                public void run() {
                    setImageBitmap(bitmap, min_zoom, max_zoom);
                }
            };
            return;
        }

        if (bitmap != null) {
            setImageDrawable(new FastBitmapDrawable(bitmap), min_zoom, max_zoom);
        } else {
            setImageDrawable(null, min_zoom, max_zoom);
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        float minScale = 1f;
        float maxScale = 8f;
        setImageDrawable(drawable, minScale, maxScale);
    }

    public void setImageDrawable(final Drawable drawable, final float min_zoom, final float max_zoom) {
        final int viewWidth = getWidth();

        if (viewWidth <= 0) {
            mLayoutRunnable = new Runnable() {

                @Override
                public void run() {
                    setImageDrawable(drawable, min_zoom, max_zoom);
                }
            };
            return;
        }
        _setImageDrawable(drawable, min_zoom, max_zoom);
    }

    protected void _setImageDrawable(final Drawable drawable, float min_zoom, float max_zoom) {

        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "_setImageDrawable");
        }

        mBaseMatrix.reset();

        if (drawable != null) {
            if (LOG_ENABLED) {
                Log.d(LOG_TAG, "size: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
            }
            super.setImageDrawable(drawable);
        } else {
            super.setImageDrawable(null);
        }

        if (min_zoom != ZOOM_INVALID && max_zoom != ZOOM_INVALID) {
            min_zoom = Math.min(min_zoom, max_zoom);
            max_zoom = Math.max(min_zoom, max_zoom);

            mMinZoom = min_zoom;
            mMaxZoom = max_zoom;

            mMinZoomDefined = true;
            mMaxZoomDefined = true;
        } else {
            mMinZoom = ZOOM_INVALID;
            mMaxZoom = ZOOM_INVALID;

            mMinZoomDefined = false;
            mMaxZoomDefined = false;
        }

        if (LOG_ENABLED) {
            Log.v(LOG_TAG, "mMinZoom: " + mMinZoom + ", mMaxZoom: " + mMaxZoom);
        }

        mBitmapChanged = true;

        mScaleFactor = getMaxScale() / 3;
        requestLayout();
    }

    protected float computeMaxZoom() {
        final Drawable drawable = getDrawable();

        if (drawable == null) {
            return 1F;
        }

        float fw = (float) drawable.getIntrinsicWidth() / (float) mThisWidth;
        float fh = (float) drawable.getIntrinsicHeight() / (float) mThisHeight;
        float scale = Math.max(fw, fh) * 8;

        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "computeMaxZoom: " + scale);
        }
        return scale;
    }

    protected float computeMinZoom() {
        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "computeMinZoom");
        }

        final Drawable drawable = getDrawable();

        if (drawable == null) {
            return 1F;
        }

        float scale = getScale(mBaseMatrix);
        scale = Math.min(1f, 1f / scale);

        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "computeMinZoom: " + scale);
        }

        return scale;
    }

    public float getMaxScale() {
        if (mMaxZoom == ZOOM_INVALID) {
            mMaxZoom = computeMaxZoom();
        }
        return mMaxZoom;
    }

    public float getMinScale() {
        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "getMinScale, mMinZoom: " + mMinZoom);
        }

        if (mMinZoom == ZOOM_INVALID) {
            mMinZoom = computeMinZoom();
        }

        if (LOG_ENABLED) {
            Log.v(LOG_TAG, "mMinZoom: " + mMinZoom);
        }

        return mMinZoom;
    }

    public Matrix getImageViewMatrix() {
        return getImageViewMatrix(mSuppMatrix);
    }

    public Matrix getImageViewMatrix(Matrix supportMatrix) {
        mDisplayMatrix.set(mBaseMatrix);
        mDisplayMatrix.postConcat(supportMatrix);
        return mDisplayMatrix;
    }

    private float baseScale = 1f;

    protected void getProperBaseMatrix(Drawable drawable, Matrix matrix) {
        float viewWidth = mCropRect.width();
        float viewHeight = mCropRect.height();

        if (LOG_ENABLED) {
            Log.d(LOG_TAG, "getProperBaseMatrix. view: " + viewWidth + "x" + viewHeight);
        }

        float w = drawable.getIntrinsicWidth();
        float h = drawable.getIntrinsicHeight();
        float widthScale, heightScale;
        matrix.reset();

        if (w > viewWidth || h > viewHeight) {
            widthScale = viewWidth / w;
            heightScale = viewHeight / h;
            baseScale = Math.max(widthScale, heightScale);
            matrix.postScale(baseScale, baseScale);

            float tw = (viewWidth - w * baseScale) / 2.0f;
            float th = (viewHeight - h * baseScale) / 2.0f;
            matrix.postTranslate(tw, th);

        } else {
            widthScale = viewWidth / w;
            heightScale = viewHeight / h;
            baseScale = Math.max(widthScale, heightScale);
            matrix.postScale(baseScale, baseScale);

            float tw = (viewWidth - w * baseScale) / 2.0f;
            float th = (viewHeight - h * baseScale) / 2.0f;
            matrix.postTranslate(tw, th);
        }

        if (LOG_ENABLED) {
            printMatrix(matrix);
        }
    }

    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    public void printMatrix(Matrix matrix) {
        float scalex = getValue(matrix, Matrix.MSCALE_X);
        float scaley = getValue(matrix, Matrix.MSCALE_Y);
        float tx = getValue(matrix, Matrix.MTRANS_X);
        float ty = getValue(matrix, Matrix.MTRANS_Y);
        Log.d(LOG_TAG, "matrix: { x: " + tx + ", y: " + ty + ", scalex: " + scalex + ", scaley: " + scaley + " }");
    }

    public RectF getBitmapRect() {
        return getBitmapRect(mSuppMatrix);
    }

    protected RectF getBitmapRect(Matrix supportMatrix) {
        final Drawable drawable = getDrawable();

        if (drawable == null) return null;
        Matrix m = getImageViewMatrix(supportMatrix);
        mBitmapRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        m.mapRect(mBitmapRect);
        return mBitmapRect;
    }

    protected float getScale(Matrix matrix) {
        return getValue(matrix, Matrix.MSCALE_X);
    }

    @SuppressLint("Override")
    public float getRotation() {
        return 0;
    }

    public float getScale() {
        return getScale(mSuppMatrix);
    }

    public float getBaseScale() {
        return getScale(mBaseMatrix);
    }

    protected void center(boolean horizontal, boolean vertical) {
        final Drawable drawable = getDrawable();
        if (drawable == null) return;

        RectF rect = getCenter(mSuppMatrix, horizontal, vertical);

        if (rect.left != 0 || rect.top != 0) {

            if (LOG_ENABLED) {
                Log.i(LOG_TAG, "center");
            }
            postTranslate(rect.left, rect.top);
        }
    }

    protected RectF getCenter(Matrix supportMatrix, boolean horizontal, boolean vertical) {
        final Drawable drawable = getDrawable();

        if (drawable == null) return new RectF(0, 0, 0, 0);

        mCenterRect.set(0, 0, 0, 0);
        RectF rect = getBitmapRect(supportMatrix);
        float height = rect.height();
        float width = rect.width();
        float deltaX = 0, deltaY = 0;
        if (vertical) {
            int viewHeight = mThisHeight;
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = mThisHeight - rect.bottom;
            }
        }
        if (horizontal) {
            int viewWidth = mThisWidth;
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }
        mCenterRect.set(deltaX, deltaY, 0, 0);
        return mCenterRect;
    }

    protected void postTranslate(float deltaX, float deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            if (LOG_ENABLED) {
                Log.i(LOG_TAG, "postTranslate: " + deltaX + "x" + deltaY);
            }
            mSuppMatrix.postTranslate(deltaX, deltaY);
            setImageMatrix(getImageViewMatrix());
        }
    }

    protected void postScale(float scale, float centerX, float centerY) {
        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "postScale: " + scale + ", center: " + centerX + "x" + centerY);
        }
        mSuppMatrix.postScale(scale, scale, centerX, centerY);
        setImageMatrix(getImageViewMatrix());
    }

    protected PointF getCenter() {
        return mCenter;
    }

    protected void zoomTo(float scale) {
        if (LOG_ENABLED) {
            Log.i(LOG_TAG, "zoomTo: " + scale);
        }

        if (scale > getMaxScale()) scale = getMaxScale();
        if (scale < getMinScale()) scale = getMinScale();

        if (LOG_ENABLED) {
            Log.d(LOG_TAG, "sanitized scale: " + scale);
        }


        PointF center = getCenter();
        zoomTo(scale, center.x, center.y);
    }

    public void zoomTo(float scale, float durationMs) {
        PointF center = getCenter();
        zoomTo(scale, center.x, center.y, durationMs);
    }

    protected void zoomTo(float scale, float centerX, float centerY) {
        if (scale > getMaxScale()) scale = getMaxScale();

        float oldScale = getScale();
        float deltaScale = scale / oldScale;
        postScale(deltaScale, centerX, centerY);
        center(true, true);
    }

    protected void onZoomAnimationCompleted(float scale) {
        if (LOG_ENABLED) {
            Log.d(LOG_TAG, "onZoomAnimationCompleted. scale: " + scale + ", minZoom: " + getMinScale());
        }

        if (scale < getMinScale()) {
            zoomTo(getMinScale(), 50);
        }
    }

    public void scrollBy(float x, float y) {
        panBy(x, y);
    }

    protected void panBy(double dx, double dy) {
        mScrollRect.set((float) dx, (float) dy, 0, 0);
        postTranslate(mScrollRect.left, mScrollRect.top);
        adjustCropAreaImage();
    }

    private void adjustCropAreaImage() {
        final Drawable drawable = getDrawable();
        if (drawable == null) return;

        RectF rect = getAdjust(mSuppMatrix);

        if (rect.left != 0 || rect.top != 0) {

            if (LOG_ENABLED) {
                Log.i(LOG_TAG, "center");
            }
            postTranslate(rect.left, rect.top);
        }
    }

    private RectF getAdjust(Matrix supportMatrix) {
        final Drawable drawable = getDrawable();

        if (drawable == null) return new RectF(0, 0, 0, 0);

        mCenterRect.set(0, 0, 0, 0);
        RectF rect = getBitmapRect(supportMatrix);
        float deltaX = 0, deltaY = 0;

        //Y
        if (rect.top > mCropRect.top) {
            deltaY = mCropRect.top - rect.top;
        } else if (rect.bottom < mCropRect.bottom) {
            deltaY = mCropRect.bottom - rect.bottom;

        }

        //X
        if (rect.left > mCropRect.left) {
            deltaX = mCropRect.left - rect.left;
        } else if (rect.right < mCropRect.right) {
            deltaX = mCropRect.right - rect.right;
        }

        mCenterRect.set(deltaX, deltaY, 0, 0);
        return mCenterRect;

    }

    protected void scrollBy(float distanceX, float distanceY, final double durationMs) {
        final double dx = distanceX;
        final double dy = distanceY;
        final long startTime = System.currentTimeMillis();
        mHandler.post(
                new Runnable() {

                    double old_x = 0;
                    double old_y = 0;

                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        double currentMs = Math.min(durationMs, now - startTime);
                        double x = mEasing.easeOut(currentMs, 0, dx, durationMs);
                        double y = mEasing.easeOut(currentMs, 0, dy, durationMs);
                        panBy((x - old_x), (y - old_y));
                        old_x = x;
                        old_y = y;
                        if (currentMs < durationMs) {
                            mHandler.post(this);
                        }
                    }
                }
        );
    }

    protected void zoomTo(float scale, float centerX, float centerY, final float durationMs) {
        if (scale > getMaxScale()) scale = getMaxScale();

        final long startTime = System.currentTimeMillis();
        final float oldScale = getScale();

        final float deltaScale = scale - oldScale;

        Matrix m = new Matrix(mSuppMatrix);
        m.postScale(scale, scale, centerX, centerY);
        RectF rect = getCenter(m, true, true);

        final float destX = centerX + rect.left * scale;
        final float destY = centerY + rect.top * scale;

        mHandler.post(
                new Runnable() {

                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        float currentMs = Math.min(durationMs, now - startTime);
                        float newScale = (float) mEasing.easeInOut(currentMs, 0, deltaScale, durationMs);
                        zoomTo(oldScale + newScale, destX, destY);
                        if (currentMs < durationMs) {
                            mHandler.post(this);
                        } else {
                            onZoomAnimationCompleted(getScale());
                            center(true, true);
                        }
                    }
                }
        );
    }

    public Bitmap getCroppedImage() {
        CropInfo cropInfo = getCropInfo();
        if (cropInfo == null) {
            return null;
        }
        Bitmap bitmap;
        if (imageFilePath != null) {
            bitmap = cropInfo.getCroppedImage(imageFilePath);
        } else {
            bitmap = getViewBitmap();
            if (bitmap != null) {
                bitmap = cropInfo.getCroppedImage(bitmap);
            }
        }
        return bitmap;
    }

    public CropInfo getCropInfo() {
        Bitmap viewBitmap = getViewBitmap();
        if (viewBitmap == null) {
            return null;
        }
        float scale = baseScale * getScale();
        RectF viewImageRect = getBitmapRect();

        return new CropInfo(scale, viewBitmap.getWidth(), viewImageRect.top, viewImageRect.left, mCropRect.top, mCropRect.left, mCropRect.width(), mCropRect.height());
    }

    public Bitmap getViewBitmap() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            return ((FastBitmapDrawable) drawable).getBitmap();
        } else {
            Log.e(LOG_TAG, "drawable is null");
            return null;
        }
    }

    public void setGridInnerMode(int gridInnerMode) {
        this.gridInnerMode = gridInnerMode;
        invalidate();
    }

    public void setGridOuterMode(int gridOuterMode) {
        this.gridOuterMode = gridOuterMode;
        invalidate();
    }

    public void setGridLeftRightMargin(int marginDP) {
        this.gridLeftRightMargin = dpToPixel(marginDP);
        requestLayout();
    }

    public void setGridTopBottomMargin(int marginDP) {
        this.gridTopBottomMargin = dpToPixel(marginDP);
        requestLayout();
    }

    private int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void saveState() {
        savedAspectRatioWidth = mAspectRatioWidth;
        savedAspectRatioHeight = mAspectRatioHeight;
        mSuppMatrix.getValues(suppMatrixValues);
    }

    public void restoreState() {
        mBitmapChanged = true;
        mRestoreRequest = true;
        mAspectRatioWidth = savedAspectRatioWidth;
        mAspectRatioHeight = savedAspectRatioHeight;
        mTargetAspectRatio = (float) mAspectRatioHeight / (float) mAspectRatioWidth;

        mSuppMatrix = new Matrix();
        mSuppMatrix.setValues(suppMatrixValues);

        setImageMatrix(getImageViewMatrix());
        postInvalidate();
        requestLayout();
    }

    public void setDoubleTapListener(OnImageViewTouchDoubleTapListener listener) {
        mDoubleTapListener = listener;
    }

    public void setSingleTapListener(OnImageViewTouchSingleTapListener listener) {
        mSingleTapListener = listener;
    }

    public void setDoubleTapEnabled(boolean value) {
        mDoubleTapEnabled = value;
    }

    public void setScaleEnabled(boolean value) {
        mScaleEnabled = value;
    }

    public void setScrollEnabled(boolean value) {
        mScrollEnabled = value;
    }

    public boolean getDoubleTapEnabled() {
        return mDoubleTapEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mBitmapChanged) return false;
        mScaleDetector.onTouchEvent(event);

        if (!mScaleDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }

        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                return onUp(event);
        }
        return true;
    }

    protected float onDoubleTapPost(float scale, float maxZoom) {
        if (mDoubleTapDirection == 1) {
            if ((scale + (mScaleFactor * 2)) <= maxZoom) {
                return scale + mScaleFactor;
            } else {
                mDoubleTapDirection = -1;
                return maxZoom;
            }
        } else {
            mDoubleTapDirection = 1;
            return 1f;
        }
    }

    public boolean onSingleTapConfirmed(MotionEvent e) {
        return true;
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mUserScaled = true;
        scrollBy(-distanceX, -distanceY);
        invalidate();
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        if (Math.abs(velocityX) > 800 || Math.abs(velocityY) > 800) {
            mUserScaled = true;
            scrollBy(diffX / 2, diffY / 2, 300);
            invalidate();
            return true;
        }
        return false;
    }

    public boolean onDown(MotionEvent e) {
        if (mBitmapChanged) return false;
        return true;
    }

    public boolean onUp(MotionEvent e) {
        if (mBitmapChanged) return false;
        if (getScale() < getMinScale()) {
            zoomTo(getMinScale(), 50);
        }
        return true;
    }

    public boolean onSingleTapUp(MotionEvent e) {
        if (mBitmapChanged) return false;
        return true;
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            if (null != mSingleTapListener) {
                mSingleTapListener.onSingleTapConfirmed();
            }

            return ImageCropView.this.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (LOG_ENABLED) {
                Log.d(LOG_TAG, "onDoubleTap. double tap enabled? " + mDoubleTapEnabled);
            }
            if (mDoubleTapEnabled) {
                mUserScaled = true;
                float scale = getScale();
                float targetScale = onDoubleTapPost(scale, getMaxScale());
                targetScale = Math.min(getMaxScale(), Math.max(targetScale, getMinScale()));
                zoomTo(targetScale, e.getX(), e.getY(), DEFAULT_ANIMATION_DURATION);
                invalidate();
            }

            if (null != mDoubleTapListener) {
                mDoubleTapListener.onDoubleTap();
            }

            return super.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (isLongClickable()) {
                if (!mScaleDetector.isInProgress()) {
                    setPressed(true);
                    performLongClick();
                }
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mScrollEnabled) return false;
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            if (mScaleDetector.isInProgress()) return false;
            return ImageCropView.this.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!mScrollEnabled) return false;

            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            if (mScaleDetector.isInProgress()) return false;

            return ImageCropView.this.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return ImageCropView.this.onSingleTapUp(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return ImageCropView.this.onDown(e);
        }
    }


    public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        protected boolean mScaled = false;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isChangingScale = true;
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float span = detector.getCurrentSpan() - detector.getPreviousSpan();
            float targetScale = getScale() * detector.getScaleFactor();

            if (mScaleEnabled) {
                if (mScaled && span != 0) {
                    mUserScaled = true;
                    targetScale = Math.min(getMaxScale(), Math.max(targetScale, getMinScale() - 0.1f));
                    zoomTo(targetScale, detector.getFocusX(), detector.getFocusY());
                    mDoubleTapDirection = 1;
                    invalidate();
                    return true;
                }

                // This is to prevent a glitch the first time
                // image is scaled.
                if (!mScaled) mScaled = true;
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isChangingScale = false;
            super.onScaleEnd(detector);
        }
    }

    public interface OnImageViewTouchDoubleTapListener {

        void onDoubleTap();
    }

    public interface OnImageViewTouchSingleTapListener {

        void onSingleTapConfirmed();
    }

    public boolean isChangingScale() {
        return isChangingScale;
    }
}
