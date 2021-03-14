package com.grand.duke.elliot.wavrecorder.util

import android.content.res.Resources
import android.os.Environment
import com.grand.duke.elliot.wavrecorder.main.APPLICATION_DIR_PATH
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

fun Float.isZero() = this == 0F
fun Int.isZero() = this == 0
fun Int.isNotZero() = isZero().not()

fun Float.toDp(): Float {
    return this / Resources.getSystem().displayMetrics.density
}

fun Int.toDp(): Float {
    return this / Resources.getSystem().displayMetrics.density
}

fun Float.toPx(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Int.toPx(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Long.toDateFormat(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(
    this
)

fun String.fileName() = this.substring(this.lastIndexOf("/"))

@Suppress("DEPRECATION")
fun applicationDir() = Environment.getExternalStorageDirectory().toString() + "/$APPLICATION_DIR_PATH"

fun byte2short(byteArray: ByteArray): ShortArray {
    val shortArray = ShortArray(byteArray.size / 2)
    ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
    return shortArray
}