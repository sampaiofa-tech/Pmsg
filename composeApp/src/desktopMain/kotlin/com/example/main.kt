package com.example

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pmsg [desktop-dev]",
        alwaysOnTop = true
    ) {
        App()
    }
}
