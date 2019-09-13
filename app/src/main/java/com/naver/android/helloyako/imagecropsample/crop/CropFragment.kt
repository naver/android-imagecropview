package com.naver.android.helloyako.imagecropsample.crop

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.naver.android.helloyako.imagecropsample.databinding.FragmentCropBinding

class CropFragment : Fragment() {
    private lateinit var viewDataBinding: FragmentCropBinding
    private val viewModel = CropViewModel()
    val args: CropFragmentArgs by navArgs()

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
    }

    companion object {
        private const val TAG = "CropFragment"
    }
}