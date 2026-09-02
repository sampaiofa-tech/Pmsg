package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.worker.ExpiredMessageCleanupWorker
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.ChannelListScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.ContactsHelper
import com.example.util.NotificationHelper
import com.example.util.ScreenshotDetector
import com.example.util.ShakeDetector

class MainActivity : FragmentActivity() {

  private val viewModel: ChatViewModel by viewModels()
  private lateinit var screenshotDetector: ScreenshotDetector
  private lateinit var shakeDetector: ShakeDetector

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize notification channels
    NotificationHelper.createNotificationChannels(applicationContext)

    // Initialize screenshot detector
    screenshotDetector = ScreenshotDetector(this) {
      viewModel.onScreenshotDetected()
    }

    // Initialize shake detector for instant zero-trace chat wipe
    shakeDetector = ShakeDetector(this) {
      viewModel.onDeviceShaken()
    }

    // Initialize background Worker to automatically purge all Room messages older than 24h at regular intervals
    ExpiredMessageCleanupWorker.schedulePeriodicCleanup(applicationContext)
    ExpiredMessageCleanupWorker.runImmediateCleanup(applicationContext)

    handleIncomingRoomIntent(intent)

    setContent {
      val screenProtectionEnabled by viewModel.screenProtectionEnabled.collectAsStateWithLifecycle()
      val screenshotDetectionEnabled by viewModel.screenshotDetectionEnabled.collectAsStateWithLifecycle()
      val context = LocalContext.current

      var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
      }

      var hasContactsPermission by remember {
        mutableStateOf(ContactsHelper.hasContactsPermission(context))
      }

      val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
      ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
          viewModel.showFeedback("🔔 Notificações ativadas! (Avisos apenas de novas conversas)")
        } else {
          viewModel.showFeedback("⚠️ Notificações foram negadas pelo usuário.")
        }
      }

      val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
      ) { isGranted ->
        hasContactsPermission = isGranted
        viewModel.refreshContacts()
        if (isGranted) {
          viewModel.showFeedback("👥 Contatos sincronizados com sucesso!")
        } else {
          viewModel.showFeedback("⚠️ Permissão de contatos negada.")
        }
      }

      // Automatically request notification permission on Android 13+ on first launch
      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      // Dynamically toggle FLAG_SECURE to prevent screenshots and screen recordings
      LaunchedEffect(screenProtectionEnabled) {
        if (screenProtectionEnabled) {
          window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
          )
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
      }

      // Dynamically start/stop real-time screenshot detector
      LaunchedEffect(screenshotDetectionEnabled) {
        if (screenshotDetectionEnabled) {
          screenshotDetector.startListening()
        } else {
          screenshotDetector.stopListening()
        }
      }

      val shakeToClearEnabled by viewModel.shakeToClearEnabled.collectAsStateWithLifecycle()
      val shakeSensitivity by viewModel.shakeSensitivity.collectAsStateWithLifecycle()

      // Dynamically start/stop shake detector with adjusted sensitivity
      LaunchedEffect(shakeToClearEnabled, shakeSensitivity) {
        shakeDetector.sensitivityThreshold = when (shakeSensitivity) {
          "HIGH" -> 1.8f
          "LOW" -> 3.2f
          else -> 2.4f
        }
        if (shakeToClearEnabled) {
          shakeDetector.startListening()
        } else {
          shakeDetector.stopListening()
        }
      }

      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = ImmersiveSurface
        ) {
          VanishApp(
            viewModel = viewModel,
            notificationsEnabled = hasNotificationPermission,
            hasContactsPermission = hasContactsPermission,
            onRequestNotificationPermission = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
              }
            },
            onRequestContactsPermission = {
              contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
          )
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    if (::screenshotDetector.isInitialized && viewModel.screenshotDetectionEnabled.value) {
      screenshotDetector.startListening()
    }
    if (::shakeDetector.isInitialized && viewModel.shakeToClearEnabled.value) {
      shakeDetector.startListening()
    }
  }

  override fun onStop() {
    super.onStop()
    if (::screenshotDetector.isInitialized) {
      screenshotDetector.stopListening()
    }
    if (::shakeDetector.isInitialized) {
      shakeDetector.stopListening()
    }
  }

  override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
    viewModel.onUserInteraction()
    return super.dispatchTouchEvent(ev)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingRoomIntent(intent)
  }

  private fun handleIncomingRoomIntent(intent: Intent?) {
    val roomId = intent?.getStringExtra("SELECTED_ROOM_ID")
    if (!roomId.isNullOrBlank()) {
      val channel = viewModel.channels.value.find { it.id == roomId }
      if (channel != null) {
        viewModel.selectChannel(channel)
      }
    }
  }
}

