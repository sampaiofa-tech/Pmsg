package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PmsgLogoBadge
import com.example.ui.components.PmsgWordmark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveHeader
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOnlineGreen
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface

import com.example.util.security.SecurePrefsHelper

@Composable
fun BiometricLockScreen(
    onVerifyPin: ((String) -> Boolean)? = null,
    biometricEnabled: Boolean = true,
    autoLockTimeoutMinutes: Int = 5,
    onDuressTriggered: (() -> Unit)? = null,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var isPinMode by remember { mutableStateOf(!biometricEnabled) }
    var pinInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authSuccess by remember { mutableStateOf(false) }
    var hasAttemptedAutoPrompt by remember { mutableStateOf(false) }

    // Pulsing biometric ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun triggerHapticFeedback(isError: Boolean = false) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isError) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isError) 150 else 50)
            }
        } catch (_: Exception) {}
    }

    var lockoutRemainingSeconds by remember { mutableIntStateOf(SecurePrefsHelper.getLockoutRemainingSeconds(context)) }

    // Lockout countdown timer
    LaunchedEffect(lockoutRemainingSeconds) {
        if (lockoutRemainingSeconds > 0) {
            delay(1000L)
            lockoutRemainingSeconds = SecurePrefsHelper.getLockoutRemainingSeconds(context)
        }
    }

    fun performBiometricAuth() {
        if (SecurePrefsHelper.isLockedOut(context)) {
            lockoutRemainingSeconds = SecurePrefsHelper.getLockoutRemainingSeconds(context)
            authError = "Aplicativo bloqueado por tentativas incorretas. Aguarde ${lockoutRemainingSeconds}s."
            isPinMode = true
            return
        }

        isAuthenticating = true
        authError = null

        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null && BiometricAuthHelper.isBiometricAvailable(context)) {
            BiometricAuthHelper.promptBiometric(
                activity = fragmentActivity,
                title = "Desbloquear Pmsg",
                subtitle = "Use sua impressão digital ou reconhecimento facial",
                negativeButtonText = "Usar PIN",
                onSuccess = {
                    triggerHapticFeedback(false)
                    authSuccess = true
                    authError = null
                    onUnlocked()
                },
                onError = { errorCode, errString ->
                    isAuthenticating = false
                    isPinMode = true
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                        authError = "Biometria indisponível ($errString). Digite o PIN de segurança."
                    }
                },
                onFailed = {
                    triggerHapticFeedback(true)
                    authError = "Biometria não reconhecida. Tente novamente ou use o PIN."
                    isAuthenticating = false
                }
            )
        } else {
            // If biometric hardware is unavailable or not enrolled, switch securely to PIN mode
            isAuthenticating = false
            isPinMode = true
            if (biometricEnabled) {
                authError = "Biometria não cadastrada no dispositivo. Digite o PIN de segurança."
            }
        }
    }

    // Auto-launch biometric prompt when screen appears
    LaunchedEffect(Unit) {
        if (biometricEnabled && !hasAttemptedAutoPrompt && !isPinMode && !SecurePrefsHelper.isLockedOut(context)) {
            hasAttemptedAutoPrompt = true
            performBiometricAuth()
        }
    }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 4) {
            val result = SecurePrefsHelper.verifyPinWithRateLimit(context, pinInput)
            when (result) {
                is com.example.util.security.PinValidationResult.Success -> {
                    triggerHapticFeedback(false)
                    authSuccess = true
                    authError = null
                    onUnlocked()
                }
                is com.example.util.security.PinValidationResult.DuressTriggered -> {
                    triggerHapticFeedback(false)
                    authSuccess = true
                    authError = null
                    // Under duress coercion: silently shred all data and purge clipboard immediately
                    com.example.util.security.ClipboardSanitizer.sanitizeNow(context)
                    onDuressTriggered?.invoke()
                    onUnlocked()
                }
                is com.example.util.security.PinValidationResult.InvalidPin -> {
                    triggerHapticFeedback(true)
                    authError = if (result.attemptsUntilLockout <= 2) {
                        "PIN incorreto! Restam ${result.attemptsUntilLockout} tentativa(s) antes do bloqueio."
                    } else {
                        "PIN incorreto. Tente novamente."
                    }
                    pinInput = ""
                }
                is com.example.util.security.PinValidationResult.LockedOut -> {
                    triggerHapticFeedback(true)
                    lockoutRemainingSeconds = result.remainingSeconds
                    authError = "Muitas tentativas incorretas! Bloqueado por ${result.remainingSeconds}s."
                    pinInput = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveSurface)
            .statusBarsPadding()
            .testTag("biometric_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // App Branding
            PmsgLogoBadge(size = 56.dp, iconSize = 34.dp, shapeRadius = 16.dp)
            Spacer(modifier = Modifier.height(14.dp))
            PmsgWordmark(fontSize = 24.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "APLICATIVO BLOQUEADO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersivePrimary,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isPinMode) {
                // Biometric Modalities Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ImmersiveCardVariant)
                        .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Impressão Digital",
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Digital",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = ImmersiveMuted
                    )
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Reconhecimento Facial",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Facial",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Biometric Fingerprint Visual
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    // Outer pulsing rings
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(ImmersivePrimary.copy(alpha = 0.12f))
                    )
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .background(ImmersivePrimary.copy(alpha = 0.2f))
                            .border(1.5.dp, ImmersivePrimary.copy(alpha = 0.5f), CircleShape)
                    )

                    // Fingerprint Touch Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ImmersivePrimary, ElectricCyan)
                                )
                            )
                            .clickable { performBiometricAuth() }
                            .testTag("biometric_fingerprint_touch_target"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Desbloquear com Biometria",
                            tint = ImmersiveSurface,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Toque para desbloqueio rápido",
                    fontSize = 15.sp,
                    color = ImmersiveOnSurface,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Autenticação rápida por biometria digital ou facial",
                    fontSize = 12.sp,
                    color = ImmersiveMutedLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError ?: "",
                        color = EmberOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { performBiometricAuth() },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(46.dp)
                        .testTag("biometric_auth_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = ImmersiveOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autenticar com Digital / Face",
                        color = ImmersiveOnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { isPinMode = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        tint = ImmersiveMutedLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Usar PIN de Segurança",
                        color = ImmersiveMutedLight,
                        fontSize = 12.sp
                    )
                }
            } else {
                // PIN Code Entry Pad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ImmersiveCard)
                        .border(1.dp, ImmersiveOutline, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Digite o PIN de Segurança",
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveOnSurface,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Auto-bloqueio de segurança ativo",
                        fontSize = 11.sp,
                        color = ImmersiveMutedLight,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = authError ?: "",
                            color = EmberOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Digit dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            val isFilled = pinInput.length > index
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) ImmersivePrimary else ImmersiveCardVariant
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isFilled) ImmersivePrimary else ImmersiveOutline,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Number keypad
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Bio", "0", "Del")
                    )

                    keyRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (key == "Bio" || key == "Del") ImmersiveHeader else ImmersiveCardVariant
                                        )
                                        .border(0.8.dp, ImmersiveOutline, CircleShape)
                                        .clickable {
                                            when (key) {
                                                "Del" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                                "Bio" -> isPinMode = false
                                                else -> if (pinInput.length < 4) pinInput += key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "Del" -> Icon(Icons.Default.Backspace, contentDescription = "Apagar", tint = ImmersiveMutedLight, modifier = Modifier.size(18.dp))
                                        "Bio" -> Icon(Icons.Default.Fingerprint, contentDescription = "Biometria", tint = ImmersivePrimary, modifier = Modifier.size(22.dp))
                                        else -> Text(text = key, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ImmersiveOnSurface)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { isPinMode = false }) {
                        Text("Voltar para Biometria", color = ImmersivePrimary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Security assurance footer
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ImmersiveOnlineGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Criptografia Local de Nível de Dispositivo",
                    fontSize = 11.sp,
                    color = ImmersiveMuted
                )
            }
        }
    }
}
