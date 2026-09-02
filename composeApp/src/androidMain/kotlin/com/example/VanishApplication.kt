package com.example

import android.app.Application
import com.example.data.worker.ExpiredMessageCleanupWorker
import com.example.util.NotificationHelper

/**
 * Main Application class initializing essential services, notification channels,
 * and registering the background WorkManager periodic task for Room expired message cleanup.
 */
class VanishApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize notification channels
            NotificationHelper.createNotificationChannels(this)
        } catch (t: Throwable) {
            // Safe fallback for testing environments
        }

        try {
            // Schedule periodic WorkManager task to clean up expired messages in Room
            ExpiredMessageCleanupWorker.schedulePeriodicCleanup(this)

            // Run immediate cleanup pass on application startup
            ExpiredMessageCleanupWorker.runImmediateCleanup(this)
        } catch (t: Throwable) {
            // Safe fallback for testing environments
        }
    }
}