@Composable
fun VanishApp(
  viewModel: ChatViewModel,
  notificationsEnabled: Boolean,
  hasContactsPermission: Boolean,
  onRequestNotificationPermission: () -> Unit,
  onRequestContactsPermission: () -> Unit
) {
  val channels by viewModel.channels.collectAsStateWithLifecycle()
  val contacts by viewModel.contacts.collectAsStateWithLifecycle()
  val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
  val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
  val selectedTtl by viewModel.selectedTtl.collectAsStateWithLifecycle()
  val screenProtectionEnabled by viewModel.screenProtectionEnabled.collectAsStateWithLifecycle()
  val screenshotDetectionEnabled by viewModel.screenshotDetectionEnabled.collectAsStateWithLifecycle()
  val blockSensitiveOnScreenshot by viewModel.blockSensitiveOnScreenshot.collectAsStateWithLifecycle()
  val isScreenshotLockdownActive by viewModel.isScreenshotLockdownActive.collectAsStateWithLifecycle()
  val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
  val autoLockEnabled by viewModel.autoLockEnabled.collectAsStateWithLifecycle()
  val autoLockTimeoutMinutes by viewModel.autoLockTimeoutMinutes.collectAsStateWithLifecycle()
  val securityPin by viewModel.securityPin.collectAsStateWithLifecycle()
  val readReceiptsEnabled by viewModel.readReceiptsEnabled.collectAsStateWithLifecycle()
  val vanishAfterReadPresetSeconds by viewModel.vanishAfterReadPresetSeconds.collectAsStateWithLifecycle()
  val shakeToClearEnabled by viewModel.shakeToClearEnabled.collectAsStateWithLifecycle()
  val shakeSensitivity by viewModel.shakeSensitivity.collectAsStateWithLifecycle()
  val shakeRequiresConfirmation by viewModel.shakeRequiresConfirmation.collectAsStateWithLifecycle()
  val shakeDialogVisible by viewModel.shakeDialogVisible.collectAsStateWithLifecycle()
  val shakeWipeEventTimestamp by viewModel.shakeWipeEventTimestamp.collectAsStateWithLifecycle()
  val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
  val userFeedback by viewModel.userFeedback.collectAsStateWithLifecycle()
  var isSettingsOpen by remember { mutableStateOf(false) }

  // If auto-lock or biometric lock is active and app is locked, display the Lock Screen
  if (!isAppUnlocked && (biometricLockEnabled || autoLockEnabled)) {
    BiometricLockScreen(
      securityPin = securityPin,
      biometricEnabled = biometricLockEnabled,
      autoLockTimeoutMinutes = autoLockTimeoutMinutes,
      onUnlocked = { viewModel.unlockApp() }
    )
    return
  }

  // Handle Android system back press
  BackHandler(enabled = isSettingsOpen || selectedChannel != null) {
    if (isSettingsOpen) {
      isSettingsOpen = false
    } else if (selectedChannel != null) {
      viewModel.selectChannel(null)
    }
  }

  // Sealed representation of current full page destination
  val currentScreen = when {
    isSettingsOpen -> "SETTINGS"
    selectedChannel != null -> "CHAT"
    else -> "CHANNEL_LIST"
  }

  AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
      if (targetState == "SETTINGS" || targetState == "CHAT") {
        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
          slideOutHorizontally { width -> -width / 3 } + fadeOut()
        )
      } else {
        (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
          slideOutHorizontally { width -> width } + fadeOut()
        )
      }
    },
    label = "screen_transition"
  ) { screenState ->
    when (screenState) {
      "SETTINGS" -> {
        SettingsScreen(
          screenProtectionEnabled = screenProtectionEnabled,
          screenshotDetectionEnabled = screenshotDetectionEnabled,
          blockSensitiveOnScreenshot = blockSensitiveOnScreenshot,
          biometricLockEnabled = biometricLockEnabled,
          autoLockEnabled = autoLockEnabled,
          autoLockTimeoutMinutes = autoLockTimeoutMinutes,
          securityPin = securityPin,
          readReceiptsEnabled = readReceiptsEnabled,
          vanishAfterReadPresetSeconds = vanishAfterReadPresetSeconds,
          shakeToClearEnabled = shakeToClearEnabled,
          shakeSensitivity = shakeSensitivity,
          shakeRequiresConfirmation = shakeRequiresConfirmation,
          notificationsEnabled = notificationsEnabled,
          onRequestNotificationPermission = onRequestNotificationPermission,
          onTestNotification = { viewModel.triggerTestNotification() },
          onToggleScreenProtection = { viewModel.toggleScreenProtection() },
          onToggleScreenshotDetection = { viewModel.setScreenshotDetectionEnabled(it) },
          onToggleBlockSensitiveOnScreenshot = { viewModel.setBlockSensitiveOnScreenshot(it) },
          onSimulateScreenshot = { viewModel.simulateScreenshotDetection() },
          onToggleBiometricLock = { viewModel.setBiometricLockEnabled(it) },
          onToggleAutoLock = { viewModel.setAutoLockEnabled(it) },
          onSetAutoLockTimeout = { viewModel.setAutoLockTimeoutMinutes(it) },
          onSetSecurityPin = { viewModel.setSecurityPin(it) },
          onToggleReadReceipts = { viewModel.setReadReceiptsEnabled(it) },
          onSetVanishAfterReadPresetSeconds = { viewModel.setVanishAfterReadPresetSeconds(it) },
          onToggleShakeToClear = { viewModel.setShakeToClearEnabled(it) },
          onSetShakeSensitivity = { viewModel.setShakeSensitivity(it) },
          onToggleShakeRequiresConfirmation = { viewModel.setShakeRequiresConfirmation(it) },
          onSimulateShake = { viewModel.onDeviceShaken() },
          onLockNow = { viewModel.lockApp() },
          onPanicWipe = { viewModel.panicWipeAll() },
          onBack = { isSettingsOpen = false }
        )
      }
      "CHAT" -> {
        selectedChannel?.let { targetChannel ->
          ChatScreen(
            channel = targetChannel,
            messages = activeMessages,
            currentTime = currentTime,
            selectedTtl = selectedTtl,
            ttlPresets = viewModel.ttlPresets,
            userFeedback = userFeedback,
            isScreenshotLockdownActive = isScreenshotLockdownActive,
            vanishAfterReadPresetSeconds = vanishAfterReadPresetSeconds,
            shakeDialogVisible = shakeDialogVisible,
            shakeWipeEventTimestamp = shakeWipeEventTimestamp,
            onSetVanishAfterReadPresetSeconds = { viewModel.setVanishAfterReadPresetSeconds(it) },
            onDismissScreenshotLockdown = { viewModel.dismissScreenshotLockdown() },
            onSimulateScreenshot = { viewModel.simulateScreenshotDetection() },
            onSimulateRead = { viewModel.simulateRecipientRead(it) },
            onTriggerShakeWipe = { viewModel.wipeActiveChatHistory() },
            onDismissShakeDialog = { viewModel.dismissShakeDialog() },
            onSimulateShake = { viewModel.onDeviceShaken() },
            onBack = { viewModel.selectChannel(null) },
            onSendMessage = { text, isBurnerNote, isViewOnce, customTtl ->
              viewModel.sendMessage(
                text = text,
                isBurnerNote = isBurnerNote,
                customTtlHours = customTtl,
                isViewOnce = isViewOnce
              )
            },
            onSendAudio = { duration, isViewOnce, customTtl ->
              viewModel.sendAudioMessage(
                durationSeconds = duration,
                isViewOnce = isViewOnce,
                customTtlHours = customTtl
              )
            },
            onSendMedia = { mediaType, mediaUri, fileName, fileSize, caption, isViewOnce, customTtl ->
              viewModel.sendMediaMessage(
                mediaType = mediaType,
                mediaUri = mediaUri,
                fileName = fileName,
                fileSize = fileSize,
                caption = caption,
                isViewOnce = isViewOnce,
                customTtlHours = customTtl
              )
            },
            onSimulateReply = { viewModel.simulateContactReply() },
            onShredMessage = { viewModel.shredMessage(it) },
            onIncinerateRoom = { viewModel.incinerateRoom(it) },
            onSelectTtl = { viewModel.setTtl(it) },
            onClearFeedback = { viewModel.clearFeedback() }
          )
        }
      }
      else -> {
        ChannelListScreen(
          channels = channels,
          contacts = contacts,
          currentTime = currentTime,
          screenProtectionEnabled = screenProtectionEnabled,
          biometricLockEnabled = biometricLockEnabled,
          autoLockEnabled = autoLockEnabled,
          autoLockTimeoutMinutes = autoLockTimeoutMinutes,
          securityPin = securityPin,
          notificationsEnabled = notificationsEnabled,
          hasContactsPermission = hasContactsPermission,
          userFeedback = userFeedback,
          onSelectChannel = { viewModel.selectChannel(it) },
          onCreateChannel = { name, code, ttlHours -> viewModel.createBurnerChannel(name, code, ttlHours) },
          onStartChatWithContact = { viewModel.startChatWithContact(it) },
          onDeleteChannel = { viewModel.deleteChannel(it) },
          onPanicWipe = { viewModel.panicWipeAll() },
          onToggleScreenProtection = { viewModel.toggleScreenProtection() },
          onToggleBiometricLock = { viewModel.setBiometricLockEnabled(it) },
          onToggleAutoLock = { viewModel.setAutoLockEnabled(it) },
          onSetAutoLockTimeout = { viewModel.setAutoLockTimeoutMinutes(it) },
          onSetSecurityPin = { viewModel.setSecurityPin(it) },
          onLockNow = { viewModel.lockApp() },
          onRequestNotificationPermission = onRequestNotificationPermission,
          onRequestContactsPermission = onRequestContactsPermission,
          onRefreshContacts = { viewModel.refreshContacts() },
          onSimulateIncomingNewConversation = { viewModel.simulateIncomingNewConversation() },
          onTestNotification = { viewModel.triggerTestNotification() },
          onOpenSettings = { isSettingsOpen = true },
          onClearFeedback = { viewModel.clearFeedback() }
        )
      }
    }
  }
}
