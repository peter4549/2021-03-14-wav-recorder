package com.grand.duke.elliot.wavrecorder.wav_file_list

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WavFileListViewModelFactory(private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(WavFileListViewModel::class.java)) {
            return WavFileListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}