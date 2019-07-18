package com.naver.android.helloyako.imagecrop.model;

import android.graphics.Matrix;

public class ViewState {
    private Matrix matrix;
    private int aspectRatioWidth;
    private int aspectRatioHeight;
    private float[] suppMatrixValues;

    public ViewState(Matrix matrix, int aspectRatioWidth, int aspectRatioHeight, float[] suppMatrixValues) {
        this.matrix = matrix;
        this.aspectRatioWidth = aspectRatioWidth;
        this.aspectRatioHeight = aspectRatioHeight;
        this.suppMatrixValues = suppMatrixValues;
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public int getAspectRatioWidth() {
        return aspectRatioWidth;
    }

    public int getAspectRatioHeight() {
        return aspectRatioHeight;
    }

    public float[] getSuppMatrixValues() {
        return suppMatrixValues;
    }
}
