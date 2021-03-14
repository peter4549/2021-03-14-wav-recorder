package com.grand.duke.elliot.wavrecorder.util

import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavHeader {
    fun updateWavHeader(wavFile: File) {
        val byteBuffer = ByteBuffer
            .allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt((wavFile.length() - 8).toInt())
            .putInt((wavFile.length() - 44).toInt())
            .array()

        val randomAccessFile = RandomAccessFile(wavFile, "rw")
        try {
            randomAccessFile.seek(4)
            randomAccessFile.write(byteBuffer, 0, 4)
            randomAccessFile.seek(40)
            randomAccessFile.write(byteBuffer, 4, 4)

        } catch (e: IOException) {
            throw e
        } finally {
            try {
                randomAccessFile.close()
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    fun writeWavHeader(
        wavFile: RandomAccessFile,
        /** Mono: 1, Stereo: 2 */ channels: Short,
        sampleRateInHz: Int,
        bitDepth: Short
    ) {
        val wavHeader = ByteBuffer
            .allocate(14)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(channels)
            .putInt(sampleRateInHz)
            .putInt(sampleRateInHz * channels * (bitDepth / 8))
            .putShort((channels * (bitDepth / 8)).toShort())
            .putShort(bitDepth)
            .array()
        wavFile.write(
            byteArrayOf(
                'R'.toByte(), 'I'.toByte(), 'F'.toByte(), 'F'.toByte(),  // Chunk ID
                0, 0, 0, 0,  // Chunk Size
                'W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte(),  // Format
                'f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte(),  // Sub-chunk1 ID
                16, 0, 0, 0,  // Sub-chunk1 Size
                1, 0,  // Audio Format
                wavHeader[0], wavHeader[1],  // Num Channels
                wavHeader[2], wavHeader[3], wavHeader[4], wavHeader[5],  // Sample Rate
                wavHeader[6], wavHeader[7], wavHeader[8], wavHeader[9],  // Byte Rate
                wavHeader[10], wavHeader[11],  // Block Align
                wavHeader[12], wavHeader[13],  // Bits Per Sample
                'd'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte(),  // Sub-chunk2 ID
                0, 0, 0, 0  // Sub-chunk2 Size
            )
        )
    }
}