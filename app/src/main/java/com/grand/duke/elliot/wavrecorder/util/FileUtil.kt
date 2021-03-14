package com.grand.duke.elliot.wavrecorder.util

import android.content.Context
import android.media.MediaMetadataRetriever
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.lang.Exception

object FileUtil {
    const val blank = ""

    fun delete(file: File): Boolean {
        if (file.exists())
            return file.delete()

        return false
    }

    fun renameTo(src: File, dest: File): Boolean = src.renameTo(dest)

    fun getAudioFileMetadata(audioFile: File): AudioFileMetaData? {
        return try {
            val fileInputStream = FileInputStream(audioFile)
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(fileInputStream.fd)
            val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: blank
            AudioFileMetaData(
                    name = audioFile.name,
                    duration = duration ?: "",//?.toLong() ?: 0L,
                    date = audioFile.lastModified()
            )
        } catch (e: RuntimeException) {
            Timber.e(e)
            null
        }
    }

    fun getWavFileNames(context: Context): List<String>? {
        val dir = File(context.getExternalFilesDir(null).toString())
        return dir.listFiles()?.map { it.name }
    }
}

data class AudioFileMetaData (
    val name: String,
    val duration: String,
    val date: Long
)