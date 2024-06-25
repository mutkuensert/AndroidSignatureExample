package com.mutkuensert.androidkeystoreexample

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _uiModel = MutableStateFlow(MainUiModel.initial())
    val uiModel = _uiModel.asStateFlow()

    fun createKeyPair() {

    }

    fun deleteEntry() {

    }

    fun signData() {

    }

    fun changeDataValue() {

    }
}