package com.grand.duke.elliot.wavrecorder.audio_player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.grand.duke.elliot.wavrecorder.wav_file_list.WavFile

class AudioPlaybackViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(AudioPlaybackViewModel::class.java)) {
            return AudioPlaybackViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}