package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VanishDatabase
import com.example.data.model.BurnerChannel
import com.example.data.model.EphemeralMessage
import com.example.data.model.PmsgContact
import com.example.data.repository.ChatRepository
import com.example.util.ContactsHelper
import com.example.util.NotificationHelper
import com.example.util.security.CryptoManager
import com.example.util.security.SecurePrefsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TtlPreset(
    val label: String,
    val hours: Float,
    val description: String
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    // Hardware-backed cryptographic status
    val isHardwareBackedCrypto: Boolean = CryptoManager.isHardwareBacked()

    // Live continuous time ticker (updates every 500ms for smooth live countdowns)
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // Currently selected channel/room
    private val _selectedChannel = MutableStateFlow<BurnerChannel?>(null)
    val selectedChannel: StateFlow<BurnerChannel?> = _selectedChannel.asStateFlow()

    // Contact list with Pmsg status discovery
    private val _contacts = MutableStateFlow<List<PmsgContact>>(emptyList())
    val contacts: StateFlow<List<PmsgContact>> = _contacts.asStateFlow()

    // Current TTL mode (Default is strictly 24 hours as requested by user)
    val ttlPresets = listOf(
        TtlPreset("24 Horas", 24f, "Expiração padrão de 24 horas (Zero Rastro)"),
        TtlPreset("12 Horas", 12f, "Expiração média de 12 horas"),
        TtlPreset("6 Horas", 6f, "Expiração intermediária de 6 horas"),
        TtlPreset("1 Hora", 1f, "Expiração curta de 1 hora"),
        TtlPreset("5 Min", 0.08333f, "Expiração rápida de 5 minutos"),
        TtlPreset("1 Minuto", 0.01667f, "Expiração de 1 minuto"),
        TtlPreset("30 Segundos", 0.00833f, "Queima e some em 30 segundos"),
        TtlPreset("10 Segundos", 0.00277f, "Queima ultrarrápida em 10 segundos")
    )

    private val _selectedTtl = MutableStateFlow(ttlPresets[0])
    val selectedTtl: StateFlow<TtlPreset> = _selectedTtl.asStateFlow()

    // Anti-screenshot / FLAG_SECURE status (default false for streaming emulator preview)
    private val _screenProtectionEnabled = MutableStateFlow(false)
    val screenProtectionEnabled: StateFlow<Boolean> = _screenProtectionEnabled.asStateFlow()

    // Screenshot Detection & Prevention
    private val _screenshotDetectionEnabled = MutableStateFlow(SecurePrefsHelper.isScreenshotDetectionEnabled(application))
    val screenshotDetectionEnabled: StateFlow<Boolean> = _screenshotDetectionEnabled.asStateFlow()

    private val _blockSensitiveOnScreenshot = MutableStateFlow(SecurePrefsHelper.isBlockSensitiveOnScreenshotEnabled(application))
    val blockSensitiveOnScreenshot: StateFlow<Boolean> = _blockSensitiveOnScreenshot.asStateFlow()

    private val _isScreenshotLockdownActive = MutableStateFlow(false)
    val isScreenshotLockdownActive: StateFlow<Boolean> = _isScreenshotLockdownActive.asStateFlow()

    private val _lastScreenshotDetectedTime = MutableStateFlow(0L)
    val lastScreenshotDetectedTime: StateFlow<Long> = _lastScreenshotDetectedTime.asStateFlow()

    // Read Receipts & Disappearing Effect Settings
    private val _readReceiptsEnabled = MutableStateFlow(SecurePrefsHelper.isReadReceiptsEnabled(application))
    val readReceiptsEnabled: StateFlow<Boolean> = _readReceiptsEnabled.asStateFlow()

    // Vanish after read preset in seconds (0 = follow room TTL, 5, 10, 30, 60 seconds)
    private val _vanishAfterReadPresetSeconds = MutableStateFlow(SecurePrefsHelper.getVanishAfterReadSeconds(application))
    val vanishAfterReadPresetSeconds: StateFlow<Int> = _vanishAfterReadPresetSeconds.asStateFlow()

    // Biometric / PIN App Lock
    private val _biometricLockEnabled = MutableStateFlow(SecurePrefsHelper.isBiometricLockEnabled(application))
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    // Shake to Clear feature settings
    private val _shakeToClearEnabled = MutableStateFlow(SecurePrefsHelper.isShakeToClearEnabled(application))
    val shakeToClearEnabled: StateFlow<Boolean> = _shakeToClearEnabled.asStateFlow()

    private val _shakeSensitivity = MutableStateFlow(SecurePrefsHelper.getShakeSensitivity(application))
    val shakeSensitivity: StateFlow<String> = _shakeSensitivity.asStateFlow()

    private val _shakeRequiresConfirmation = MutableStateFlow(SecurePrefsHelper.isShakeRequiresConfirmation(application))
    val shakeRequiresConfirmation: StateFlow<Boolean> = _shakeRequiresConfirmation.asStateFlow()

    private val _shakeDialogVisible = MutableStateFlow(false)
    val shakeDialogVisible: StateFlow<Boolean> = _shakeDialogVisible.asStateFlow()

    private val _shakeWipeEventTimestamp = MutableStateFlow(0L)
    val shakeWipeEventTimestamp: StateFlow<Long> = _shakeWipeEventTimestamp.asStateFlow()

    // Auto-lock feature (locks automatically when time expires, default 5 minutes)
    private val _autoLockEnabled = MutableStateFlow(SecurePrefsHelper.isAutoLockEnabled(application))
    val autoLockEnabled: StateFlow<Boolean> = _autoLockEnabled.asStateFlow()

    // Auto-lock timeout in minutes (default 5 minutes)
    private val _autoLockTimeoutMinutes = MutableStateFlow(SecurePrefsHelper.getAutoLockTimeoutMinutes(application))
    val autoLockTimeoutMinutes: StateFlow<Int> = _autoLockTimeoutMinutes.asStateFlow()

    // Track user activity timestamp for auto-lock
    private var lastUserActivityTime = System.currentTimeMillis()

    // Current unlock status
    private val _isAppUnlocked = MutableStateFlow(
        if (SecurePrefsHelper.isBiometricLockEnabled(application) || SecurePrefsHelper.isAutoLockEnabled(application)) {
            false
        } else {
            true
        }
    )
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Status snackbar / banner message
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    // Active Channels from Room
    val channels: StateFlow<List<BurnerChannel>>

    // Active non-expired messages for currently selected channel
    val activeMessages: StateFlow<List<EphemeralMessage>>

    init {
        val db = VanishDatabase.getDatabase(application, viewModelScope)
        repository = ChatRepository(db.messageDao(), db.channelDao())

        channels = repository.allChannels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Combine selectedChannel with currentTime ticker to auto-evaporate messages
        activeMessages = _selectedChannel.flatMapLatest { channel ->
            if (channel == null) {
                MutableStateFlow(emptyList())
            } else {
                repository.getAllMessagesForRoom(channel.id)
            }
        }.combine(_currentTime) { messages, now ->
            // Filter out any message whose TTL has expired or that has been marked shredded
            messages.filter { !it.isExpired(now) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load initial device contacts
        refreshContacts()

        // Start background purge and live countdown ticker
        startPurgeAndTickerLoop()
    }

    fun refreshContacts() {
        viewModelScope.launch {
            val list = ContactsHelper.getDevicePmsgContacts(getApplication())
            _contacts.value = list
        }
    }

    private fun startPurgeAndTickerLoop() {
        viewModelScope.launch {
            var counter = 0
            while (true) {
                val now = System.currentTimeMillis()
                _currentTime.value = now
                counter++

                // Every 2 seconds, execute physical zero-trace database purge of expired records
                if (counter % 4 == 0) {
                    repository.purgeExpired(now)
                }

                // Check auto-lock timer if enabled and app is currently unlocked
                if (_autoLockEnabled.value && _isAppUnlocked.value) {
                    val timeoutMs = _autoLockTimeoutMinutes.value * 60 * 1000L
                    if (now - lastUserActivityTime >= timeoutMs) {
                        _isAppUnlocked.value = false
                    }
                }

                delay(500)
            }
        }
    }

    /**
     * Record user interaction (tap, scroll, message send) to reset the auto-lock countdown timer
     */
    fun onUserInteraction() {
        lastUserActivityTime = System.currentTimeMillis()
    }

    fun selectChannel(channel: BurnerChannel?) {
        _selectedChannel.value = channel
        if (channel != null) {
            val matched = ttlPresets.find { it.hours == channel.defaultTtlHours }
                ?: TtlPreset("${channel.defaultTtlHours}h", channel.defaultTtlHours, "Expiração de ${channel.defaultTtlHours}h")
            _selectedTtl.value = matched
            // Mark incoming unread messages as read when opening channel
            viewModelScope.launch {
                repository.markIncomingMessagesAsRead(channel.id)
            }
        }
    }

    fun startChatWithContact(contact: PmsgContact) {
        viewModelScope.launch {
            val existing = channels.value.find { it.name.equals(contact.name, ignoreCase = true) }
            if (existing != null) {
                _selectedChannel.value = existing
                repository.markIncomingMessagesAsRead(existing.id)
                showFeedback("Conectado à conversa existente com ${contact.name}")
            } else {
                val code = contact.phoneNumber.filter { it.isDigit() }.takeLast(4).ifEmpty { "0000" }
                val newChannel = repository.createBurnerChannel(
                    name = contact.name,
                    customCode = code,
                    ttlHours = _selectedTtl.value.hours
                )
                _selectedChannel.value = newChannel
                showFeedback("🔒 Nova conversa segura com ${contact.name} iniciada!")
            }
        }
    }

    /**
     * Simulates an incoming NEW conversation from an external contact.
     * This will trigger the dedicated New Conversation Notification as requested by the user.
     */
    fun simulateIncomingNewConversation(
        senderName: String = "Juliana Vieira (Pmsg)",
        initialMessage: String = "Oi! Vi que você tem o Pmsg instalado. Conversa iniciada com expiração de 24h 🔒"
    ) {
        viewModelScope.launch {
            val newChannel = repository.createBurnerChannel(
                name = senderName,
                customCode = "PMSG",
                ttlHours = _selectedTtl.value.hours
            )
            repository.sendMessage(
                roomId = newChannel.id,
                senderId = newChannel.id,
                senderName = senderName,
                text = initialMessage,
                ttlHours = _selectedTtl.value.hours
            )

            // Strictly notify ONLY on the arrival of new conversations
            NotificationHelper.showNewConversationNotification(
                context = getApplication(),
                contactName = senderName,
                previewText = initialMessage,
                roomId = newChannel.id
            )

            showFeedback("💬 Nova conversa recebida de $senderName!")
        }
    }

    fun setTtl(preset: TtlPreset) {
        _selectedTtl.value = preset
        showFeedback("⏱️ Tempo de expiração configurado para: ${preset.label}")
    }

    fun setReadReceiptsEnabled(enabled: Boolean) {
        _readReceiptsEnabled.value = enabled
        SecurePrefsHelper.setReadReceiptsEnabled(getApplication(), enabled)
        showFeedback(if (enabled) "✓✓ Confirmação de Leitura Ativada" else "Confirmação de Leitura Desativada")
    }

    fun setVanishAfterReadPresetSeconds(seconds: Int) {
        _vanishAfterReadPresetSeconds.value = seconds
        SecurePrefsHelper.setVanishAfterReadSeconds(getApplication(), seconds)
        val desc = when (seconds) {
            0 -> "Desativado (segue TTL padrão)"
            5 -> "5 segundos após leitura"
            10 -> "10 segundos após leitura"
            30 -> "30 segundos após leitura"
            60 -> "1 minuto após leitura"
            else -> "$seconds segundos"
        }
        showFeedback("🔥 Auto-desaparecimento configurado para: $desc")
    }

    fun toggleScreenProtection() {
        val newVal = !_screenProtectionEnabled.value
        _screenProtectionEnabled.value = newVal
        showFeedback(if (newVal) "🛡️ Proteção de tela (Anti-Print) Ativada" else "⚠️ Proteção de tela Desativada")
    }

    fun setShakeToClearEnabled(enabled: Boolean) {
        _shakeToClearEnabled.value = enabled
        SecurePrefsHelper.setShakeToClearEnabled(getApplication(), enabled)
        showFeedback(if (enabled) "📳 Shake-to-Clear ATIVADO: chacoalhe o dispositivo para limpar o chat!" else "Shake-to-Clear Desativado")
    }

    fun setShakeSensitivity(sensitivity: String) {
        _shakeSensitivity.value = sensitivity
        SecurePrefsHelper.setShakeSensitivity(getApplication(), sensitivity)
        val desc = when (sensitivity) {
            "HIGH" -> "Alta (sensível)"
            "LOW" -> "Baixa (requer chacoalhada forte)"
            else -> "Média (padrão)"
        }
        showFeedback("Sensibilidade do Shake: $desc")
    }

    fun setShakeRequiresConfirmation(requiresConfirmation: Boolean) {
        _shakeRequiresConfirmation.value = requiresConfirmation
        SecurePrefsHelper.setShakeRequiresConfirmation(getApplication(), requiresConfirmation)
        showFeedback(if (requiresConfirmation) "Shake-to-Clear: Pedir confirmação antes de apagar" else "Shake-to-Clear: Limpeza instantânea ativada (Zero Trace)")
    }

    fun dismissShakeDialog() {
        _shakeDialogVisible.value = false
    }

    fun onDeviceShaken() {
        if (!_shakeToClearEnabled.value) return
        val currentChannel = _selectedChannel.value
        if (currentChannel != null) {
            if (_shakeRequiresConfirmation.value) {
                _shakeDialogVisible.value = true
            } else {
                wipeActiveChatHistory()
            }
        } else {
            showFeedback("📳 Chacoalhar detectado! Entre em uma conversa para limpar o histórico instantaneamente.")
        }
    }

    fun wipeActiveChatHistory() {
        val channel = _selectedChannel.value ?: return
        _shakeDialogVisible.value = false
        _shakeWipeEventTimestamp.value = System.currentTimeMillis()
        viewModelScope.launch {
            repository.clearChatHistory(channel.id)
            showFeedback("⚡ SHAKE TO CLEAR: Histórico da conversa incinerado sem deixar vestígios!")
        }
    }

    fun simulateShake() {
        onDeviceShaken()
    }

    fun sendMessage(
        text: String,
        isBurnerNote: Boolean = false,
        customTtlHours: Float? = null,
        isViewOnce: Boolean = false,
        disappearAfterReadSeconds: Int? = null
    ) {
        val channel = _selectedChannel.value ?: return
        if (text.isBlank()) return

        val ttl = customTtlHours ?: _selectedTtl.value.hours
        val vanishSeconds = disappearAfterReadSeconds ?: _vanishAfterReadPresetSeconds.value
        viewModelScope.launch {
            val sentMsg = repository.sendMessage(
                roomId = channel.id,
                senderId = "ME",
                senderName = "Você",
                text = text.trim(),
                ttlHours = ttl,
                mediaType = if (isBurnerNote) "BURNER_NOTE" else "TEXT",
                isViewOnce = isViewOnce,
                disappearAfterReadSeconds = vanishSeconds
            )
            if (isViewOnce) {
                showFeedback("👁️ Mensagem de visualização única enviada!")
            }

            // Simulate realistic delivery and recipient read receipt with disappearing trigger
            if (_readReceiptsEnabled.value && !isViewOnce) {
                scheduleRecipientReadSimulation(sentMsg.id)
            }
        }
    }

    fun sendAudioMessage(
        durationSeconds: Int,
        audioUri: String? = null,
        isViewOnce: Boolean = false,
        customTtlHours: Float? = null,
        disappearAfterReadSeconds: Int? = null
    ) {
        val channel = _selectedChannel.value ?: return
        val ttl = customTtlHours ?: _selectedTtl.value.hours
        val vanishSeconds = disappearAfterReadSeconds ?: _vanishAfterReadPresetSeconds.value
        viewModelScope.launch {
            val sentMsg = repository.sendMessage(
                roomId = channel.id,
                senderId = "ME",
                senderName = "Você",
                text = "",
                ttlHours = ttl,
                mediaType = "AUDIO",
                mediaUri = audioUri ?: "audio://voice_note_${System.currentTimeMillis()}.m4a",
                fileName = "Nota_Voz_${durationSeconds}s.m4a",
                fileSize = "${(durationSeconds * 16)} KB",
                audioDurationSeconds = durationSeconds,
                isViewOnce = isViewOnce,
                disappearAfterReadSeconds = vanishSeconds
            )
            if (isViewOnce) {
                showFeedback("🎤 Áudio de visualização única enviado!")
            } else {
                showFeedback("🎤 Mensagem de voz enviada (${durationSeconds}s)!")
                if (_readReceiptsEnabled.value) {
                    scheduleRecipientReadSimulation(sentMsg.id)
                }
            }
        }
    }

    fun sendMediaMessage(
        mediaType: String,
        mediaUri: String,
        fileName: String? = null,
        fileSize: String? = null,
        caption: String = "",
        isViewOnce: Boolean = false,
        customTtlHours: Float? = null,
        disappearAfterReadSeconds: Int? = null
    ) {
        val channel = _selectedChannel.value ?: return
        val ttl = customTtlHours ?: _selectedTtl.value.hours
        val vanishSeconds = disappearAfterReadSeconds ?: _vanishAfterReadPresetSeconds.value
        viewModelScope.launch {
            val sentMsg = repository.sendMessage(
                roomId = channel.id,
                senderId = "ME",
                senderName = "Você",
                text = caption.trim(),
                ttlHours = ttl,
                mediaType = mediaType,
                mediaUri = mediaUri,
                fileName = fileName,
                fileSize = fileSize,
                isViewOnce = isViewOnce,
                disappearAfterReadSeconds = vanishSeconds
            )
            val mediaLabel = when (mediaType) {
                "IMAGE" -> "Foto"
                "VIDEO" -> "Vídeo"
                "AUDIO" -> "Áudio"
                else -> "Arquivo"
            }
            if (isViewOnce) {
                showFeedback("👁️ $mediaLabel de visualização única enviado!")
            } else {
                showFeedback("📎 $mediaLabel criptografado enviado com sucesso!")
                if (_readReceiptsEnabled.value) {
                    scheduleRecipientReadSimulation(sentMsg.id)
                }
            }
        }
    }

    /**
     * Schedules realistic recipient read receipt simulation with visual progression
     */
    private fun scheduleRecipientReadSimulation(messageId: Long) {
        viewModelScope.launch {
            delay(800)
            repository.markMessageDelivered(messageId)
            delay(2000)
            repository.markMessageRead(messageId)
        }
    }

    fun markMessageAsRead(messageId: Long) {
        viewModelScope.launch {
            repository.markMessageRead(messageId)
            showFeedback("👁️ Mensagem lida pelo destinatário • Desaparecimento ativado.")
        }
    }

    fun simulateRecipientRead(messageId: Long) {
        viewModelScope.launch {
            repository.markMessageRead(messageId)
            showFeedback("👁️ Destinatário leu a mensagem! Efeito de desaparecimento ativado.")
        }
    }

    fun markMessageViewed(messageId: Long) {
        viewModelScope.launch {
            repository.markMessageViewed(messageId)
        }
    }

    /**
     * Simulates an ephemeral reply inside an EXISTING conversation.
     * As per user requirement, ongoing messages in existing chats do not trigger notifications.
     */
    fun simulateContactReply(customReplyText: String? = null) {
        val channel = _selectedChannel.value ?: return

        val replies = listOf(
            "Mensagem recebida e criptografada. O cronômetro de 24h já está rodando ⏳",
            "Entendido! Assim que o tempo expirar, essa mensagem desaparecerá sem deixar registros.",
            "Confirmado. Nenhum rastro deixado na memória ou no disco 🛡️",
            "Chave de sessão atualizada. Autodestruição programada com sucesso.",
            "Protocolo de segurança ativo. Mensagem será vaporizada."
        )

        val replyContent = customReplyText ?: replies.random()

        viewModelScope.launch {
            delay(600) // Realistic typing delay
            repository.sendMessage(
                roomId = channel.id,
                senderId = channel.id,
                senderName = channel.name,
                text = replyContent,
                ttlHours = _selectedTtl.value.hours
            )
        }
    }

    fun shredMessage(messageId: Long) {
        viewModelScope.launch {
            repository.shredMessage(messageId)
            showFeedback("🔥 Mensagem destruída e apagada sem deixar vestígios.")
        }
    }

    fun incinerateRoom(roomId: String) {
        viewModelScope.launch {
            repository.incinerateRoom(roomId)
            if (_selectedChannel.value?.id == roomId) {
                _selectedChannel.value = null
            }
            showFeedback("💥 Conversa e todas as mensagens foram apagadas permanentemente sem deixar rastros!")
        }
    }

    fun createBurnerChannel(name: String, code: String = "", ttlHours: Float = 24f) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val finalTtl = if (ttlHours > 0f) ttlHours else 24f
            val newChannel = repository.createBurnerChannel(
                name = name.trim(),
                customCode = code.trim(),
                ttlHours = finalTtl
            )
            val matchedPreset = ttlPresets.find { it.hours == finalTtl }
                ?: TtlPreset("${finalTtl}h", finalTtl, "Expiração de ${finalTtl}h")
            _selectedTtl.value = matchedPreset
            _selectedChannel.value = newChannel
            val timeLabel = matchedPreset.label
            showFeedback("✨ Nova conversa '${newChannel.name}' criada com incineração em $timeLabel!")
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            repository.deleteChannel(channelId)
            if (_selectedChannel.value?.id == channelId) {
                _selectedChannel.value = null
            }
            showFeedback("🗑️ Canal e histórico apagados permanentemente.")
        }
    }

    fun panicWipeAll() {
        viewModelScope.launch {
            repository.panicWipeAll()
            SecurePrefsHelper.panicWipeAllPrefs(getApplication())
            _selectedChannel.value = null
            showFeedback("🚨 PÂNICO EXECUTADO: Todo o banco, chaves mestras e dados foram vaporizados com Zero Rastro!")
        }
    }

    fun triggerTestNotification() {
        simulateIncomingNewConversation(
            senderName = "Marcos Vinicius (Pmsg)",
            initialMessage = "Olá! Nova conversa protegida iniciada no Pmsg. Expira em 24h."
        )
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        SecurePrefsHelper.setBiometricLockEnabled(getApplication(), enabled)
        if (!enabled && !_autoLockEnabled.value) {
            _isAppUnlocked.value = true
        }
        showFeedback(if (enabled) "🔒 Desbloqueio biométrico ATIVADO para abrir o Pmsg." else "🔓 Desbloqueio biométrico DESATIVADO.")
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        _autoLockEnabled.value = enabled
        SecurePrefsHelper.setAutoLockEnabled(getApplication(), enabled)
        if (enabled) {
            lastUserActivityTime = System.currentTimeMillis()
            showFeedback("⏱️ Auto-bloqueio ATIVADO: app bloqueará a cada ${_autoLockTimeoutMinutes.value} min de inatividade.")
        } else {
            showFeedback("🔓 Auto-bloqueio desativado.")
        }
    }

    fun setAutoLockTimeoutMinutes(minutes: Int) {
        val validMinutes = minutes.coerceIn(1, 60)
        _autoLockTimeoutMinutes.value = validMinutes
        SecurePrefsHelper.setAutoLockTimeoutMinutes(getApplication(), validMinutes)
        lastUserActivityTime = System.currentTimeMillis()
        val timeLabel = when (validMinutes) {
            1 -> "1 minuto"
            5 -> "5 minutos (Padrão)"
            else -> "$validMinutes minutos"
        }
        showFeedback("⏱️ Tempo de auto-bloqueio alterado para $timeLabel.")
    }

    fun setSecurityPin(pin: String) {
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            SecurePrefsHelper.setPin(getApplication(), pin)
            showFeedback("🔑 Novo PIN de 4 dígitos criptografado e salvo com sucesso!")
        }
    }

    fun verifySecurityPin(pin: String): Boolean {
        return SecurePrefsHelper.verifyPin(getApplication(), pin)
    }

    fun setScreenshotDetectionEnabled(enabled: Boolean) {
        _screenshotDetectionEnabled.value = enabled
        SecurePrefsHelper.setScreenshotDetectionEnabled(getApplication(), enabled)
        showFeedback(if (enabled) "📸 Detecção de capturas de tela ATIVADA." else "📸 Detecção de capturas DESATIVADA.")
    }

    fun setBlockSensitiveOnScreenshot(enabled: Boolean) {
        _blockSensitiveOnScreenshot.value = enabled
        SecurePrefsHelper.setBlockSensitiveOnScreenshotEnabled(getApplication(), enabled)
        showFeedback(if (enabled) "🛡️ Bloqueio preventivo de conteúdo sensível ATIVADO." else "🛡️ Bloqueio preventivo DESATIVADO.")
    }

    fun onScreenshotDetected() {
        if (!_screenshotDetectionEnabled.value) return

        val now = System.currentTimeMillis()
        _lastScreenshotDetectedTime.value = now

        if (_blockSensitiveOnScreenshot.value) {
            _isScreenshotLockdownActive.value = true
        }

        val activeChannel = _selectedChannel.value
        if (activeChannel != null) {
            viewModelScope.launch {
                // Post a security alert message directly into the chat timeline
                repository.sendMessage(
                    roomId = activeChannel.id,
                    senderId = "SECURITY_SYSTEM",
                    senderName = "🛡️ Alerta do Sistema",
                    text = "⚠️ Captura de tela detectada nesta sala! O conteúdo sensível de visualização única foi protegido preventivamente.",
                    ttlHours = _selectedTtl.value.hours,
                    mediaType = "SECURITY_ALERT"
                )
            }
            showFeedback("📸 ALERTA DE SEGURANÇA: Captura de tela detectada nesta conversa!")
        } else {
            showFeedback("📸 Captura de tela detectada no aplicativo.")
        }
    }

    fun dismissScreenshotLockdown() {
        _isScreenshotLockdownActive.value = false
        showFeedback("🔓 Bloqueio de segurança pós-captura liberado.")
    }

    fun simulateScreenshotDetection() {
        onScreenshotDetected()
    }

    fun unlockApp() {
        lastUserActivityTime = System.currentTimeMillis()
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        _isAppUnlocked.value = false
    }

    fun showFeedback(message: String) {
        _userFeedback.value = message
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }
}
