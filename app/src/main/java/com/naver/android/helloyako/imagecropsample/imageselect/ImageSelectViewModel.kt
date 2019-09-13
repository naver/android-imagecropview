package com.naver.android.helloyako.imagecropsample.imageselect

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naver.android.helloyako.imagecropsample.Event

class ImageSelectViewModel : ViewModel() {

    private val _chooseEvent = MutableLiveData<Event<Unit>>()
    val chooseEvent: LiveData<Event<Unit>> = _chooseEvent
    private val _chooseRandomEvent = MutableLiveData<Event<Unit>>()
    val chooseRandomEvent: LiveData<Event<Unit>> = _chooseRandomEvent
    private val _editEvent = MutableLiveData<Event<Unit>>()
    val editEvent: LiveData<Event<Unit>> = _editEvent

    fun onClickEditButton() {
        _editEvent.value = Event(Unit)
    }

    fun onClickChoose() {
        _chooseEvent.value = Event(Unit)
    }

    fun onClickChooseRandom() {
        _chooseRandomEvent.value = Event(Unit)
    }
}