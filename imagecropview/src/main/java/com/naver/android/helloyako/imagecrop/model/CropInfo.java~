package com.naver.android.helloyako.imagecrop.model;

import android.graphics.Bitmap;

import com.naver.android.helloyako.imagecrop.util.BitmapLoadUtils;

/**
 * Created by helloyako on 2016. 5. 10..
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class CropInfo {
    private float scale;
    private float viewBitmapWidth;
    private float viewImageTop;
    private float viewImageLeft;
    private float cropTop;
    private float cropLeft;

    private float cropWidth;
    private float cropHeight;


    public CropInfo(float scale, float viewBitmapWidth, float viewImageTop, float viewImageLeft, float cropTop, float cropLeft, float cropWidth, float cropHeight) {
        this.scale = scale;
        this.viewBitmapWidth = viewBitmapWidth;
        this.viewImageTop = viewImageTop;
        this.viewImageLeft = viewImageLeft;
        this.cropTop = cropTop;
        this.cropLeft = cropLeft;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
    }

    public Bitmap getCroppedImage(String path) {
        return getCroppedImage(path, 4000);
    }

    /**
     * @param reqSize for image sampling
     *
     */
    public Bitmap getCroppedImage(String path, int reqSize) {
        Bitmap bitmap = BitmapLoadUtils.decode(path, reqSize, reqSize);
        return getCroppedImage(bitmap);
    }

    public Bitmap getCroppedImage(Bitmap bitmap) {
        float scale = this.scale * (viewBitmapWidth / (float) bitmap.getWidth());
        float x = Math.abs(viewImageLeft - cropLeft) / scale;
        float y = Math.abs(viewImageTop - cropTop) / scale;
        float actualCropWidth = cropWidth / scale;
        float actualCropHeight = cropHeight / scale;

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if (y + actualCropHeight > bitmap.getHeight()) {
            actualCropHeight = bitmap.getHeight() - y;
        }

        if (x + actualCropWidth > bitmap.getWidth()) {
            actualCropWidth = bitmap.getWidth() - x;
        }

        return Bitmap.createBitmap(bitmap, (int) x, (int) y, (int) actualCropWidth, (int) actualCropHeight);
    }

    public float getScale() {
        return scale;
    }

    public float getViewBitmapWidth() {
        return viewBitmapWidth;
    }

    public float getViewImageTop() {
        return viewImageTop;
    }

    public float getViewImageLeft() {
        return viewImageLeft;
    }

    public float getCropTop() {
        return cropTop;
    }

    public float getCropLeft() {
        return cropLeft;
    }

    public float getCropWidth() {
        return cropWidth;
    }

    public float getCropHeight() {
        return cropHeight;
    }
}
