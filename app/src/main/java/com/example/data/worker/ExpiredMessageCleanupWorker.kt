package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.VanishDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit

class ExpiredMessageCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val scope = CoroutineScope(Dispatchers.IO)
            val database = VanishDatabase.getDatabase(applicationContext, scope)
            val now = System.currentTimeMillis()
            val cutoff24Hours = now - (24 * 60 * 60 * 1000L)

            // Purge expired and shredded messages
            val purgedByTtl = database.messageDao().purgeExpiredMessages(now)
            // Explicitly purge anything older than 24 hours
            val purgedBy24h = database.messageDao().deleteMessagesOlderThan(cutoff24Hours)
            // Delete channels/conversations whose activity is older than 24 hours
            val purgedChannels = database.channelDao().deleteChannelsOlderThan(cutoff24Hours)

            val totalPurged = purgedByTtl + purgedBy24h
            Log.d(TAG, "ExpiredMessageCleanupWorker completed. Purged messages: $totalPurged, purged channels: $purgedChannels")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired messages in background Worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ExpiredMessageWorker"
        const val PERIODIC_WORK_NAME = "periodic_expired_messages_cleanup_worker"
        const val ONE_TIME_WORK_NAME = "one_time_expired_messages_cleanup_worker"

        /**
         * Schedules a periodic worker to run every 15 minutes (WorkManager minimum interval)
         * to automatically delete all expired messages and messages older than 24 hours.
         */
        fun schedulePeriodicCleanup(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<ExpiredMessageCleanupWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        /**
         * Runs an immediate one-time cleanup pass.
         */
        fun runImmediateCleanup(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<ExpiredMessageCleanupWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
