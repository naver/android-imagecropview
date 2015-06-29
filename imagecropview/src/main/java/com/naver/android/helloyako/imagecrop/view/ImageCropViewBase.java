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
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.naver.android.helloyako.imagecrop.R;
import com.naver.android.helloyako.imagecrop.util.BitmapLoadUtils;
import com.naver.android.helloyako.imagecrop.view.graphics.FastBitmapDrawable;

import it.sephiroth.android.library.easing.Cubic;
import it.sephiroth.android.library.easing.Easing;


public abstract class ImageCropViewBase extends ImageView{

	public interface OnDrawableChangeListener {
		void onDrawableChanged(Drawable drawable);
	}

	;

	public interface OnLayoutChangeListener {
		void onLayoutChanged(boolean changed, int left, int top, int right, int bottom);
	}

	public static final String LOG_TAG = "ImageViewTouchBase";
	protected static final boolean LOG_ENABLED = false;

	public static final float ZOOM_INVALID = - 1f;

    public static final int DEFAULT_ASPECT_RATIO_WIDTH = 1;
    public static final int DEFAULT_ASPECT_RATIO_HEIGHT = 1;

    public static final int GRID_OFF = 0;
    public static final int GRID_ON = 1;

	protected Easing mEasing = new Cubic();
	protected Matrix mBaseMatrix = new Matrix();
	protected Matrix mSuppMatrix = new Matrix();
	protected Matrix mNextMatrix;
	protected Handler mHandler = new Handler();
	protected Runnable mLayoutRunnable = null;
	protected boolean mUserScaled = false;

	private float mMaxZoom = ZOOM_INVALID;
	private float mMinZoom = ZOOM_INVALID;

	// true when min and max zoom are explicitly defined
	private boolean mMaxZoomDefined;
	private boolean mMinZoomDefined;

	protected final Matrix mDisplayMatrix = new Matrix();
	protected final float[] mMatrixValues = new float[9];

	private int mThisWidth = - 1;
	private int mThisHeight = - 1;
	private PointF mCenter = new PointF();

	private boolean mScaleTypeChanged;
	private boolean mBitmapChanged;

	final protected int DEFAULT_ANIMATION_DURATION = 200;
    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#99000000";

	protected RectF mBitmapRect = new RectF();
	protected RectF mCenterRect = new RectF();
	protected RectF mScrollRect = new RectF();
    protected RectF mCropRect = new RectF();

	private OnDrawableChangeListener mDrawableChangeListener;
	private OnLayoutChangeListener mOnLayoutChangeListener;

    private Paint mTransparentLayerPaint;

    private int mAspectRatioWidth = ImageCropViewBase.DEFAULT_ASPECT_RATIO_WIDTH;
    private int mAspectRatioHeight = ImageCropViewBase.DEFAULT_ASPECT_RATIO_HEIGHT;

    private float mTargetAspectRatio = mAspectRatioHeight / mAspectRatioWidth;

    private float[] mPts;
    private final int GRID_ROW_COUNT = 3;
    private final int GRID_COLUMN_COUNT = 3;
    private Paint mGridInnerLinePaint;
    private Paint mGridOuterLinePaint;
    private int gridInnerMode;
    private int gridOuterMode;

	private String imageFilePath;

	public ImageCropViewBase(Context context) {
		this(context, null);
	}

	public ImageCropViewBase(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageCropViewBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	public boolean getBitmapChanged() {
		return mBitmapChanged;
	}

	public void setOnDrawableChangedListener(OnDrawableChangeListener listener) {
		mDrawableChangeListener = listener;
	}

	public void setOnLayoutChangeListener(OnLayoutChangeListener listener) {
		mOnLayoutChangeListener = listener;
	}

	protected void init(Context context, AttributeSet attrs, int defStyle) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ImageCropView);

