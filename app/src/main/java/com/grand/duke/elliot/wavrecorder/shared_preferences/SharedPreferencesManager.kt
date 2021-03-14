package com.grand.duke.elliot.wavrecorder.shared_preferences

import android.app.Application
import android.content.Context

class SharedPreferencesManager private constructor(private val application: Application){

    fun putPlayingSpeed(speed: Float) {
        val sharedPreferences = application.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putFloat(KEY_PLAYING_SPEED, speed).apply()
    }

    fun getPlayingSpeed(): Float {
        val sharedPreferences = application.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getFloat(KEY_PLAYING_SPEED, 1F)
    }

    companion object {
        @Volatile
        private var INSTANCE: SharedPreferencesManager? = null

        fun getInstance(application: Application): SharedPreferencesManager {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = SharedPreferencesManager(application)
                    INSTANCE = instance
                }

                return instance
            }
        }

        private const val SHARED_PREFERENCES_NAME = "com.grand.duke.elliot.wavrecorder.shared_preferences" +
                ".shared_preferences_name"
        private const val KEY_PLAYING_SPEED = "com.grand.duke.elliot.wavrecorder.shared_preferences" +
                ".key_playing_speed"
    }
}