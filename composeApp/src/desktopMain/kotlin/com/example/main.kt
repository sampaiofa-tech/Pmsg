package com.example

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val isDev = System.getProperty("raix.dev") == "true" || System.getenv("RAIX_DEV") == "true"
    val windowTitle = if (isDev) "Raix [desktop-dev]" else "Raix"
    Window(
        onCloseRequest = ::exitApplication,
        title = windowTitle,
        icon = painterResource("icon.png"),
        alwaysOnTop = true
    ) {
        App()
    }
}
