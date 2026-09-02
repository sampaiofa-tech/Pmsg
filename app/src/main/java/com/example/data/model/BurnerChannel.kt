package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

/**
 * Burner communication channel / contact.
 */
@Entity(tableName = "burner_channels")
data class BurnerChannel(
    @PrimaryKey
    val id: String,
    val name: String,
    val avatarColorHex: Long = 0xFF00E5FF,
    val securityTag: String = "E2EE Zero Trace",
    val channelCode: String = "VN-8942",
    val lastMessagePreview: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val defaultTtlHours: Float = 24f,
    val isPinned: Boolean = false
) {
    val customCode: String
        get() = channelCode

    fun remainingTime(currentTime: Long): Long {
        val expireTime = lastMessageTimestamp + (defaultTtlHours * 3600 * 1000).toLong()
        return (expireTime - currentTime).coerceAtLeast(0L)
    }

    fun formattedRemainingTime(currentTime: Long): String {
        val remaining = remainingTime(currentTime)
        if (remaining <= 0) return "00:00"

        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60

        return if (hours > 0) {
            String.format("%02dh %02dm", hours, minutes)
        } else {
            String.format("%02dm %02ds", minutes, seconds)
        }
    }
}
