package com.grand.duke.elliot.wavrecorder.audio_recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.WorkerThread
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class AudioRecorder private constructor() {
    private val isRecording: AtomicBoolean = AtomicBoolean(false)
    private var executorService: ExecutorService? = null

    @Synchronized
    fun start(
        sampleRateInHz: Int,
        sizeInBytes: Int,
        audioDataCallback: AudioDataCallback
    ): Boolean {
        stop()
        executorService = Executors.newSingleThreadExecutor()
        if (isRecording.compareAndSet(false, true)) {
            executorService?.execute(
                AudioRecordRunnable(
                    sampleRateInHz,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeInBytes,
                    audioDataCallback
                )
            )
            return true
        }

        return false
    }

    @Synchronized
    fun stop() {
        isRecording.compareAndSet(true, false)
        executorService?.let {
            it.shutdown()
            executorService = null
        }
    }

    interface AudioDataCallback {
        @WorkerThread
        fun onAudioData(audioData: ByteArray, sizeInBytes: Int)
        fun onError()
    }

    private inner class AudioRecordRunnable constructor(
        sampleRateInHz: Int,
        channelConfig: Int,
        private val audioFormat: Int,
        sizeInBytes: Int,
        audioDataCallback: AudioDataCallback
    ) :
        Runnable {
        private val audioRecord: AudioRecord
        private val audioDataCallback: AudioDataCallback
        private val audioBuffer: ByteArray
        private val audioData: ShortArray
        private val sizeInBytes: Int
        private val sizeInShorts: Int

        override fun run() {
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    audioRecord.startRecording()
                } catch (e: IllegalStateException) {
                    Timber.e(e, "startRecording failed: ${e.message}")
                    audioDataCallback.onError()
                    return
                }

                while (isRecording.get()) {
                    var read: Int
                    if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                        read = audioRecord.read(audioData, 0, sizeInShorts)

                        if (read > 0)
                            audioDataCallback.onAudioData(short2byte(audioData, read, audioBuffer), read * 2)
                        else {
                            onError(read)
                            break
                        }
                    } else {
                        read = audioRecord.read(audioBuffer, 0, sizeInBytes)

                        if (read > 0)
                            audioDataCallback.onAudioData(audioBuffer, read)
                        else {
                            onError(read)
                            break
                        }
                    }
                }
            }

            audioRecord.release()
        }

        private fun short2byte(shortArray: ShortArray, size: Int, byteArray: ByteArray): ByteArray {
            if (size > shortArray.size || size * 2 > byteArray.size)
                Timber.e("Too long shortArray.")

            for (i in 0 until size) {
                byteArray[i * 2] = (shortArray[i].toInt() and 0x00FF).toByte()
                byteArray[i * 2 + 1] = (shortArray[i].toInt() shr 8).toByte()
            }

            return byteArray
        }

        private fun onError(errorCode: Int) {
            if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
                Timber.e("Record failed: ERROR_INVALID_OPERATION")
                audioDataCallback.onError()
            } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
                Timber.e("Record failed: ERROR_BAD_VALUE")
                audioDataCallback.onError()
            }
        }

        init {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
            this.sizeInBytes = sizeInBytes
            sizeInShorts = this.sizeInBytes / 2
            audioBuffer = ByteArray(this.sizeInBytes)
            audioData = ShortArray(sizeInShorts)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                channelConfig,
                audioFormat, max(minBufferSize, this.sizeInBytes)
            )

            this.audioDataCallback = audioDataCallback
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioRecorder? = null

        fun getInstance(): AudioRecorder {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = AudioRecorder()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}