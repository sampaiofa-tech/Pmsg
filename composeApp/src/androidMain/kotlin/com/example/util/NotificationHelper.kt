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
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_NEW_CONVERSATIONS_ID = "pmsg_new_conversations_channel"

    private const val NOTIFICATION_ID_BASE = 2000
    private var notificationCounter = 0

    /**
     * Initializes notification channels for Android 8.0+ (Oreo+) with native system properties.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel specifically for incoming conversations matching Android system standards
            val conversationsChannel = NotificationChannel(
                CHANNEL_NEW_CONVERSATIONS_ID,
                "Mensagens e Conversas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de novas conversas e mensagens recebidas no Pmsg"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
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
     * Sends a push notification formatted as a native Android Messaging notification.
     * Default behavior adheres strictly to user directive: não notificar quando uma nova conversa for iniciada.
     */
    fun showNewConversationNotification(
        context: Context,
        contactName: String,
        previewText: String,
        roomId: String = ""
    ) {
        val prefs = context.getSharedPreferences("pmsg_app_prefs", Context.MODE_PRIVATE)
        val notifyEnabled = prefs.getBoolean("notify_on_new_conversation", false)
        if (!notifyEnabled) {
            // Strictly do not notify when a new conversation is initiated
            return
        }

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

        val cleanContent = if (previewText.isNotBlank()) previewText else "Nova conversa protegida iniciada"

        val senderPerson = Person.Builder()
            .setName(contactName)
            .setKey(contactName)
            .build()

        val userPerson = Person.Builder()
            .setName("Você")
            .setKey("user_me")
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(contactName)
            .addMessage(
                NotificationCompat.MessagingStyle.Message(
                    cleanContent,
                    System.currentTimeMillis(),
                    senderPerson
                )
            )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_CONVERSATIONS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setContentTitle(contactName)
            .setContentText(cleanContent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF22C55E.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationCounter = (notificationCounter + 1) % 50
            notificationManager.notify(NOTIFICATION_ID_BASE + notificationCounter, notification)
        } catch (e: Throwable) {
            // Handled safely across all OEM implementations
        }
    }
}
