package com.naver.android.helloyako.imagecropsample.crop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naver.android.helloyako.imagecropsample.Event

class CropViewModel : ViewModel() {

    private val _ratioEvent = MutableLiveData<Event<Pair<Int, Int>>>()
    val ratioEvent: LiveData<Event<Pair<Int, Int>>> = _ratioEvent
    private val _cropEvent = MutableLiveData<Event<Unit>>()
    val cropEvent: LiveData<Event<Unit>> = _cropEvent
    private val _saveEvent = MutableLiveData<Event<Unit>>()
    val saveEvent: LiveData<Event<Unit>> = _saveEvent
    private val _restoreEvent = MutableLiveData<Event<Unit>>()
    val restoreEvent: LiveData<Event<Unit>> = _restoreEvent


    fun onClickRatio(width: Int, height: Int) {
        _ratioEvent.value = Event(Pair(width, height))
    }

    fun onClickCrop() {
        _cropEvent.value = Event(Unit)
    }

    fun onClickSave() {
        _saveEvent.value = Event(Unit)
    }

    fun onClickRestore() {
        _restoreEvent.value = Event(Unit)
    }

}