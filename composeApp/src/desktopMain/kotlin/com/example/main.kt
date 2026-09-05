package com.example

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Raix [desktop-dev]",
        icon = painterResource("icon.png"),
        alwaysOnTop = true
    ) {
        App()
    }
}
