package com.example.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.ImageUtils
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.tr
import java.io.File

/**
 * Reusable camera capture helper that handles runtime CAMERA permission,
 * FileProvider temp file creation (surviving activity recreation via rememberSaveable),
 * ActivityResultContracts.TakePicture, and cleanup of the temp file after processing.
 */
@Composable
fun rememberCameraCaptureLauncher(
    onPhotoCaptured: (uri: Uri, onCleanup: () -> Unit) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var pendingFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingFilePath
        pendingFilePath = null
        val file = path?.let { File(it) }
        if (success && file != null && file.exists() && file.length() > 0L) {
            val capturedUri = runCatching {
                ImageUtils.getCameraTempImageUri(context, file)
            }.getOrNull()
            if (capturedUri != null) {
                onPhotoCaptured(capturedUri) {
                    runCatching { file.delete() }
                }
            } else {
                runCatching { file.delete() }
                onError(
                    AppLanguageManager.tr(
                        "Gagal membaca gambar yang diambil daripada kamera.",
                        "Failed to read photo captured from camera."
                    )
                )
            }
        } else {
            runCatching { file?.delete() }
        }
    }

    fun launchSystemCamera() {
        try {
            val tempFile = ImageUtils.createCameraTempImageFile(context)
            val tempUri = ImageUtils.getCameraTempImageUri(context, tempFile)
            pendingFilePath = tempFile.absolutePath
            takePictureLauncher.launch(tempUri)
        } catch (_: ActivityNotFoundException) {
            pendingFilePath = null
            onError(
                AppLanguageManager.tr(
                    "Aplikasi kamera tidak ditemui pada peranti ini.",
                    "No camera app was found on this device."
                )
            )
        } catch (e: Exception) {
            pendingFilePath = null
            onError(
                e.localizedMessage ?: AppLanguageManager.tr(
                    "Gagal membuka kamera peranti.",
                    "Failed to open device camera."
                )
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSystemCamera()
        } else {
            onError(
                AppLanguageManager.tr(
                    "Kebenaran kamera diperlukan untuk mengambil gambar. Anda masih boleh memilih daripada galeri.",
                    "Camera permission is required to take a photo. You can still choose from gallery."
                )
            )
        }
    }

    return {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            launchSystemCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Clean bilingual photo source dialog offering Camera capture, Gallery selection,
 * and optional existing photo removal.
 */
@Composable
fun PhotoSourceChooserDialog(
    title: String,
    subtitle: String,
    hasExistingPhoto: Boolean,
    onTakePhotoClick: () -> Unit,
    onChooseGalleryClick: () -> Unit,
    onRemovePhotoClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    tagPrefix: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("${tagPrefix}_photo_source_dialog"),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        onDismiss()
                        onTakePhotoClick()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("${tagPrefix}_source_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Ambil Foto (Kamera)", "Take Photo (Camera)"))
                }

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onChooseGalleryClick()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("${tagPrefix}_source_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Pilih dari Galeri", "Choose from Gallery"))
                }

                if (hasExistingPhoto && onRemovePhotoClick != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRemovePhotoClick()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("${tagPrefix}_source_remove_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tr("Padam Foto Semasa", "Remove Current Photo"),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .testTag("${tagPrefix}_source_cancel_button")
            ) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}
