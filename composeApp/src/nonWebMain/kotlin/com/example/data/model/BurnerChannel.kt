package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Burner communication channel / contact.
 */
@Serializable
@Entity(tableName = "burner_channels")
data class BurnerChannel(
    @PrimaryKey
    val id: String,
    val name: String,
    val avatarColorHex: Long = 0xFF00E5FF,
    val securityTag: String = "E2EE Zero Trace",
    val channelCode: String = "VN-8942",
    val lastMessagePreview: String = "",
    val lastMessageTimestamp: Long = 0L,
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

        val totalSeconds = remaining / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val hStr = hours.toString().padStart(2, '0')
        val mStr = minutes.toString().padStart(2, '0')
        val sStr = seconds.toString().padStart(2, '0')

        return if (hours > 0) {
            "${hStr}h ${mStr}m"
        } else {
            "${mStr}m ${sStr}s"
        }
    }
}