        mTransparentLayerPaint = new Paint();
        mTransparentLayerPaint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));

		setScaleType(ImageView.ScaleType.MATRIX);

        mGridInnerLinePaint = new Paint();
        float gridInnerStrokeWidth = a.getDimension(R.styleable.ImageCropView_gridInnerStroke,1);
        mGridInnerLinePaint.setStrokeWidth(gridInnerStrokeWidth);
        int gridInnerColor = a.getColor(R.styleable.ImageCropView_gridInnerColor,Color.WHITE);
        mGridInnerLinePaint.setColor(gridInnerColor);

        mGridOuterLinePaint = new Paint();
        float gridOuterStrokeWidth = a.getDimension(R.styleable.ImageCropView_gridOuterStroke,1);
        mGridOuterLinePaint.setStrokeWidth(gridOuterStrokeWidth);
        int gridOuterColor = a.getColor(R.styleable.ImageCropView_gridOuterColor,Color.WHITE);
        mGridOuterLinePaint.setColor(gridOuterColor);
        mGridOuterLinePaint.setStyle(Paint.Style.STROKE);

        gridInnerMode = a.getInt(R.styleable.ImageCropView_setInnerGridMode,GRID_OFF);
        gridOuterMode = a.getInt(R.styleable.ImageCropView_setOuterGridMode,GRID_OFF);

        int rowLineCount = (GRID_ROW_COUNT - 1) * 4;
        int columnLineCount = (GRID_COLUMN_COUNT - 1) * 4;
        mPts = new float[rowLineCount + columnLineCount];
	}

	@Override
	public void setScaleType(ScaleType scaleType) {
		if (scaleType == ScaleType.MATRIX) {
			super.setScaleType(scaleType);
		}
		else {
			Log.w(LOG_TAG, "Unsupported scaletype. Only MATRIX can be used");
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
			Log.e(LOG_TAG, "onLayout: " + changed + ", bitmapChanged: " + mBitmapChanged + ", scaleChanged: " + mScaleTypeChanged);
		}

		super.onLayout(changed, left, top, right, bottom);

		int deltaX = 0;
		int deltaY = 0;

		if (changed) {
			int oldw = mThisWidth;
			int oldh = mThisHeight;

			mThisWidth = right - left;
			mThisHeight = bottom - top;

			deltaX = mThisWidth - oldw;
			deltaY = mThisHeight - oldh;

			// update center point
			mCenter.x = mThisWidth / 2f;
			mCenter.y = mThisHeight / 2f;
		}

        int height = (int) (mThisWidth * mTargetAspectRatio);
        if(height > mThisHeight){
            int width = (int) (mThisHeight / mTargetAspectRatio);
            int halfDiff = (mThisWidth - width) / 2;
            mCropRect.set(left + halfDiff, top, right - halfDiff, bottom);
        } else {
            int halfDiff = (mThisHeight - height) / 2;
            mCropRect.set(left, halfDiff - top, right, height + halfDiff);
        }

		Runnable r = mLayoutRunnable;

		if (r != null) {
			mLayoutRunnable = null;
			r.run();
		}

		final Drawable drawable = getDrawable();

		if (drawable != null) {

			if (changed || mScaleTypeChanged || mBitmapChanged) {

				if (mBitmapChanged) {
					mBaseMatrix.reset();
					if (! mMinZoomDefined) mMinZoom = ZOOM_INVALID;
					if (! mMaxZoomDefined) mMaxZoom = ZOOM_INVALID;
				}

				float scale = 1;

				// retrieve the old values
				float old_matrix_scale = getScale(mBaseMatrix);
				float old_scale = getScale();
				float old_min_scale = Math.min(1f, 1f / old_matrix_scale);

				getProperBaseMatrix(drawable, mBaseMatrix);

				float new_matrix_scale = getScale(mBaseMatrix);

				if (LOG_ENABLED) {
					Log.d(LOG_TAG, "old matrix scale: " + old_matrix_scale);
					Log.d(LOG_TAG, "new matrix scale: " + new_matrix_scale);
					Log.d(LOG_TAG, "old min scale: " + old_min_scale);
					Log.d(LOG_TAG, "old scale: " + old_scale);
				}

				// 1. bitmap changed or scaletype changed
				if (mBitmapChanged || mScaleTypeChanged) {

					if (LOG_ENABLED) {
						Log.d(LOG_TAG, "newMatrix: " + mNextMatrix);
					}

					setImageMatrix(getImageViewMatrix());

				}
				else if (changed) {

					// 2. layout size changed

					if (! mMinZoomDefined) mMinZoom = ZOOM_INVALID;
					if (! mMaxZoomDefined) mMaxZoom = ZOOM_INVALID;

					setImageMatrix(getImageViewMatrix());
					postTranslate(- deltaX, - deltaY);


					if (! mUserScaled) {
						zoomTo(scale);
					}
					else {
						if (Math.abs(old_scale - old_min_scale) > 0.001) {
							scale = (old_matrix_scale / new_matrix_scale) * old_scale;
						}
						if (LOG_ENABLED) {
							Log.v(LOG_TAG, "userScaled. scale=" + scale);
						}
						zoomTo(scale);
					}

					if (LOG_ENABLED) {
						Log.d(LOG_TAG, "old scale: " + old_scale);
						Log.d(LOG_TAG, "new scale: " + scale);
					}


				}

				mUserScaled = false;

				if (scale > getMaxScale() || scale < getMinScale()) {
					// if current scale if outside the min/max bounds
					// then restore the correct scale
					zoomTo(scale);
				}

				center(true, true);

				if (mBitmapChanged) onDrawableChanged(drawable);
				if (changed || mBitmapChanged || mScaleTypeChanged) onLayoutChanged(left, top, right, bottom);

				if (mScaleTypeChanged) mScaleTypeChanged = false;
				if (mBitmapChanged) mBitmapChanged = false;

				if (LOG_ENABLED) {
					Log.d(LOG_TAG, "new scale: " + getScale());
				}
			}
		}
		else {
			// drawable is null
			if (mBitmapChanged) onDrawableChanged(drawable);
			if (changed || mBitmapChanged || mScaleTypeChanged) onLayoutChanged(left, top, right, bottom);

			if (mBitmapChanged) mBitmapChanged = false;
			if (mScaleTypeChanged) mScaleTypeChanged = false;

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

        canvas.drawRect(r.left, r.top, r.right, mCropRect.top, mTransparentLayerPaint);                       // top
        canvas.drawRect(r.left, mCropRect.bottom, mCropRect.right, r.bottom, mTransparentLayerPaint);         // bottom
        canvas.drawRect(r.left, mCropRect.top, mCropRect.left, mCropRect.bottom, mTransparentLayerPaint);     // left
        canvas.drawRect(mCropRect.right, mCropRect.top, r.right, mCropRect.bottom, mTransparentLayerPaint);   // right
    }

    private void drawGrid(Canvas canvas){
        int index = 0;
        for(int i = 0; i < GRID_ROW_COUNT - 1; i++){
            mPts[index++] = mCropRect.left;																				//start Xi
            mPts[index++] = (mCropRect.height() *  (((float)i+1.0f) / (float)GRID_ROW_COUNT)) + mCropRect.top;			//start Yi
            mPts[index++] = mCropRect.right;                    		                                            	//stop  Xi
            mPts[index++] = (mCropRect.height() *  (((float)i+1.0f) / (float)GRID_ROW_COUNT))+ mCropRect.top;         	//stop  Yi
        }

        for(int i = 0; i < GRID_COLUMN_COUNT - 1; i++){
            mPts[index++] = (mCropRect.width() *  (((float)i+1.0f) / (float)GRID_COLUMN_COUNT)) + mCropRect.left;		//start Xi
            mPts[index++] = mCropRect.top;                                                               			    //start Yi
            mPts[index++] = (mCropRect.width() *  (((float)i+1.0f) / (float)GRID_COLUMN_COUNT)) + mCropRect.left;       //stop  Xi
            mPts[index++] = mCropRect.bottom;                                                           			    //stop  Yi
        }

        if(gridInnerMode == GRID_ON) {
            canvas.drawLines(mPts, mGridInnerLinePaint);
        }

        if(gridOuterMode == GRID_ON){
            float halfLineWidth = mGridOuterLinePaint.getStrokeWidth() * 0.5f;
            canvas.drawRect(mCropRect.left + halfLineWidth , mCropRect.top + halfLineWidth, mCropRect.right - halfLineWidth, mCropRect.bottom - halfLineWidth,mGridOuterLinePaint);
        }
    }

	@Override
	public void setImageResource(int resId) {
		setImageDrawable(getContext().getResources().getDrawable(resId));
	}

    public void setAspectRatio(int aspectRatioWidth, int aspectRatioHeight){
        if (aspectRatioWidth <= 0 || aspectRatioHeight <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        } else {
            mAspectRatioWidth = aspectRatioWidth;
            mAspectRatioHeight = aspectRatioHeight;
            mTargetAspectRatio = (float)mAspectRatioHeight / (float)mAspectRatioWidth;
        }

        resetDisplay();
    }

	public void setImageFilePath(String imageFilePath){
		this.imageFilePath = imageFilePath;
		int reqSize = 1000;
		Bitmap bitmap = BitmapLoadUtils.decode(imageFilePath, reqSize, reqSize, true);
		setImageBitmap(bitmap);
	}

	@Override
	public void setImageBitmap(final Bitmap bitmap) {
        float minScale = 1f;
        float maxScale = 8f;
        Matrix m = new Matrix();
        m.postScale(minScale, minScale);
        setImageBitmap(bitmap, m, minScale, maxScale);
	}

	public void setImageBitmap(final Bitmap bitmap, final Matrix matrix, final float min_zoom, final float max_zoom) {
		final int viewWidth = getWidth();
		if (viewWidth <= 0) {
			mLayoutRunnable = new Runnable() {

				@Override
				public void run() {
					setImageBitmap(bitmap, matrix, min_zoom, max_zoom);
				}
			};
			return;
		}

		if (bitmap != null) {
			setImageDrawable(new FastBitmapDrawable(bitmap), matrix, min_zoom, max_zoom);
		} else {
			setImageDrawable(null, matrix, min_zoom, max_zoom);
		}
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
        float minScale = 1f;
        float maxScale = 8f;
        Matrix m = new Matrix();
        m.postScale(minScale, minScale);
		setImageDrawable(drawable, m, minScale, maxScale);
	}

	public void setImageDrawable(final Drawable drawable, final Matrix initial_matrix, final float min_zoom, final float max_zoom) {
		final int viewWidth = getWidth();

		if (viewWidth <= 0) {
			mLayoutRunnable = new Runnable() {

				@Override
				public void run() {
					setImageDrawable(drawable, initial_matrix, min_zoom, max_zoom);
				}
			};
			return;
		}
		_setImageDrawable(drawable, initial_matrix, min_zoom, max_zoom);
	}

	protected void _setImageDrawable(final Drawable drawable, final Matrix initial_matrix, float min_zoom, float max_zoom) {

		if (LOG_ENABLED) {
			Log.i(LOG_TAG, "_setImageDrawable");
		}

		mBaseMatrix.reset();

		if (drawable != null) {
			if (LOG_ENABLED) {
				Log.d(LOG_TAG, "size: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
			}
			super.setImageDrawable(drawable);
		}
		else {
			super.setImageDrawable(null);
		}

		if (min_zoom != ZOOM_INVALID && max_zoom != ZOOM_INVALID) {
			min_zoom = Math.min(min_zoom, max_zoom);
			max_zoom = Math.max(min_zoom, max_zoom);

			mMinZoom = min_zoom;
			mMaxZoom = max_zoom;

			mMinZoomDefined = true;
			mMaxZoomDefined = true;
		}
		else {
			mMinZoom = ZOOM_INVALID;
			mMaxZoom = ZOOM_INVALID;

			mMinZoomDefined = false;
			mMaxZoomDefined = false;
		}

		if (initial_matrix != null) {
			mNextMatrix = new Matrix(initial_matrix);
		}

		if (LOG_ENABLED) {
			Log.v(LOG_TAG, "mMinZoom: " + mMinZoom + ", mMaxZoom: " + mMaxZoom);
		}

		mBitmapChanged = true;
		requestLayout();
	}

	protected void onDrawableChanged(final Drawable drawable) {
		if (LOG_ENABLED) {
			Log.i(LOG_TAG, "onDrawableChanged");
			Log.v(LOG_TAG, "scale: " + getScale() + ", minScale: " + getMinScale());
		}
		fireOnDrawableChangeListener(drawable);
	}

	protected void fireOnLayoutChangeListener(int left, int top, int right, int bottom) {
		if (null != mOnLayoutChangeListener) {
			mOnLayoutChangeListener.onLayoutChanged(true, left, top, right, bottom);
		}
	}

	protected void fireOnDrawableChangeListener(Drawable drawable) {
		if (null != mDrawableChangeListener) {
			mDrawableChangeListener.onDrawableChanged(drawable);
		}
	}

	protected void onLayoutChanged(int left, int top, int right, int bottom) {
		if (LOG_ENABLED) {
			Log.i(LOG_TAG, "onLayoutChanged");
		}
		fireOnLayoutChangeListener(left, top, right, bottom);
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

	@Override
	public void setImageMatrix(Matrix matrix) {

		Matrix current = getImageMatrix();
		boolean needUpdate = false;

		if (matrix == null && ! current.isIdentity() || matrix != null && ! current.equals(matrix)) {
			needUpdate = true;
		}

		super.setImageMatrix(matrix);

		if (needUpdate) onImageMatrixChanged();
	}

	protected void onImageMatrixChanged() {}

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

		}
		else {
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
			}
			else if (rect.top > 0) {
				deltaY = - rect.top;
			}
			else if (rect.bottom < viewHeight) {
				deltaY = mThisHeight - rect.bottom;
			}
		}
		if (horizontal) {
			int viewWidth = mThisWidth;
			if (width < viewWidth) {
				deltaX = (viewWidth - width) / 2 - rect.left;
			}
			else if (rect.left > 0) {
				deltaX = - rect.left;
			}
			else if (rect.right < viewWidth) {
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
		onZoom(getScale());
		center(true, true);
	}

	protected void onZoom(float scale) {}

	protected void onZoomAnimationCompleted(float scale) {}

	public void scrollBy(float x, float y) {
		panBy(x, y);
	}

	protected void panBy(double dx, double dy) {
		mScrollRect.set((float) dx, (float) dy, 0, 0);
		postTranslate(mScrollRect.left, mScrollRect.top);
        adjustCropAreaImage();
	}

    private void adjustCropAreaImage(){
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

    private RectF getAdjust(Matrix supportMatrix){
        final Drawable drawable = getDrawable();

        if (drawable == null) return new RectF(0, 0, 0, 0);

        mCenterRect.set(0, 0, 0, 0);
        RectF rect = getBitmapRect(supportMatrix);
        float deltaX = 0, deltaY = 0;

        //Y
        if(rect.top > mCropRect.top){
            deltaY = mCropRect.top - rect.top;
        } else if (rect.bottom < mCropRect.bottom){
            deltaY = mCropRect.bottom - rect.bottom;

        }

        //X
        if(rect.left > mCropRect.left){
            deltaX = mCropRect.left - rect.left;
        } else if(rect.right < mCropRect.right){
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
					}
					else {
						onZoomAnimationCompleted(getScale());
						center(true, true);
					}
				}
			}
		);
	}

    public Bitmap getCroppedImage() {

        Bitmap viewBitmap = getViewBitmap();
		Bitmap sourceBitmap = viewBitmap;

        float scale = baseScale * getScale();

		if(imageFilePath != null) {
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			int imageWidth = (int) ((float) metrics.widthPixels / 1.5);
			int imageHeight = (int) ((float) metrics.heightPixels / 1.5);
			sourceBitmap = BitmapLoadUtils.decode(imageFilePath, imageWidth, imageHeight);
			scale = scale * ((float) viewBitmap.getWidth() / (float) sourceBitmap.getWidth());
		}

		RectF viewImageRect = getBitmapRect();

        float x = Math.abs(viewImageRect.left - mCropRect.left) / scale;
        float y = Math.abs(viewImageRect.top - mCropRect.top) / scale;
        float actualCropWidth = mCropRect.width() / scale;
        float actualCropHeight = mCropRect.height() / scale;

		if(x < 0){
			x = 0;
		}

		if(y < 0){
			y = 0;
		}

		if( y + actualCropHeight > sourceBitmap.getHeight()){
			actualCropHeight = sourceBitmap.getHeight() - y;
		}

		if( x + actualCropWidth > sourceBitmap.getWidth()){
			actualCropWidth = sourceBitmap.getWidth() - x;
		}

        return Bitmap.createBitmap(sourceBitmap, (int) x, (int) y, (int) actualCropWidth, (int) actualCropHeight);
    }

	public Bitmap getViewBitmap(){
		return ((FastBitmapDrawable)getDrawable()).getBitmap();
	}

	public void setGridInnerMode(int gridInnerMode) {
		this.gridInnerMode = gridInnerMode;
		invalidate();
	}

	public void setGridOuterMode(int gridOuterMode) {
		this.gridOuterMode = gridOuterMode;
		invalidate();
	}
}
