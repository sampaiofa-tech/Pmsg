package com.example

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pmsg - Ephemeral Secure Messaging"
    ) {
        App()
    }
}
