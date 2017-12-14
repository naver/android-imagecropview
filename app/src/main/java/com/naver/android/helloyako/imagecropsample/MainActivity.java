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

package com.naver.android.helloyako.imagecropsample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.naver.android.helloyako.imagecrop.util.BitmapLoadUtils;


public class MainActivity extends Activity {

    public static final String TAG = "MainActivity";

    private static final int MAIN_ACTIVITY_REQUEST_STORAGE = RESULT_FIRST_USER;
    private static final int ACTION_REQUEST_GALLERY = 99;

    Button mGalleryButton;
    Button mEditButton;
    ImageView mImage;
    View mImageContainer;

    Uri mImageUri;

    int imageWidth, imageHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageWidth = 1000;
        imageHeight = 1000;

        mGalleryButton = (Button) findViewById(R.id.button1);
        mEditButton = (Button) findViewById(R.id.button2);
        mImage = ((ImageView) findViewById(R.id.image));
        mImageContainer = findViewById(R.id.image_container);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getPackageManager().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                initClickListener();
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MAIN_ACTIVITY_REQUEST_STORAGE);
        } else {
            initClickListener();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MAIN_ACTIVITY_REQUEST_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initClickListener();
                }
                break;
        }
    }

    private void initClickListener() {
        mGalleryButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });

        mEditButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mImageUri != null) {
                    startCrop(mImageUri);
                }
            }
        });

        mImageContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                findViewById(R.id.touch_me).setVisibility(View.GONE);
                Uri uri = pickRandomImage();
                if (uri != null) {
                    Log.d(TAG, "image uri: " + uri);
                    loadAsync(uri);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTION_REQUEST_GALLERY:
                    String filePath = "";
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        filePath = getRealPathFromURI_API19(this, data.getData());
                    }else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                        filePath = getRealPathFromURI_API11to18(this, data.getData());
                    }else {
                        filePath = getRealPathFromURI_BelowAPI11(this, data.getData());
                    }

                    Uri filePathUri = Uri.parse(filePath);
                    loadAsync(filePathUri);
                    break;
            }
        }
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        Intent chooser = Intent.createChooser(intent, "Choose a Picture");
        startActivityForResult(chooser, ACTION_REQUEST_GALLERY);
    }

    private void startCrop(Uri imageUri) {
        Intent intent = new Intent(MainActivity.this, CropActivity.class);
        intent.setData(imageUri);
        startActivity(intent);
    }

    private boolean setImageURI(final Uri uri, final Bitmap bitmap) {

        Log.d(TAG, "image size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        mImage.setImageBitmap(bitmap);
        mImage.setBackgroundDrawable(null);

        mEditButton.setEnabled(true);
        mImageUri = uri;

        return true;
    }

    private Uri pickRandomImage() {
        Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA},
                MediaStore.Images.ImageColumns.SIZE + ">?", new String[]{"90000"}, null);
        Uri uri = null;

        if (c != null) {
            int total = c.getCount();
            int position = (int) (Math.random() * total);
            Log.d(TAG, "pickRandomImage. total images: " + total + ", position: " + position);
            if (total > 0) {
                if (c.moveToPosition(position)) {
                    String data = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    uri = Uri.parse(data);
                    Log.d(TAG, uri.toString());
                }
            }
            c.close();
        }
        return uri;
    }

    private void loadAsync(final Uri uri) {
        Log.i(TAG, "loadAsync: " + uri);

        Drawable toRecycle = mImage.getDrawable();
        if (toRecycle != null && toRecycle instanceof BitmapDrawable) {
            if (((BitmapDrawable) mImage.getDrawable()).getBitmap() != null)
                ((BitmapDrawable) mImage.getDrawable()).getBitmap().recycle();
        }
        mImage.setImageDrawable(null);
        mImageUri = null;

        DownloadAsync task = new DownloadAsync();
        task.execute(uri);
    }

    class DownloadAsync extends AsyncTask<Uri, Void, Bitmap> implements DialogInterface.OnCancelListener {

        ProgressDialog mProgress;
        private Uri mUri;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgress = new ProgressDialog(MainActivity.this);
            mProgress.setIndeterminate(true);
            mProgress.setCancelable(true);
            mProgress.setMessage("Loading image...");
            mProgress.setOnCancelListener(this);
            mProgress.show();
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            mUri = params[0];

            Bitmap bitmap = null;

//            while (mImageContainer.getWidth() < 1) {
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            final int w = mImageContainer.getWidth();
//            Log.d(TAG, "width: " + w);
            bitmap = BitmapLoadUtils.decode(mUri.toString(), imageWidth, imageHeight, true);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);

            if (mProgress.getWindow() != null) {
                mProgress.dismiss();
            }

            if (result != null) {
                setImageURI(mUri, result);
            } else {
                Toast.makeText(MainActivity.this, "Failed to load image " + mUri, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Log.i(TAG, "onProgressCancel");
            this.cancel(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.i(TAG, "onCancelled");
        }

    }

    private String getPathFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = this.getContentResolver().query(uri, proj, null, null, null);
            if (cursor == null) {
                return null;
            }
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getRealPathFromURI_API19(Context context, Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        String result = null;

        CursorLoader cursorLoader = new CursorLoader(
                context,
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        if(cursor != null) {
            int columnIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(columnIndex);
        }
        return result;
    }

    public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        int columnIndex
                = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(columnIndex);
    }
}
