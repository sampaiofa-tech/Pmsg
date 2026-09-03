package com.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepositoryProvider
import com.example.ui.screens.AddContactModelAScreen
import com.example.ui.screens.ContactChatScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.IdentityScreen
import com.example.ui.screens.SafetyNumberScreen

sealed interface AppDestination {
    data object Contacts : AppDestination
    data class Chat(val contact: ContactItem) : AppDestination
    data object Identity : AppDestination
    data object AddModelA : AppDestination
    data class SafetyNumber(val contact: ContactItem) : AppDestination
}

private val PmsgDarkColors = darkColorScheme(
    primary = Color(0xFF00FFC2),
    onPrimary = Color(0xFF0A1128),
    surface = Color(0xFF0A0E17),
    onSurface = Color.White,
    background = Color(0xFF0A0E17),
    onBackground = Color.White
)

@Composable
fun App() {
    val contactRepository = remember { ContactRepositoryProvider.get() }
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Contacts) }

    MaterialTheme(colorScheme = PmsgDarkColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0E17)) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                },
                label = "app_navigation_transition"
            ) { destination ->
                when (destination) {
                    is AppDestination.Contacts -> {
                        ContactsScreen(
                            contactRepository = contactRepository,
                            onContactSelected = { contact ->
                                currentDestination = AppDestination.Chat(contact)
                            },
                            onOpenIdentity = {
                                currentDestination = AppDestination.Identity
                            },
                            onAddContactModelA = {
                                currentDestination = AppDestination.AddModelA
                            },
                            onCompareSafetyNumber = { contact ->
                                currentDestination = AppDestination.SafetyNumber(contact)
                            }
                        )
                    }

                    is AppDestination.Chat -> {
                        ContactChatScreen(
                            contact = destination.contact,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onCompareSafetyNumber = {
                                currentDestination = AppDestination.SafetyNumber(destination.contact)
                            }
                        )
                    }

                    is AppDestination.Identity -> {
                        IdentityScreen(
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }

                    is AppDestination.AddModelA -> {
                        AddContactModelAScreen(
                            contactRepository = contactRepository,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onContactCreated = { newContact ->
                                currentDestination = AppDestination.SafetyNumber(newContact)
                            }
                        )
                    }

                    is AppDestination.SafetyNumber -> {
                        SafetyNumberScreen(
                            contact = destination.contact,
                            contactRepository = contactRepository,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onVerifiedComplete = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }
                }
            }
        }
    }
}
