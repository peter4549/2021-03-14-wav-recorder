package com.grand.duke.elliot.wavrecorder.main

import android.app.Application
import androidx.lifecycle.ViewModel
import com.grand.duke.elliot.wavrecorder.audio_player.AudioPlayer
import com.grand.duke.elliot.wavrecorder.audio_recorder.AudioRecorder
import com.grand.duke.elliot.wavrecorder.util.toDateFormat
import timber.log.Timber
import java.io.IOException
import java.io.RandomAccessFile

class MainViewModel(application: Application): ViewModel() {

    val audioFilePath = createAudioFilePath(application)
    val audioPlayer: AudioPlayer = AudioPlayer.getInstance()
    val audioRecorder: AudioRecorder = AudioRecorder.getInstance()
    var audioFile: RandomAccessFile? = null
    var state = MainActivity.State.Initialized

    fun openAudioFile(): Boolean {
        return try {
            audioFile = RandomAccessFile(audioFilePath, "rw")
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun closeAudioFile() {
        try {
            audioFile?.close()
            audioFile = null
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    private fun createAudioFilePath(application: Application): String {
        val currentTimeString = System.currentTimeMillis().toDateFormat("yyyyMMddHHmmss")
        return application.getExternalFilesDir(null).toString() + "/${currentTimeString}.wav"
    }
}