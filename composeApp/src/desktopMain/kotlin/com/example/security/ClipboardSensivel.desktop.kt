package com.example.security

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual class ClipboardSensivel {
    private val scope = CoroutineScope(Dispatchers.Default)

    actual fun copySensitive(text: String, label: String) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)

            // Auto-wipe after 30 seconds
            scope.launch {
                delay(30_000L)
                clear()
            }
        } catch (_: Throwable) {}
    }

    actual fun clear() {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(""), null)
        } catch (_: Throwable) {}
    }
}
