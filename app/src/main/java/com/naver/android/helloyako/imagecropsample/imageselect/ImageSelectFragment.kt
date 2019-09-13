package com.naver.android.helloyako.imagecropsample.imageselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.naver.android.helloyako.imagecropsample.databinding.FragmentImageSelectBinding

class ImageSelectFragment: Fragment() {
    private lateinit var viewDataBinding: FragmentImageSelectBinding
    private val viewModel = ImageSelectViewModel()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewDataBinding = FragmentImageSelectBinding.inflate(inflater, container, false). apply {
            viewmodel = viewModel
        }

        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
    }
}