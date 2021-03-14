package com.grand.duke.elliot.wavrecorder.audio_player

import androidx.lifecycle.ViewModel
import com.grand.duke.elliot.wavrecorder.audio_recorder.SAMPLE_RATE_IN_HZ
import com.grand.duke.elliot.wavrecorder.audio_recorder.minBufferSize
import com.grand.duke.elliot.wavrecorder.util.byte2short
import com.grand.duke.elliot.wavrecorder.wav_file_list.WavFile
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioPlaybackViewModel: ViewModel() {

    private lateinit var wavFile: WavFile
    private val amplitudes = arrayListOf<Int>()

    val sampleRateInHz = SAMPLE_RATE_IN_HZ
    val bufferSize = minBufferSize()
    val buffer = ByteArray(bufferSize)

    var audioFile: RandomAccessFile? = null
    val audioPlayer: AudioPlayer = AudioPlayer.getInstance()
    var state = AudioPlaybackActivity.State.Initialized

    fun openAudioFile(): Boolean {
        return try {
            audioFile = RandomAccessFile(wavFile.path, "rw")
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

    fun setWavFile(wavFile: WavFile) {
        this.wavFile = wavFile
    }

    fun getAmplitudes(): ArrayList<Int> {
        if (this::wavFile.isInitialized) {
            if (openAudioFile()) {
                audioFile?.let { audioFile ->
                    audioFile.seek(44)  // Wav header.

                    while (audioFile.read(buffer) > 0) {
                        byte2short(buffer).maxOrNull()?.let {
                            amplitudes.add(it.toInt())
                        }
                    }
                }
            }
        }

        return amplitudes
    }
}