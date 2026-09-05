package com.example.util

import android.content.Context

/**
 * Holder for the Android Application Context to allow multiplatform storage services
 * to safely access SharedPreferences and local storage.
 */
object AndroidContextHolder {
    @Volatile
    var appContext: Context? = null
}
