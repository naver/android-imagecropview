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

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ViewConfiguration;

public class ImageCropView extends ImageCropViewBase {

	protected ScaleGestureDetector mScaleDetector;
	protected GestureDetector mGestureDetector;
	protected int mTouchSlop;
	protected float mScaleFactor;
	protected int mDoubleTapDirection;
	protected OnGestureListener mGestureListener;
	protected OnScaleGestureListener mScaleListener;
	protected boolean mDoubleTapEnabled = false;
	protected boolean mScaleEnabled = true;
	protected boolean mScrollEnabled = true;
	private OnImageViewTouchDoubleTapListener mDoubleTapListener;
	private OnImageViewTouchSingleTapListener mSingleTapListener;

    private boolean isChangingScale = false;

	public ImageCropView(Context context) {
		super(context);
	}

	public ImageCropView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageCropView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void init(Context context, AttributeSet attrs, int defStyle) {
		super.init(context, attrs, defStyle);
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		mGestureListener = getGestureListener();
		mScaleListener = getScaleListener();

		mScaleDetector = new ScaleGestureDetector(getContext(), mScaleListener);
		mGestureDetector = new GestureDetector(getContext(), mGestureListener, null, true);

		mDoubleTapDirection = 1;
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

	protected OnGestureListener getGestureListener() {
		return new GestureListener();
	}

	protected OnScaleGestureListener getScaleListener() {
		return new ScaleListener();
	}

	@Override
	protected void _setImageDrawable(final Drawable drawable, final Matrix initial_matrix, float min_zoom, float max_zoom) {
		super._setImageDrawable(drawable, initial_matrix, min_zoom, max_zoom);
		mScaleFactor = getMaxScale() / 3;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(getBitmapChanged()) return false;
		mScaleDetector.onTouchEvent(event);

		if (! mScaleDetector.isInProgress()) {
			mGestureDetector.onTouchEvent(event);
		}

		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_UP:
				return onUp(event);
		}
		return true;
	}


	@Override
	protected void onZoomAnimationCompleted(float scale) {

		if (LOG_ENABLED) {
			Log.d(LOG_TAG, "onZoomAnimationCompleted. scale: " + scale + ", minZoom: " + getMinScale());
		}

		if (scale < getMinScale()) {
			zoomTo(getMinScale(), 50);
		}
	}

	protected float onDoubleTapPost(float scale, float maxZoom) {
		if (mDoubleTapDirection == 1) {
			if ((scale + (mScaleFactor * 2)) <= maxZoom) {
				return scale + mScaleFactor;
			}
			else {
				mDoubleTapDirection = - 1;
				return maxZoom;
			}
		}
		else {
			mDoubleTapDirection = 1;
			return 1f;
		}
	}

	public boolean onSingleTapConfirmed(MotionEvent e) {
		return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		mUserScaled = true;
		scrollBy(- distanceX, - distanceY);
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
		if (getBitmapChanged()) return false;
		return true;
	}

	public boolean onUp(MotionEvent e) {
		if (getBitmapChanged()) return false;
		if (getScale() < getMinScale()) {
			zoomTo(getMinScale(), 50);
		}
		return true;
	}

	public boolean onSingleTapUp(MotionEvent e) {
		if (getBitmapChanged()) return false;
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
				Log.i(LOG_TAG, "onDoubleTap. double tap enabled? " + mDoubleTapEnabled);
			}
			if (mDoubleTapEnabled) {
				mUserScaled = true;
				float scale = getScale();
				float targetScale = scale;
                Log.d(LOG_TAG,"targetScale : " + targetScale);
				targetScale = onDoubleTapPost(scale, getMaxScale());
                Log.d(LOG_TAG,"targetScale : " + targetScale);
				targetScale = Math.min(getMaxScale(), Math.max(targetScale, getMinScale()));
                Log.d(LOG_TAG,"targetScale : " + targetScale);
				zoomTo(targetScale, e.getX(), e.getY(), DEFAULT_ANIMATION_DURATION);
                Log.d(LOG_TAG,"targetScale : " + targetScale);
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
				if (! mScaleDetector.isInProgress()) {
					setPressed(true);
					performLongClick();
				}
			}
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (! mScrollEnabled) return false;
			if (e1 == null || e2 == null) return false;
			if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
			if (mScaleDetector.isInProgress()) return false;
			return ImageCropView.this.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (! mScrollEnabled) return false;

			if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
			if (mScaleDetector.isInProgress()) return false;
//			if (getScale() == 1f) return false;

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
				if (! mScaled) mScaled = true;
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

    public boolean isChangingScale(){
        return isChangingScale;
    }
}
