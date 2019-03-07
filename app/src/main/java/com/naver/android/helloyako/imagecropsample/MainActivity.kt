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

package com.naver.android.helloyako.imagecropsample

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.loader.content.CursorLoader
import com.naver.android.helloyako.imagecrop.util.BitmapLoadUtils

class MainActivity : Activity() {

    private var mGalleryButton: Button? = null
    private var mEditButton: Button? = null
    private var mImage: ImageView? = null
    private var mImageContainer: View? = null

    private var mImageUri: Uri? = null

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageWidth = 1000
        imageHeight = 1000

        mGalleryButton = findViewById(R.id.button1)
        mEditButton = findViewById(R.id.button2)
        mImage = findViewById(R.id.image)
        mImageContainer = findViewById(R.id.image_container)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (packageManager.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, packageName) == PackageManager.PERMISSION_GRANTED) {
                initClickListener()
            }
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MAIN_ACTIVITY_REQUEST_STORAGE)
        } else {
            initClickListener()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MAIN_ACTIVITY_REQUEST_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initClickListener()
            }
        }
    }

    private fun initClickListener() {
        mGalleryButton!!.setOnClickListener { v -> pickFromGallery() }

        mEditButton!!.setOnClickListener { v ->
            if (mImageUri != null) {
                startCrop(mImageUri!!)
            }
        }

        mImageContainer!!.setOnClickListener { v ->
            findViewById<View>(R.id.touch_me).visibility = View.GONE
            val uri = pickRandomImage()
            if (uri != null) {
                Log.d(TAG, "image uri: $uri")
                loadAsync(uri)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ACTION_REQUEST_GALLERY -> {
                    val filePath: String?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        filePath = getRealPathFromURI_API19(this, data.data)
                    } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                        filePath = getRealPathFromURI_API11to18(this, data.data)
                    } else {
                        filePath = getRealPathFromURI_BelowAPI11(this, data.data)
                    }

                    val filePathUri = Uri.parse(filePath)
                    loadAsync(filePathUri)
                }
            }
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        val chooser = Intent.createChooser(intent, "Choose a Picture")
        startActivityForResult(chooser, ACTION_REQUEST_GALLERY)
    }

    private fun startCrop(imageUri: Uri) {
        val intent = Intent(this@MainActivity, CropActivity::class.java)
        intent.data = imageUri
        startActivity(intent)
    }

    private fun setImageURI(uri: Uri?, bitmap: Bitmap) {
        Log.d(TAG, "image size: " + bitmap.width + "x" + bitmap.height)
        mImage!!.setImageBitmap(bitmap)
        mImage!!.setBackgroundDrawable(null)

        mEditButton!!.isEnabled = true
        mImageUri = uri
    }

    private fun pickRandomImage(): Uri? {
        val c = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA),
                MediaStore.Images.ImageColumns.SIZE + ">?", arrayOf("90000"), null)
        var uri: Uri? = null

        if (c != null) {
            val total = c.count
            val position = (Math.random() * total).toInt()
            Log.d(TAG, "pickRandomImage. total images: $total, position: $position")
            if (total > 0) {
                if (c.moveToPosition(position)) {
                    val data = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
                    uri = Uri.parse(data)
                    Log.d(TAG, uri!!.toString())
                }
            }
            c.close()
        }
        return uri
    }

    private fun loadAsync(uri: Uri) {
        Log.i(TAG, "loadAsync: $uri")

        val toRecycle = mImage!!.drawable
        if (toRecycle is BitmapDrawable) {
            if ((mImage!!.drawable as BitmapDrawable).bitmap != null)
                (mImage!!.drawable as BitmapDrawable).bitmap.recycle()
        }
        mImage!!.setImageDrawable(null)
        mImageUri = null

        val task = DownloadAsync()
        task.execute(uri)
    }

    internal inner class DownloadAsync : AsyncTask<Uri, Void, Bitmap>(), DialogInterface.OnCancelListener {

        var mProgress: ProgressDialog? = null
        private var mUri: Uri? = null

        override fun onPreExecute() {
            super.onPreExecute()

            mProgress = ProgressDialog(this@MainActivity)
            mProgress?.isIndeterminate = true
            mProgress?.setCancelable(true)
            mProgress?.setMessage("Loading image...")
            mProgress?.setOnCancelListener(this)
            mProgress?.show()
        }

        override fun doInBackground(vararg params: Uri): Bitmap {
            mUri = params[0]
            return BitmapLoadUtils.decode(mUri!!.toString(), imageWidth, imageHeight, true)
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)

            if (mProgress?.window != null) {
                mProgress!!.dismiss()
            }

            if (result != null) {
                setImageURI(mUri, result)
            } else {
                Toast.makeText(this@MainActivity, "Failed to load image " + mUri!!, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            Log.i(TAG, "onProgressCancel")
            this.cancel(true)
        }

        override fun onCancelled() {
            super.onCancelled()
            Log.i(TAG, "onCancelled")
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun getRealPathFromURI_API19(context: Context, uri: Uri?): String {
        var filePath = ""
        val wholeID = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"

        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null)

        val columnIndex = cursor!!.getColumnIndex(column[0])

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex)
        }
        cursor.close()
        return filePath
    }

    private fun getRealPathFromURI_API11to18(context: Context, contentUri: Uri?): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        var result: String? = null

        val cursorLoader = CursorLoader(
                context,
                contentUri!!, proj, null, null, null)
        val cursor = cursorLoader.loadInBackground()

        if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            result = cursor.getString(columnIndex)
        }
        return result
    }

    private fun getRealPathFromURI_BelowAPI11(context: Context, contentUri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
        val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(columnIndex)
    }

    companion object {

        private val TAG = "MainActivity"

        private val MAIN_ACTIVITY_REQUEST_STORAGE = Activity.RESULT_FIRST_USER
        private val ACTION_REQUEST_GALLERY = 99
    }
}
