package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

actual val isQrScannerSupported: Boolean = true

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerView(
    onQrCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier
) {
    var hasPermission by remember { mutableStateOf(false) }
    var permissionChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        if (status == AVAuthorizationStatusAuthorized) {
            hasPermission = true
            permissionChecked = true
        } else if (status == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                hasPermission = granted
                permissionChecked = true
            }
        } else {
            hasPermission = false
            permissionChecked = true
        }
    }

    if (!permissionChecked) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00FFC2))
        }
    } else if (!hasPermission) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permissão de Câmera Necessária",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Acesse Ajustes > Pmsg para habilitar a câmera, ou use a aba 'Colar Presencial' para inserir o código manualmente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                ) {
                    Text(
                        text = "Voltar para Colar Código",
                        color = Color(0xFF0A1128),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            UIKitView(
                factory = {
                    val container = UIView()
                    val session = AVCaptureSession()
                    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                    if (device != null) {
                        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                        if (input != null && session.canAddInput(input)) {
                            session.addInput(input)
                        }
                        val output = AVCaptureMetadataOutput()
                        if (session.canAddOutput(output)) {
                            session.addOutput(output)
                            output.setMetadataObjectsDelegate(
                                object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
                                    override fun captureOutput(
                                        output: AVCaptureOutput,
                                        didOutputMetadataObjects: List<*>,
                                        fromConnection: AVCaptureConnection
                                    ) {
                                        for (item in didOutputMetadataObjects) {
                                            if (item is AVMetadataMachineReadableCodeObject &&
                                                item.type == AVMetadataObjectTypeQRCode
                                            ) {
                                                val stringValue = item.stringValue
                                                if (!stringValue.isNullOrBlank()) {
                                                    session.stopRunning()
                                                    onQrCodeScanned(stringValue)
                                                    break
                                                }
                                            }
                                        }
                                    }
                                },
                                queue = dispatch_get_main_queue()
                            )
                            output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
                        }
                        val previewLayer = AVCaptureVideoPreviewLayer.layerWithSession(session)
                        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                        previewLayer.frame = container.bounds
                        container.layer.addSublayer(previewLayer)
                        session.startRunning()
                    }
                    container
                },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131B2A))
            ) {
                Text("Fechar Câmera", color = Color(0xFF00FFC2), fontWeight = FontWeight.Bold)
            }
        }
    }
}
