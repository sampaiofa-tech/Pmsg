package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BurnerChannel
import com.example.data.model.PmsgContact
import com.example.ui.theme.ImmersiveAvatarDeep
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveHeader
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOnlineGreen
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChannelListScreen(
    channels: List<BurnerChannel>,
    contacts: List<PmsgContact> = emptyList(),
    currentTime: Long,
    screenProtectionEnabled: Boolean,
    biometricLockEnabled: Boolean = false,
    autoLockEnabled: Boolean = true,
    autoLockTimeoutMinutes: Int = 5,
    securityPin: String = "1234",
    notificationsEnabled: Boolean = true,
    hasContactsPermission: Boolean = true,
    userFeedback: String?,
    onSelectChannel: (BurnerChannel) -> Unit,
    onCreateChannel: (String, String, Float) -> Unit,
    onStartChatWithContact: (PmsgContact) -> Unit = {},
    onDeleteChannel: (String) -> Unit,
    onPanicWipe: () -> Unit,
    onToggleScreenProtection: () -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleAutoLock: (Boolean) -> Unit = {},
    onSetAutoLockTimeout: (Int) -> Unit = {},
    onSetSecurityPin: (String) -> Unit = {},
    onLockNow: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestContactsPermission: () -> Unit = {},
    onRefreshContacts: () -> Unit = {},
    onSimulateIncomingNewConversation: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onClearFeedback: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userFeedback) {
        userFeedback?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            onClearFeedback()
        }
    }

    val filteredChannels = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) || it.customCode.contains(searchQuery, ignoreCase = true) }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ImmersiveSurface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 1) {
                        showCreateDialog = true
                    } else {
                        showCreateDialog = true
                    }
                },
                containerColor = ImmersivePrimary,
                contentColor = ImmersiveOnPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("create_channel_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.Add else Icons.Default.PersonAdd,
                        contentDescription = "Novo Chat",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTab == 0) "Novo Chat" else "Novo Contato",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ImmersivePrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            // Sleek Immersive Top Bar with statusBarsPadding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.ObsidianSurface)
                    .statusBarsPadding()
                    .border(width = 1.dp, color = com.example.ui.theme.ObsidianBorder)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.PmsgLogoBadge(
                            size = 38.dp,
                            iconSize = 24.dp,
                            shapeRadius = 11.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            com.example.ui.components.PmsgWordmark(
                                fontSize = 18.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(com.example.ui.theme.SecurityEmerald)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Zero Rastro • Hardware TEE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = com.example.ui.theme.TitaniumMuted
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Settings & Security Vault Page Button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.example.ui.theme.ObsidianCardElevated)
                                .testTag("security_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Configurações e Cofre",
                                tint = com.example.ui.theme.TitaniumPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Refined PANIC WIPE Button
                        Button(
                            onClick = { showPanicDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.IncinerateCrimsonBg,
                                contentColor = com.example.ui.theme.IncinerateCrimson
                            ),
                            border = BorderStroke(1.dp, com.example.ui.theme.IncinerateCrimson.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("panic_wipe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Pânico",
                                tint = com.example.ui.theme.IncinerateCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "INCINERAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.IncinerateCrimson,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Modern Sliding Pill Tab Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.example.ui.theme.ObsidianCard)
                    .border(1.dp, com.example.ui.theme.ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Conversas Tab Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (selectedTab == 0) com.example.ui.theme.ObsidianCardElevated
                                else Color.Transparent
                            )
                            .border(
                                width = if (selectedTab == 0) 1.dp else 0.dp,
                                color = if (selectedTab == 0) com.example.ui.theme.ObsidianBorder else Color.Transparent,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp)
                            .testTag("tab_conversations"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (selectedTab == 0) com.example.ui.theme.TitaniumPrimary else com.example.ui.theme.TitaniumMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Conversas (${channels.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.5.sp,
                                color = if (selectedTab == 0) com.example.ui.theme.TitaniumPrimary else com.example.ui.theme.TitaniumMuted
                            )
                        }
                    }

                    // Contatos Tab Pill
                    val pmsgCount = contacts.count { it.hasPmsgInstalled }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (selectedTab == 1) com.example.ui.theme.ObsidianCardElevated
                                else Color.Transparent
                            )
                            .border(
                                width = if (selectedTab == 1) 1.dp else 0.dp,
                                color = if (selectedTab == 1) com.example.ui.theme.ObsidianBorder else Color.Transparent,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp)
                            .testTag("tab_contacts"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (selectedTab == 1) com.example.ui.theme.TitaniumPrimary else com.example.ui.theme.TitaniumMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contatos ($pmsgCount)",
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.5.sp,
                                color = if (selectedTab == 1) com.example.ui.theme.TitaniumPrimary else com.example.ui.theme.TitaniumMuted
                            )
                        }
                    }
                }
            }

            // Harmonious Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "Buscar conversas ativas..." else "Buscar contatos com Pmsg...",
                            fontSize = 12.5.sp,
                            color = com.example.ui.theme.TitaniumMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = com.example.ui.theme.TitaniumMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (selectedTab == 1) {
                            IconButton(onClick = onRefreshContacts) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Atualizar Contatos",
                                    tint = com.example.ui.theme.TitaniumPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Limpar busca",
                                    tint = com.example.ui.theme.TitaniumMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ImmersiveOnSurface,
                        unfocusedTextColor = ImmersiveOnSurface,
                        focusedBorderColor = com.example.ui.theme.TitaniumSecondary,
                        unfocusedBorderColor = com.example.ui.theme.ObsidianBorder,
                        focusedContainerColor = com.example.ui.theme.ObsidianCard,
                        unfocusedContainerColor = com.example.ui.theme.ObsidianCard
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            if (selectedTab == 0) {
                // TAB 0: CONVERSAS ATIVAS
                if (filteredChannels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ImmersiveMuted,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Nenhuma conversa ativa no momento",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = ImmersiveMutedLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Acesse a aba 'Contatos Pmsg' ou crie um novo chat.",
                                fontSize = 12.sp,
                                color = ImmersiveMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver Contatos com Pmsg", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredChannels, key = { it.id }) { channel ->
                            ChannelListItem(
                                channel = channel,
                                currentTime = currentTime,
                                onClick = { onSelectChannel(channel) },
                                onDelete = { onDeleteChannel(channel.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            } else {
                // TAB 1: CONTATOS COM PMSG INSTALADO
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Contact header banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ImmersiveCardVariant)
                            .border(1.dp, ImmersiveOutline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Identificação de contatos com Pmsg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ImmersiveOnSurface
                            )
                        }

                        // Test incoming new conversation notification
                        OutlinedButton(
                            onClick = onSimulateIncomingNewConversation,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ImmersivePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp).testTag("simulate_new_conv_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simular Chegada", color = ImmersivePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!hasContactsPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ImmersiveCard)
                                .border(1.dp, ImmersivePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Permissão de Contatos Necessária",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ImmersiveOnSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Permita o acesso aos contatos para verificar quais amigos já utilizam o Pmsg e iniciar conversas criptografadas.",
                                    fontSize = 12.sp,
                                    color = ImmersiveMutedLight
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRequestContactsPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Autorizar Contatos", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            ContactListItem(
                                contact = contact,
                                onStartChat = { onStartChatWithContact(contact) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Create New Channel Dialog
    if (showCreateDialog) {
        var contactName by remember { mutableStateOf("") }
        // Default to 24 hours as required ("caso não escolha o tempo, sera usado 24 horas como padrão")
        var selectedTtlHours by remember { mutableStateOf(24f) }

        val ttlOptions = listOf(
            24f to "24 HRS",
            12f to "12 HRS",
            6f to "6 HRS",
            1f to "1 HR",
            0.08333f to "5 MN",
            0.00833f to "30 SEG"
        )

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = ImmersiveHeader,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Novo Chat",
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveOnSurface,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Informe o nome da pessoa e selecione o tempo até a exclusão automática das mensagens.",
                        fontSize = 12.sp,
                        color = ImmersiveMutedLight
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Nome do Contato") },
                        placeholder = { Text("Ex: Mariana, Carlos, Beatriz...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ImmersiveOnSurface,
                            unfocusedTextColor = ImmersiveOnSurface,
                            focusedBorderColor = ImmersivePrimary,
                            unfocusedBorderColor = ImmersiveOutline,
                            focusedLabelColor = ImmersivePrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TEMPO ATÉ A EXCLUSÃO:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // TTL Chips selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ttlOptions.take(3).forEach { (hours, label) ->
                                val isSelected = selectedTtlHours == hours
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ImmersivePrimary else ImmersiveCardVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) ImmersivePrimary else ImmersiveOutline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedTtlHours = hours }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ttlOptions.drop(3).forEach { (hours, label) ->
                                val isSelected = selectedTtlHours == hours
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ImmersivePrimary else ImmersiveCardVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) ImmersivePrimary else ImmersiveOutline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedTtlHours = hours }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveOnSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank()) {
                            onCreateChannel(contactName.trim(), "", selectedTtlHours)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_create_channel_button")
                ) {
                    Text("Iniciar Chat", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = ImmersiveMutedLight)
                }
            }
        )
    }

    // Panic Wipe Confirmation Dialog
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            containerColor = ImmersiveHeader,
            icon = {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ImmersiveExpiring, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = "🚨 INCINERAÇÃO TOTAL EM PÂNICO",
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveExpiring,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ATENÇÃO: Todas as conversas ativas, contatos vinculados e mensagens armazenadas no Room serão DESTRUÍDAS e sobrescritas de forma permanente. Nenhum dado poderá ser recuperado.",
                    color = ImmersiveOnSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicDialog = false
                        onPanicWipe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveExpiring),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_panic_wipe_button")
                ) {
                    Text("VAPORIZAR TUDO AGORA", fontWeight = FontWeight.Bold, color = Color(0xFF601410))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicDialog = false }) {
                    Text("Cancelar", color = ImmersiveMutedLight)
                }
            }
        )
    }
}

