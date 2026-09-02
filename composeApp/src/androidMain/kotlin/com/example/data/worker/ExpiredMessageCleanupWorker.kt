package com.example.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.VanishDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that periodically cleans up and deletes messages from the
 * Room database once their expiration time has passed.
 *
 * It purges:
 *  1. Messages that exceeded their TTL (expiresAt <= currentTime)
 *  2. Messages with vanish-after-read whose timer completed
 *  3. View-once messages that have been opened/viewed
 *  4. Messages marked as shredded
 *  5. Messages older than the strict 24-hour absolute cutoff
 *  6. Any local temporary media files associated with the expired messages
 *
 * Operates silently in the background with zero trace.
 */
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

            // 1. Identify all expired messages before deletion to clean up any physical files
            val expiredMessages = database.messageDao().getExpiredMessages(now)
            val olderMessages = database.messageDao().getMessagesOlderThan(cutoff24Hours)
            val allMessagesToPurge = (expiredMessages + olderMessages).distinctBy { it.id }

            // 2. Erase associated local media files (zero traces left in storage)
            var deletedMediaFilesCount = 0
            for (msg in allMessagesToPurge) {
                msg.mediaUri?.let { uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        if (uri.scheme == "file") {
                            uri.path?.let { path ->
                                val file = File(path)
                                if (file.exists() && file.delete()) {
                                    deletedMediaFilesCount++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to clean physical media file for message ${msg.id}: ${e.message}")
                    }
                }
            }

            // 3. Purge expired, vanished, and shredded messages from Room database
            val purgedByTtl = database.messageDao().purgeExpiredMessages(now)
            // Explicitly purge anything older than 24 hours
            val purgedBy24h = database.messageDao().deleteMessagesOlderThan(cutoff24Hours)
            // Delete channels/conversations whose activity is older than 24 hours
            val purgedChannels = database.channelDao().deleteChannelsOlderThan(cutoff24Hours)

            val totalPurged = purgedByTtl + purgedBy24h

            // 4. Save cleanup telemetry in SharedPreferences for UI status verification
            val prefs = applicationContext.getSharedPreferences("vanish_cleanup_prefs", Context.MODE_PRIVATE)
            val currentLifetime = prefs.getLong("total_lifetime_purged", 0L)
            prefs.edit()
                .putLong("last_worker_run_time", now)
                .putInt("last_worker_purged_count", totalPurged)
                .putInt("last_worker_purged_media", deletedMediaFilesCount)
                .putLong("total_lifetime_purged", currentLifetime + totalPurged)
                .apply()

            Log.i(
                TAG,
                "ExpiredMessageCleanupWorker completed successfully. " +
                        "Purged messages: $totalPurged (TTL/vanished: $purgedByTtl, 24h cutoff: $purgedBy24h), " +
                        "Deleted media files: $deletedMediaFilesCount, Purged channels: $purgedChannels"
            )

            val outputData = workDataOf(
                KEY_PURGED_COUNT to totalPurged,
                KEY_TIMESTAMP to now,
                KEY_PURGED_MEDIA to deletedMediaFilesCount
            )
            Result.success(outputData)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up expired messages in background Worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ExpiredMessageWorker"
        const val PERIODIC_WORK_NAME = "periodic_expired_messages_cleanup_worker"
        const val ONE_TIME_WORK_NAME = "one_time_expired_messages_cleanup_worker"

        const val KEY_PURGED_COUNT = "key_purged_count"
        const val KEY_TIMESTAMP = "key_timestamp"
        const val KEY_PURGED_MEDIA = "key_purged_media"

        /**
         * Schedules a periodic worker to run every 15 minutes (WorkManager minimum interval)
         * to automatically delete all expired messages from the Room database.
         */
        fun schedulePeriodicCleanup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<ExpiredMessageCleanupWorker>(
                    15, TimeUnit.MINUTES,
                    5, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .addTag(PERIODIC_WORK_NAME)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRequest
                )
                Log.d(TAG, "Periodic cleanup worker registered (15-min interval).")
            } catch (e: Throwable) {
                Log.w(TAG, "Could not register periodic cleanup worker: ${e.message}")
            }
        }

        /**
         * Runs an immediate one-time cleanup pass to wipe any messages that have passed their TTL.
         */
        fun runImmediateCleanup(context: Context) {
            try {
                val oneTimeRequest = OneTimeWorkRequestBuilder<ExpiredMessageCleanupWorker>()
                    .addTag(ONE_TIME_WORK_NAME)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
                Log.d(TAG, "Immediate cleanup worker enqueued.")
            } catch (e: Throwable) {
                Log.w(TAG, "Could not enqueue immediate cleanup worker: ${e.message}")
            }
        }
    }
}
