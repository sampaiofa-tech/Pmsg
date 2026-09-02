package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.BiometricAuthHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveHeader
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface

@Composable
fun SecurityVaultDialog(
    screenProtectionEnabled: Boolean,
    biometricLockEnabled: Boolean = false,
    autoLockEnabled: Boolean = true,
    autoLockTimeoutMinutes: Int = 5,
    securityPin: String = "1234",
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onTestNotification: () -> Unit,
    onToggleScreenProtection: () -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleAutoLock: (Boolean) -> Unit = {},
    onSetAutoLockTimeout: (Int) -> Unit = {},
    onSetSecurityPin: (String) -> Unit = {},
    onLockNow: () -> Unit = {},
    onPanicWipe: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEditingPin by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val autoLockTimeoutOptions = listOf(
        1 to "1 min",
        5 to "5 min (Padrão)",
        10 to "10 min",
        15 to "15 min",
        30 to "30 min"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("security_vault_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveHeader),
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PmsgLogoBadge(
                            size = 38.dp,
                            iconSize = 22.dp,
                            shapeRadius = 10.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PmsgWordmark(fontSize = 17.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Segurança",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ImmersiveOnSurface
                                )
                            }
                            Text(
                                text = "Protocolo de Proteção e Auto-Bloqueio",
                                fontSize = 11.sp,
                                color = ImmersiveMutedLight
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = ImmersiveMutedLight)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = ImmersiveOutline)
                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Lock Section (Requested: Auto-lock every 5 min by default, customizable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ImmersiveCardVariant)
                        .border(1.dp, ImmersivePrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ImmersivePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = ImmersivePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Auto-Bloqueio Automático",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveOnSurface
                                )
                                Text(
                                    text = if (autoLockEnabled) "Bloqueia e pede senha a cada $autoLockTimeoutMinutes min de inatividade." else "Desativado.",
                                    fontSize = 11.sp,
                                    color = ImmersiveMutedLight,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Switch(
                            checked = autoLockEnabled,
                            onCheckedChange = { onToggleAutoLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ImmersiveSurface,
                                checkedTrackColor = ImmersivePrimary,
                                uncheckedThumbColor = ImmersiveMuted,
                                uncheckedTrackColor = ImmersiveCard
                            ),
                            modifier = Modifier.testTag("auto_lock_switch")
                        )
                    }

                    if (autoLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "TEMPO PARA BLOQUEAR AUTOMATICAMENTE:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersivePrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Preset chips for auto-lock timeout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            autoLockTimeoutOptions.forEach { (mins, label) ->
                                val isSelected = autoLockTimeoutMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ImmersivePrimary else ImmersiveCard)
                                        .border(
                                            0.8.dp,
                                            if (isSelected) ImmersivePrimary else ImmersiveOutline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onSetAutoLockTimeout(mins)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 5) "5m (Padrão)" else "${mins}m",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security PIN Configuration Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ImmersiveCardVariant)
                        .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PIN de Segurança",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveOnSurface
                                )
                                Text(
                                    text = "PIN Atual: •••• (Toque para alterar)",
                                    fontSize = 11.sp,
                                    color = ImmersiveMutedLight
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isEditingPin = !isEditingPin
                                newPinInput = ""
                                pinError = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveCard),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).border(0.8.dp, ImmersivePrimary, RoundedCornerShape(8.dp))
                        ) {
                            Text(if (isEditingPin) "Cancelar" else "Alterar", color = ImmersivePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isEditingPin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                                label = { Text("Novo PIN (4 dígitos)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ImmersivePrimary,
                                    unfocusedBorderColor = ImmersiveOutline,
                                    focusedTextColor = ImmersiveOnSurface,
                                    unfocusedTextColor = ImmersiveOnSurface
                                )
                            )

                            Button(
                                onClick = {
                                    if (newPinInput.length == 4) {
                                        onSetSecurityPin(newPinInput)
                                        isEditingPin = false
                                        pinError = null
                                    } else {
                                        pinError = "Deve ter 4 dígitos"
                                    }
                                },
                                enabled = newPinInput.length == 4,
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("Salvar", color = ImmersiveOnPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (pinError != null) {
                            Text(text = pinError ?: "", color = com.example.ui.theme.EmberOrange, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Biometric App Lock Card (Fingerprint / Face Recognition)
                val context = LocalContext.current
                val biometricStatus = remember(context) { BiometricAuthHelper.getBiometricStatusDescription(context) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveCardVariant, RoundedCornerShape(14.dp))
                        .border(
                            if (biometricLockEnabled) 1.dp else 0.8.dp,
                            if (biometricLockEnabled) ImmersivePrimary.copy(alpha = 0.5f) else ImmersiveOutline,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (biometricLockEnabled) ImmersivePrimary.copy(alpha = 0.2f) else ImmersiveCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = if (biometricLockEnabled) ImmersivePrimary else ImmersiveMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Autenticação Biométrica",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveOnSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Facial",
                                        tint = if (biometricLockEnabled) com.example.ui.theme.ElectricCyan else ImmersiveMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (biometricLockEnabled) "Desbloqueio rápido por impressão digital ou reconhecimento facial." else "Desativado: desbloqueio requer PIN.",
                                    fontSize = 11.sp,
                                    color = ImmersiveMutedLight,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Switch(
                            checked = biometricLockEnabled,
                            onCheckedChange = { onToggleBiometricLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ImmersiveSurface,
                                checkedTrackColor = ImmersivePrimary,
                                uncheckedThumbColor = ImmersiveMuted,
                                uncheckedTrackColor = ImmersiveCard
                            ),
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }

                    if (biometricLockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ImmersiveSurface.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.ImmersiveOnlineGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status: $biometricStatus",
                                fontSize = 10.sp,
                                color = ImmersiveMutedLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Anti-screenshot Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveCardVariant, RoundedCornerShape(14.dp))
                        .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoPhotography,
                            contentDescription = null,
                            tint = if (screenProtectionEnabled) ImmersivePrimary else ImmersiveMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Anti-Captura (FLAG_SECURE)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveOnSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (screenProtectionEnabled) "Ativo: bloqueia capturas em aparelhos físicos." else "Desativado para exibição fluida no preview.",
                                fontSize = 11.sp,
                                color = ImmersiveMutedLight,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Switch(
                        checked = screenProtectionEnabled,
                        onCheckedChange = { onToggleScreenProtection() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ImmersiveSurface,
                            checkedTrackColor = ImmersivePrimary,
                            uncheckedThumbColor = ImmersiveMuted,
                            uncheckedTrackColor = ImmersiveCard
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notification Alerts Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveCardVariant, RoundedCornerShape(14.dp))
                        .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (notificationsEnabled) ImmersivePrimary else ImmersiveMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Notificações",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveOnSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (notificationsEnabled) "Ativo: notifica novas conversas. Destruição é 100% silenciosa." else "Toque para autorizar avisos.",
                                fontSize = 11.sp,
                                color = ImmersiveMutedLight,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (!notificationsEnabled) {
                        Button(
                            onClick = onRequestNotificationPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("enable_notifications_button")
                        ) {
                            Text("Permitir", color = ImmersiveOnPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onTestNotification,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, ImmersivePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("test_notification_button")
                        ) {
                            Text("Testar", color = ImmersivePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Immediate Lock Button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLockNow()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("lock_app_now_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bloquear Aplicativo Agora",
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Emergency Panic Incinerate Button
                Button(
                    onClick = {
                        onDismiss()
                        onPanicWipe()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("panic_wipe_button_dialog"),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveExpiring),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF5A110D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Incinerar Tudo Agora (Wipe)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5A110D),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ImmersiveCardVariant, RoundedCornerShape(14.dp))
            .border(1.dp, ImmersiveOutline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveOnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = ImmersiveMutedLight,
                lineHeight = 16.sp
            )
        }
    }
}
