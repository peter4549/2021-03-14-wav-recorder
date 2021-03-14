package com.grand.duke.elliot.wavrecorder.wav_file_list

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WavFile(
        val path: String,
        val name: String,
        val duration: String,
        val date: Long
): Parcelable