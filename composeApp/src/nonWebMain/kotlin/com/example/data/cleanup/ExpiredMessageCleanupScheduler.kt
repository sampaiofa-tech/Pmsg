package com.example.data.cleanup

import com.example.data.local.ChannelDao
import com.example.data.local.MessageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Multiplatform Coroutine Scheduler for Ephemeral Message Cleanup.
 *
 * Runs periodically across all platforms (Android, Desktop, iOS, Web).
 *
 * ARCHITECTURAL NOTICE (Zero-Trace Threat Model):
 * Client-side purge provides defense-in-depth and local privacy.
 * For true zero-trace guarantees across distributed endpoints, authoritative
 * expiration MUST be enforced server-side:
 *  1. Firestore TTL Policy (automatic field-based deletion).
 *  2. Cloud Function / Backend worker executing crypto-shredding (overwriting
 *     ciphertexts with cryptographic noise before document deletion).
 */
class ExpiredMessageCleanupScheduler(
    private val messageDao: MessageDao,
    private val channelDao: ChannelDao,
    private val onAntiForensicVacuum: suspend () -> Unit = {}
) {
    private var schedulerJob: Job? = null

    data class CleanupReport(
        val timestamp: Long,
        val purgedMessagesCount: Int,
        val purgedChannelsCount: Int
    )

    suspend fun executeCleanupPass(currentTime: Long): CleanupReport {
        val cutoff24Hours = currentTime - (24 * 60 * 60 * 1000L)

        // 1. Purge messages that exceeded TTL or vanish timers
        val purgedByTtl = messageDao.purgeExpiredMessages(currentTime)

        // 2. Enforce 24-hour absolute cutoff
        val purgedBy24h = messageDao.deleteMessagesOlderThan(cutoff24Hours)

        // 3. Purge inactive channels older than 24h
        val purgedChannels = channelDao.deleteChannelsOlderThan(cutoff24Hours)

        val totalPurgedMessages = purgedByTtl + purgedBy24h
        if (totalPurgedMessages > 0 || purgedChannels > 0) {
            onAntiForensicVacuum()
        }

        return CleanupReport(
            timestamp = currentTime,
            purgedMessagesCount = totalPurgedMessages,
            purgedChannelsCount = purgedChannels
        )
    }

    fun startPeriodicScheduler(
        scope: CoroutineScope,
        intervalMillis: Long = 15 * 60 * 1000L,
        timeProvider: () -> Long
    ) {
        stopScheduler()
        schedulerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    executeCleanupPass(timeProvider())
                } catch (_: Throwable) {
                    // Suppress and continue periodic loop
                }
                delay(intervalMillis)
            }
        }
    }

    fun stopScheduler() {
        schedulerJob?.cancel()
        schedulerJob = null
    }
}
