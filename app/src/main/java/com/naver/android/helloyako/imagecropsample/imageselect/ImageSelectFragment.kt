package com.naver.android.helloyako.imagecropsample.imageselect

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.loader.content.CursorLoader
import androidx.navigation.fragment.findNavController
import com.naver.android.helloyako.imagecrop.util.BitmapLoadUtils
import com.naver.android.helloyako.imagecropsample.EventObserver
import com.naver.android.helloyako.imagecropsample.R
import com.naver.android.helloyako.imagecropsample.databinding.FragmentImageSelectBinding

class ImageSelectFragment : Fragment() {
    private lateinit var viewDataBinding: FragmentImageSelectBinding
    private val viewModel = ImageSelectViewModel()
    private var mImageUri: Uri? = null
    private val imageWidth = 1000
    private val imageHeight = 1000

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewDataBinding = FragmentImageSelectBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
        setEventObserver()
    }

    private fun setEventObserver() {
        viewModel.chooseEvent.observe(this, EventObserver {
            chooseImage()
        })
        viewModel.editEvent.observe(this, EventObserver {
            var action = ImageSelectFragmentDirections.actionImageToCrop(mImageUri!!)
            findNavController().navigate(action)
        })

        viewModel.chooseRandomEvent.observe(this, EventObserver {
            chooseRandomImage()
        })

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CHOOSE_IMAGE_SELECT_REQUEST_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseImage()
            }
            RANDOM_IMAGE_SELECT_REQUEST_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseRandomImage()
            }
        }
    }

    private fun chooseRandomImage() {
        if (!checkPermission()) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), RANDOM_IMAGE_SELECT_REQUEST_STORAGE)
            return
        }

        pickRandomImage()?.let {
            Log.d(TAG, "image uri: $it")
            loadAsync(it)
        }
    }

    private fun chooseImage() {
        if (!checkPermission()) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), CHOOSE_IMAGE_SELECT_REQUEST_STORAGE)
            return
        }

        pickFromGallery()
    }

    private fun checkPermission(): Boolean {
        var result = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.let {
                result = it.packageManager.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, it.packageName) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            result = true
        }

        return result
    }

    private fun setImageURI(uri: Uri, bitmap: Bitmap) {
        Log.d(TAG, "image size: " + bitmap.width + "x" + bitmap.height)
        viewDataBinding.image?.setImageBitmap(bitmap)
        viewDataBinding.image?.setBackgroundDrawable(null)

        viewDataBinding.editButton?.isEnabled = true
        mImageUri = uri
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ACTION_REQUEST_GALLERY -> {
                    var filePath: String? = null
                    data?.data?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            filePath = getRealPathFromURI_API19(context!!, it)
                        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                            filePath = getRealPathFromURI_API11to18(context!!, it)
                        }

                        val filePathUri = Uri.parse(filePath)
                        loadAsync(filePathUri)
                    }
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

    private fun pickRandomImage(): Uri? {
        val c = activity?.contentResolver?.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA),
                MediaStore.Images.ImageColumns.SIZE + ">?", arrayOf("90000"), null)
        var uri: Uri? = null

        c?.let {
            val total = it.count
            val position = (Math.random() * total).toInt()
            Log.d(TAG, "pickRandomImage. total images: $total, position: $position")
            if (total > 0) {
                if (it.moveToPosition(position)) {
                    val data = it.getString(it.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
                    uri = Uri.parse(data)
                    Log.d(TAG, uri?.toString() ?: "")
                }
            }
            it.close()
        }

        return uri
    }

    private fun loadAsync(uri: Uri) {
        Log.i(TAG, "loadAsync: $uri")
        val task = DownloadAsync()
        task.execute(uri)
    }

    internal inner class DownloadAsync : AsyncTask<Uri, Void, Bitmap>(), DialogInterface.OnCancelListener {

        private var mProgress: ProgressDialog? = null
        private var mUri: Uri? = null

        override fun onPreExecute() {
            super.onPreExecute()

            mProgress = ProgressDialog(context)
            mProgress?.isIndeterminate = true
            mProgress?.setCancelable(true)
            mProgress?.setMessage("Loading image...")
            mProgress?.setOnCancelListener(this)
            mProgress?.show()
        }

        override fun doInBackground(vararg params: Uri): Bitmap? {
            mUri = params[0]

            var bitmap: Bitmap? = null
            mUri?.let {
                bitmap = BitmapLoadUtils.decode(it.toString(), imageWidth, imageHeight, true)
            }

            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)

            viewDataBinding.touchMe.visibility = View.GONE
            mProgress?.dismiss()
            result?.let {
                mUri?.let { uri ->
                    setImageURI(uri, result)
                }
            } ?: run {
                Toast.makeText(context, "Failed to load image " + mUri?.toString(), Toast.LENGTH_SHORT).show()
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
    private fun getRealPathFromURI_API19(context: Context, uri: Uri): String {
        var filePath = ""
        val wholeID = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"

        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null)

        cursor?.let {
            val columnIndex = it.getColumnIndex(column[0])

            if (it.moveToFirst()) {
                filePath = it.getString(columnIndex)
            }
            it.close()
        }

        return filePath
    }

    private fun getRealPathFromURI_API11to18(context: Context, uri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        var result: String? = null

        val cursorLoader = CursorLoader(
                context,
                uri, proj, null, null, null)
        val cursor = cursorLoader.loadInBackground()
        cursor?.let {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            result = it.getString(columnIndex)
        }

        return result
    }

    companion object {
        private const val TAG = "ImageSelectFragment"
        private const val CHOOSE_IMAGE_SELECT_REQUEST_STORAGE = Activity.RESULT_FIRST_USER
        private const val RANDOM_IMAGE_SELECT_REQUEST_STORAGE = Activity.RESULT_FIRST_USER + 1
        private const val ACTION_REQUEST_GALLERY = 99
    }
}