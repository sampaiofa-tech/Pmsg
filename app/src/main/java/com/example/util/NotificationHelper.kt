package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_NEW_CONVERSATIONS_ID = "pmsg_new_conversations_channel"

    private const val NOTIFICATION_ID_BASE = 2000
    private var notificationCounter = 0

    /**
     * Initializes notification channels for Android 8.0+ (Oreo+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel specifically and strictly for incoming new conversations
            val conversationsChannel = NotificationChannel(
                CHANNEL_NEW_CONVERSATIONS_ID,
                "Chegada de Novas Conversas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações exclusivas quando uma nova conversa for iniciada por um contato"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(conversationsChannel)
        }
    }

    /**
     * Checks if notification permission is granted.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Sends a push notification STRICTLY when a new conversation arrives.
     */
    fun showNewConversationNotification(
        context: Context,
        contactName: String,
        previewText: String,
        roomId: String = ""
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SELECTED_ROOM_ID", roomId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationCounter,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "💬 Nova conversa: $contactName"
        val content = if (previewText.isNotBlank()) previewText else "$contactName iniciou uma conversa efêmera protegida com você."

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_CONVERSATIONS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSubText("Pmsg (Nova Conversa)")
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationCounter = (notificationCounter + 1) % 50
            notificationManager.notify(NOTIFICATION_ID_BASE + notificationCounter, notification)
        } catch (e: SecurityException) {
            // Handled safely
        }
    }
}
