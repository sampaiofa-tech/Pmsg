package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Block
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contactRepository: ContactRepository,
    onContactSelected: (ContactItem) -> Unit,
    onOpenIdentity: () -> Unit,
    onOpenDataPrivacy: () -> Unit = {},
    onOpenBlockedContacts: () -> Unit = {},
    onAddContactModelA: () -> Unit,
    onCompareSafetyNumber: (ContactItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val contacts by contactRepository.getContacts().collectAsState(initial = emptyList())
    var contactToDelete by remember { mutableStateOf<ContactItem?>(null) }
    var contactToBlock by remember { mutableStateOf<ContactItem?>(null) }
    var showPanicDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Contatos Criptografados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero Rastro • X25519 E2EE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF80CBC4)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenBlockedContacts) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Contatos Bloqueados",
                            tint = Color(0xFFFF8080)
                        )
                    }
                    IconButton(onClick = onOpenDataPrivacy) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sobre seus dados (LGPD)",
                            tint = Color(0xFF00FFC2)
                        )
                    }
                    IconButton(onClick = onOpenIdentity) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Minha Identidade",
                            tint = Color(0xFF00FFC2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactModelA,
                containerColor = Color(0xFF00FFC2),
                contentColor = Color(0xFF0A1128)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Contato (Modelo A)"
                )
            }
        },
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (contacts.isEmpty()) {
                EmptyContactsView(
                    onAddContactModelA = onAddContactModelA,
                    onOpenIdentity = onOpenIdentity
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickActionBar(
                            contactCount = contacts.size,
                            onAddContact = onAddContactModelA,
                            onPanicWipe = { showPanicDialog = true }
                        )
                    }

                    items(contacts, key = { it.fingerprint }) { contact ->
                        ContactRowItem(
                            contact = contact,
                            onClick = { onContactSelected(contact) },
                            onVerifyClick = { onCompareSafetyNumber(contact) },
                            onBlockClick = { contactToBlock = contact },
                            onDeleteClick = { contactToDelete = contact }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Block Contact Dialog
    contactToBlock?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToBlock = null },
            title = { Text("Bloquear Contato?") },
            text = {
                Text("O contato \"${contact.displayName}\" será bloqueado localmente no dispositivo.\n\nNovas mensagens recebidas deste contato serão descartadas imediatamente sem serem exibidas.\n\nO servidor não tem acesso à sua lista de bloqueados.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            contactRepository.blockContact(contact.fingerprint)
                            contactToBlock = null
                        }
                    }
                ) {
                    Text("Bloquear", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToBlock = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Single Contact Dialog
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Excluir Contato?") },
            text = {
                Text("O contato \"${contact.displayName}\" será incinerado localmente. Todas as chaves e mensagens associadas serão destruídas.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            contactRepository.deleteContact(contact.fingerprint)
                            contactToDelete = null
                        }
                    }
                ) {
                    Text("Excluir", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Panic Wipe Dialog
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            title = { Text("💥 PÂNICO: Incinerar Todos os Contatos?") },
            text = {
                Text("Esta ação sobrescreve e destrói imediatamente todos os contatos salvos sem chance de recuperação forense.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            contactRepository.panicWipe()
                            showPanicDialog = false
                        }
                    }
                ) {
                    Text("INCINERAR TUDO", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun QuickActionBar(
    contactCount: Int,
    onAddContact: () -> Unit,
    onPanicWipe: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$contactCount Contato(s) Ativo(s)",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFB0BEC5),
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(
                onClick = onAddContact,
                label = { Text("+ Modelo A", fontSize = 12.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF00FFC2)
                )
            )

            SuggestionChip(
                onClick = onPanicWipe,
                label = { Text("💥 Pânico", fontSize = 12.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF331111),
                    labelColor = Color(0xFFFF8080)
                )
            )
        }
    }
}

@Composable
fun ContactRowItem(
    contact: ContactItem,
    onClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF131B2A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (contact.verified) Color(0xFF004D40) else Color(0xFF263238)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.displayName.take(1).uppercase(),
                    color = if (contact.verified) Color(0xFF00FFC2) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Verified / Unverified Badge
                    if (contact.verified) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00E676).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onVerifyClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Verificado",
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFB300).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onVerifyClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Não verificado",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Fingerprint preview
                Text(
                    text = "ID: ${contact.fingerprint.take(12)}...${contact.fingerprint.takeLast(6)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF78909C),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Block Action
            IconButton(onClick = onBlockClick) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Bloquear",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete Action
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = Color(0xFF546E7A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyContactsView(
    onAddContactModelA: () -> Unit,
    onOpenIdentity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF131B2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF00FFC2),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Nenhum Contato Criptografado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "O Raix não possui lista telefônica pública ou diretório de nick. Para conversar, faça a troca de chaves presencial (Modelo A).",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF90A4AE),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(
            onClick = onAddContactModelA,
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF00FFC2)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF0A1128)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Adicionar Contato (Modelo A)",
                    color = Color(0xFF0A1128),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onOpenIdentity) {
            Text("Ver Minha Identidade e Chave Pública", color = Color(0xFF00FFC2))
        }
    }
}
