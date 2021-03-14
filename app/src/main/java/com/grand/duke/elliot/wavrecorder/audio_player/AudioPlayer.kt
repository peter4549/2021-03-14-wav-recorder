package com.grand.duke.elliot.wavrecorder.audio_player

import android.media.*
import timber.log.Timber
import kotlin.math.max

class AudioPlayer private constructor() {
    private lateinit var audioTrack: AudioTrack

    @Synchronized
    fun init(sampleRateInHz: Int, bufferSize: Int, speed: Float) {
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO  // Not AudioFormat.CHANNEL_IN_MONO.
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRateInHz,
            channelConfig,
            audioFormat
        )

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setChannelMask(channelConfig)
                .setSampleRate(sampleRateInHz)
                .setEncoding(audioFormat)
                .build(),
            max(minBufferSize, bufferSize).times(2),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val playbackParams = PlaybackParams()
            playbackParams.speed = speed
            audioTrack.playbackParams = playbackParams
        }

        audioTrack.play()
    }

    @Synchronized
    fun play(audioData: ByteArray, sizeInBytes: Int): Boolean {
        if (this::audioTrack.isInitialized) {
            try {
                when (audioTrack.write(audioData, 0, sizeInBytes)) {
                    AudioTrack.ERROR_INVALID_OPERATION -> {
                        Timber.e("Play failed: ERROR_INVALID_OPERATION")
                        return false
                    }
                    AudioTrack.ERROR_BAD_VALUE -> {
                        Timber.e("Play failed: ERROR_BAD_VALUE")
                        return false
                    }
                    AudioTrack.ERROR_DEAD_OBJECT -> {
                        Timber.e("Play failed: ERROR_DEAD_OBJECT")
                        return false
                    }
                }
            } catch (e: IllegalStateException) {
                Timber.e("Play failed: ${e.message}")
                return false
            }
        }

        Timber.w("Play failed: audioTrack not initialized.")
        return false
    }

    fun release() {
        if (this::audioTrack.isInitialized)
            audioTrack.release()
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayer? = null

        fun release() {
            INSTANCE?.release()
            INSTANCE = null
        }

        fun getInstance(): AudioPlayer {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = AudioPlayer()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}