@Composable
fun ContactListItem(
    contact: PmsgContact,
    onStartChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = contact.hasPmsgInstalled) { onStartChat() }
            .testTag("contact_item_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.hasPmsgInstalled) ImmersiveCard else ImmersiveCardVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (contact.hasPmsgInstalled) ImmersivePrimary.copy(alpha = 0.25f) else ImmersiveOutline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with contact initial
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(contact.avatarColorHex).copy(alpha = 0.2f))
                        .border(1.5.dp, Color(contact.avatarColorHex).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color(contact.avatarColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = ImmersiveOnSurface
                        )
                        if (contact.hasPmsgInstalled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(com.example.ui.theme.SecurityEmerald.copy(alpha = 0.15f))
                                    .border(0.5.dp, com.example.ui.theme.SecurityEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Pmsg Ativo",
                                    color = com.example.ui.theme.SecurityEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = contact.phoneNumber,
                        fontSize = 11.5.sp,
                        color = com.example.ui.theme.TitaniumMuted,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = contact.statusDescription,
                        fontSize = 10.5.sp,
                        color = if (contact.hasPmsgInstalled) com.example.ui.theme.SecurityEmerald else com.example.ui.theme.TitaniumMuted
                    )
                }
            }

            if (contact.hasPmsgInstalled) {
                Button(
                    onClick = onStartChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.TitaniumPrimary,
                        contentColor = com.example.ui.theme.ObsidianBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("start_chat_with_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = com.example.ui.theme.ObsidianBlack,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Conversar",
                        color = com.example.ui.theme.ObsidianBlack,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.example.ui.theme.ObsidianCardElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Não possui",
                        color = com.example.ui.theme.TitaniumMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: BurnerChannel,
    currentTime: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingMs = channel.remainingTime(currentTime)
    val isAlmostExpired = remainingMs < 3600000L // less than 1 hour

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("channel_item_${channel.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ObsidianCard),
        border = BorderStroke(
            width = 1.dp,
            color = if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson.copy(alpha = 0.55f) else com.example.ui.theme.ObsidianBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with hardware security badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson.copy(alpha = 0.25f)
                                        else com.example.ui.theme.SecurityEmerald.copy(alpha = 0.18f),
                                        com.example.ui.theme.ObsidianCardElevated
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson else com.example.ui.theme.SecurityEmerald,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channel.name.take(1).uppercase(),
                            color = if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson else com.example.ui.theme.TitaniumPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    // Hardware lock dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.ObsidianBlack)
                            .padding(1.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson else com.example.ui.theme.SecurityEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = ImmersiveOnSurface
                        )
                        if (channel.customCode.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(com.example.ui.theme.ObsidianCardElevated)
                                    .border(0.5.dp, com.example.ui.theme.ObsidianBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "#${channel.customCode}",
                                    color = com.example.ui.theme.TitaniumSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = channel.lastMessagePreview,
                        fontSize = 12.sp,
                        color = com.example.ui.theme.TitaniumSecondary,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live Expiration Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isAlmostExpired) com.example.ui.theme.IncinerateCrimsonBg
                                else com.example.ui.theme.ObsidianCardElevated
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAlmostExpired) Icons.Default.LocalFireDepartment else Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson else com.example.ui.theme.TitaniumSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Expira em ${channel.formattedRemainingTime(currentTime)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAlmostExpired) com.example.ui.theme.IncinerateCrimson else com.example.ui.theme.TitaniumSecondary
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.ObsidianCardElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Apagar Canal",
                    tint = com.example.ui.theme.TitaniumMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
