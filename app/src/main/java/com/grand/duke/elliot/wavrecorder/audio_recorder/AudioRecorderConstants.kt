package com.grand.duke.elliot.wavrecorder.audio_recorder

import android.media.AudioFormat
import android.media.AudioRecord

const val MONO: Short = 1
const val STEREO: Short = 2

const val BIT_DEPTH: Short = 16
const val SAMPLE_RATE_IN_HZ = 44100
const val UPDATE_INTERVAL_MILLISECONDS = 40L

const val TIMER_PATTERN = "mm:ss.SS"
const val TIMESTAMP_INTERVAL_MILLISECONDS = 2000L
const val TIMESTAMP_PATTERN = "mm:ss"

fun minBufferSize() =
        AudioRecord.getMinBufferSize(
                SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        )