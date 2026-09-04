package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Indicates whether live camera scanning is supported on this target platform.
 * True for Android and iOS; false for Desktop and Web.
 */
expect val isQrScannerSupported: Boolean

/**
 * Multiplatform QR Code camera scanner.
 * On mobile (Android/iOS), opens native camera preview and scans offline via ML Kit / AVFoundation.
 * On desktop/web, displays an informative fallback message guiding user to paste string.
 */
@Composable
expect fun QrScannerView(
    onQrCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
