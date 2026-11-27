package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.MemberQrUtils
import com.example.ui.i18n.tr
import com.example.ui.theme.HarvestGoldLight
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen Admin QR Code Scanner dialog powered by CameraX + on-device ML Kit Barcode Scanning.
 * Scans Member Card QR codes (sibtoburon://member?...) and returns the raw payload immediately
 * so the app can display full church member details.
 */
@Composable
fun MemberQrScannerDialog(
    onDismiss: () -> Unit,
    onValidMemberQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var invalidQrNotice by remember { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A1412))
                .testTag("member_qr_scanner_dialog")
        ) {
            if (hasCameraPermission) {
                CameraQrPreviewStage(
                    onCameraBound = { camera ->
                        boundCamera = camera
                        hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                    },
                    onCameraError = { message ->
                        cameraError = message
                    },
                    onQrDetected = { rawValue ->
                        val parsed = MemberQrUtils.parseMemberQrPayload(rawValue)
                        if (parsed != null) {
                            onValidMemberQrScanned(rawValue)
                            true
                        } else {
                            invalidQrNotice = rawValue
                            false
                        }
                    }
                )

                // Dark viewfinder overlay with crisp cut-out window and gold corner brackets
                QrViewfinderOverlay(modifier = Modifier.fillMaxSize())
            } else {
                // Permission Denied / Request View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = HarvestGoldLight,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = tr("Kebenaran Kamera Diperlukan", "Camera Permission Required"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tr(
                            "Benarkan akses kamera supaya Admin boleh mengimbas Kod QR pada Kad Ahli SIB Toburon.",
                            "Allow camera access so Admins can scan the QR Code on SIB Toburon Member Cards."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("qr_scanner_request_permission_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Benarkan Kamera", "Grant Camera Permission"))
                    }
                }
            }

            // Top Controls Bar (Close + Title + Flashlight Toggle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("qr_scanner_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("Tutup pengimbas", "Close scanner"),
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = HarvestGoldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tr("Imbas Kod QR Ahli", "Scan Member QR Code"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                if (hasCameraPermission && hasFlashUnit) {
                    Surface(
                        shape = CircleShape,
                        color = if (torchEnabled) HarvestGoldLight else Color.Black.copy(alpha = 0.55f)
                    ) {
                        IconButton(
                            onClick = {
                                val next = !torchEnabled
                                torchEnabled = next
                                boundCamera?.cameraControl?.enableTorch(next)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("qr_scanner_torch_button")
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = tr("Lampu suluh", "Flashlight"),
                                tint = if (torchEnabled) Color.Black else Color.White
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Bottom Guidance & Feedback Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (cameraError != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cameraError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                } else if (invalidQrNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xD98B1E1E),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_scanner_invalid_notice")
                    ) {
                        Text(
                            text = tr(
                                "Kod QR ini bukan Kad Ahli SIB Toburon. Sila imbas Kod QR pada Kad Ahli jemaat.",
                                "This QR code is not an SIB Toburon Member Card. Please scan a valid Member Card QR."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                if (hasCameraPermission) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.62f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Halakan kamera pada Kod QR di bahagian bawah Kad Ahli untuk memaparkan maklumat penuh ahli secara automatik.",
                                "Point the camera at the QR Code on a Member Card to display full member details automatically."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreviewStage(
    onCameraBound: (Camera) -> Unit,
    onCameraError: (String) -> Unit,
    onQrDetected: (String) -> Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    val handledValidScan = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner, previewView) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var activeProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    activeProvider = cameraProvider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (handledValidScan.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener(mainExecutor) { barcodes ->
                                if (handledValidScan.get()) return@addOnSuccessListener
                                val raw = barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }
                                if (raw != null) {
                                    val accepted = onQrDetected(raw)
                                    if (accepted) {
                                        handledValidScan.set(true)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    onCameraBound(camera)
                } catch (e: Exception) {
                    onCameraError(
                        e.localizedMessage ?: "Failed to initialize camera preview."
                    )
                }
            },
            mainExecutor
        )

        onDispose {
            runCatching { activeProvider?.unbindAll() }
            runCatching { barcodeScanner.close() }
            runCatching { analysisExecutor.shutdown() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun QrViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        val boxSide = (minOf(size.width, size.height) * 0.66f).coerceIn(200.dp.toPx(), 300.dp.toPx())
        val left = (size.width - boxSide) / 2f
        val top = (size.height - boxSide) / 2f - 20.dp.toPx()
        val cornerRadius = 22.dp.toPx()

        // Dimmed backdrop
        drawRect(color = Color.Black.copy(alpha = 0.56f))

        // Clear viewfinder window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Subtle inner white border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Highlighted Harvest Gold corner brackets
        val bracketLen = 30.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val bracketColor = Color(0xFFF2C879)

        // Top-Left
        drawLine(bracketColor, Offset(left, top + cornerRadius), Offset(left, top + bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + cornerRadius, top), Offset(left + bracketLen, top), strokeWidth, StrokeCap.Round)
        // Top-Right
        drawLine(bracketColor, Offset(left + boxSide, top + cornerRadius), Offset(left + boxSide, top + bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + boxSide - cornerRadius, top), Offset(left + boxSide - bracketLen, top), strokeWidth, StrokeCap.Round)
        // Bottom-Left
        drawLine(bracketColor, Offset(left, top + boxSide - cornerRadius), Offset(left, top + boxSide - bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + cornerRadius, top + boxSide), Offset(left + bracketLen, top + boxSide), strokeWidth, StrokeCap.Round)
        // Bottom-Right
        drawLine(bracketColor, Offset(left + boxSide, top + boxSide - cornerRadius), Offset(left + boxSide, top + boxSide - bracketLen), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + boxSide - cornerRadius, top + boxSide), Offset(left + boxSide - bracketLen, top + boxSide), strokeWidth, StrokeCap.Round)
    }
}
