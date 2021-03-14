package com.grand.duke.elliot.wavrecorder.wav_file_list

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import com.grand.duke.elliot.wavrecorder.util.FileUtil
import java.io.File

class WavFileListViewModel(application: Application): ViewModel() {
    private val dir = File(application.getExternalFilesDir(null).toString())
    private val files = dir.listFiles()
    val mediaPlayer = MediaPlayer()
    val wavFileList = arrayListOf<WavFile>()

    init {
        files?.let {
            for (file in files) {
                val audioFileMetadata = FileUtil.getAudioFileMetadata(file)
                audioFileMetadata?.let {
                    wavFileList.add(
                            WavFile(
                                    path = file.absolutePath,
                                    name = it.name,
                                    duration = audioFileMetadata.duration,
                                    date = audioFileMetadata.date
                            )
                    )
                }
            }
        }
    }
}