package com.naver.android.helloyako.imagecropsample.crop

import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.naver.android.helloyako.imagecrop.model.ViewState
import com.naver.android.helloyako.imagecropsample.EventObserver
import com.naver.android.helloyako.imagecropsample.R
import com.naver.android.helloyako.imagecropsample.databinding.FragmentCropBinding
import kotlinx.android.synthetic.main.fragment_crop.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CropFragment : Fragment() {
    private lateinit var viewDataBinding: FragmentCropBinding
    private val viewModel = CropViewModel()
    private val args: CropFragmentArgs by navArgs()
    private var viewState: ViewState? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewDataBinding = FragmentCropBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }
        Log.d(TAG, "uri : ${args.uri}")
        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
        viewDataBinding.imageCropView.setImageFilePath(args.uri.toString())
        viewDataBinding.imageCropView.setAspectRatio(1, 1)
        setEventObserver()
    }


    private fun setEventObserver() {
        viewModel.ratioEvent.observe(this, EventObserver {
            val widthRatio = it.first
            val heightRatio = it.second
            if (isPossibleCrop(widthRatio, heightRatio)) {
                viewDataBinding.imageCropView.setAspectRatio(widthRatio, heightRatio)
            } else {
                Toast.makeText(context, R.string.can_not_crop, Toast.LENGTH_SHORT).show()
            }
        })
        viewModel.cropEvent.observe(this, EventObserver {
            if (!viewDataBinding.imageCropView.isChangingScale) {
                viewDataBinding.imageCropView.croppedImage?.let {
                    val file = bitmapConvertToFile(it)
                    Toast.makeText(context, file?.absolutePath ?: "", Toast.LENGTH_SHORT).show()
                }
            }

        })
        viewModel.saveEvent.observe(this, EventObserver {
            viewState = viewDataBinding.imageCropView.saveState()
            viewDataBinding.restoreBtn.isEnabled = true
        })
        viewModel.restoreEvent.observe(this, EventObserver {
            viewState?.let {
                viewDataBinding.imageCropView.restoreState(it)
            }
        })
    }

    private fun isPossibleCrop(widthRatio: Int, heightRatio: Int): Boolean {
        val bitmap = viewDataBinding.imageCropView.viewBitmap
        bitmap?.let {
            val width = it.width
            val height = it.height
            return !(width < widthRatio && height < heightRatio)
        }
        return false
    }

    private fun bitmapConvertToFile(bitmap: Bitmap): File? {
        var fileOutputStream: FileOutputStream? = null
        var bitmapFile: File? = null
        try {
            val file = File(Environment.getExternalStoragePublicDirectory("image_crop_sample"), "")
            if (!file.exists()) {
                file.mkdir()
            }

            bitmapFile = File(file, "IMG_" + SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().time) + ".jpg")
            fileOutputStream = FileOutputStream(bitmapFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            MediaScannerConnection.scanFile(context, arrayOf(bitmapFile.absolutePath), null, object : MediaScannerConnection.MediaScannerConnectionClient {
                override fun onMediaScannerConnected() {

                }

                override fun onScanCompleted(path: String, uri: Uri) {
                    activity?.runOnUiThread { Toast.makeText(context, "file saved", Toast.LENGTH_LONG).show() }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush()
                    fileOutputStream.close()
                } catch (e: Exception) {
                }

            }
        }

        return bitmapFile
    }

    companion object {
        private const val TAG = "CropFragment"
    }
}