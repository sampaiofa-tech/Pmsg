package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pmsg", appName)
  }

  @Test
  fun `schedule periodic cleanup worker initializes cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.worker.ExpiredMessageCleanupWorker.schedulePeriodicCleanup(context)
    com.example.data.worker.ExpiredMessageCleanupWorker.runImmediateCleanup(context)
  }

  @Test
  fun `showNewConversationNotification does not notify when notify_on_new_conversation is disabled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.NotificationHelper.showNewConversationNotification(
      context = context,
      contactName = "Juliana",
      previewText = "Teste de mensagem",
      roomId = "room_1"
    )
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val activeNotifications = notificationManager.activeNotifications
    assertEquals(0, activeNotifications.size)
  }
}
