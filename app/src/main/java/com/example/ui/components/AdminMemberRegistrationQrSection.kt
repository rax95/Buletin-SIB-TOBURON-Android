package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.BuildConfig
import com.example.data.MemberQrUtils
import com.example.data.MemberRegistrationConfig
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun AdminQuickRegistrationQrTile(
    config: MemberRegistrationConfig?,
    onEnsureConfig: () -> Unit,
    tileSize: Dp,
    modifier: Modifier = Modifier
) {
    var requestedAutoInit by remember { mutableStateOf(false) }
    var showEnlargedQr by remember { mutableStateOf(false) }

    LaunchedEffect(config?.portalUrl, config?.accessToken) {
        if ((config == null || config.isUnconfiguredGitHubPagesUrl()) && !requestedAutoInit) {
            requestedAutoInit = true
            onEnsureConfig()
        }
    }

    val fullUrl = remember(config?.portalUrl, config?.accessToken) {
        if (config != null && config.accessToken.isNotBlank()) {
            config.buildFullRegistrationUrl()
        } else {
            ""
        }
    }
    val isEnabled = config?.enabled == true

    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = fullUrl) {
        value = if (fullUrl.isBlank()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                MemberQrUtils.generateQrCodeBitmap(content = fullUrl, sizePx = 640)
            }
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .size(tileSize)
            .testTag("admin_member_reg_quick_qr_card")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (qrBitmap != null) {
                        showEnlargedQr = true
                    } else {
                        onEnsureConfig()
                    }
                }
                .padding(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                ) {
                    val bmp = qrBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = tr(
                                "Kod QR pendaftaran ahli - ketik untuk besarkan",
                                "Member registration QR code - tap to enlarge"
                            ),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isEnabled) 1f else 0.4f)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }

    if (showEnlargedQr && qrBitmap != null) {
        MemberRegistrationEnlargedQrDialog(
            qrBitmap = qrBitmap!!,
            isEnabled = isEnabled,
            onDismiss = { showEnlargedQr = false }
        )
    }
}

@Composable
fun MemberRegistrationEnlargedQrDialog(
    qrBitmap: ImageBitmap,
    isEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_reg_enlarged_qr_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("QR Pendaftaran Ahli", "Member Registration QR"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_reg_enlarged_qr_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("Tutup", "Close")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(280.dp)
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = tr("Kod QR penuh pendaftaran ahli", "Full-size member registration QR code"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .alpha(if (isEnabled) 1f else 0.45f)
                    )
                }

                if (!isEnabled) {
                    Text(
                        text = tr(
                            "Pendaftaran web sedang dijeda oleh Admin.",
                            "Web registration is currently paused by Admin."
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = tr(
                        "Imbas dengan kamera iPhone / telefon anda untuk membuka borang pendaftaran ahli SIB Toburon.",
                        "Scan with your iPhone / phone camera to open the SIB Toburon member registration form."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AdminMemberRegistrationQrView(
    config: MemberRegistrationConfig?,
    isBusy: Boolean,
    onBack: () -> Unit,
    onEnsureConfig: () -> Unit,
    onSaveConfig: (portalUrl: String, accessToken: String, enabled: Boolean) -> Unit,
    onRegenerateToken: () -> Unit,
    onCopyLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requestedAutoInit by remember { mutableStateOf(false) }
    var showEnlargedQr by remember { mutableStateOf(false) }
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var showEditPortalDialog by remember { mutableStateOf(false) }
    var isSharingQr by remember { mutableStateOf(false) }

    LaunchedEffect(config?.portalUrl, config?.accessToken) {
        if ((config == null || config.isUnconfiguredGitHubPagesUrl()) && !requestedAutoInit) {
            requestedAutoInit = true
            onEnsureConfig()
        }
    }

    val fullUrl = remember(config?.portalUrl, config?.accessToken) {
        if (config != null && config.accessToken.isNotBlank()) {
            config.buildFullRegistrationUrl()
        } else {
            ""
        }
    }
    val isEnabled = config?.enabled == true

    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = fullUrl) {
        value = if (fullUrl.isBlank()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                MemberQrUtils.generateQrCodeBitmap(content = fullUrl, sizePx = 640)
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("admin_member_reg_qr_page")
        ) {
            AdminSubPageHeader(
                title = tr("QR & Pautan Pendaftaran Ahli", "Member Registration QR & Link"),
                subtitle = tr(
                    "Borang web terus ke Firestore untuk iPhone / iOS & bukan pengguna Android",
                    "Direct web form to Firestore for iPhone / iOS & non-Android users"
                ),
                badgeText = when {
                    config == null -> tr("Sediakan", "Setup")
                    isEnabled -> tr("Aktif", "Active")
                    else -> tr("Dijeda", "Paused")
                },
                onBack = onBack
            )

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    // Header row + Active/Paused Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Kod QR Pendaftaran Rasmi", "Official Registration QR Code"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEnabled) {
                                    tr(
                                        "Pautan aktif — pendaftaran baharu terus masuk ke senarai ahli.",
                                        "Link active — new submissions appear directly in Members."
                                    )
                                } else {
                                    tr(
                                        "Pautan dijeda — borang web tidak menerima pendaftaran baharu.",
                                        "Link paused — web form temporarily blocks new submissions."
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (config != null) {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    onSaveConfig(
                                        config.portalUrl.ifBlank { MemberRegistrationConfig.DEFAULT_PORTAL_URL },
                                        config.accessToken,
                                        checked
                                    )
                                },
                                enabled = !isBusy,
                                modifier = Modifier.testTag("admin_reg_enabled_switch")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (config == null || fullUrl.isBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Text(
                                text = tr(
                                    "Menyediakan kod QR & token keselamatan pendaftaran...",
                                    "Generating registration QR code & security token..."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            FilledTonalButton(
                                onClick = onEnsureConfig,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_reg_init_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(tr("Jana Pautan QR Sekarang", "Generate QR Link Now"))
                            }
                        }
                    } else {
                        if (config.isUnconfiguredGitHubPagesUrl()) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_reg_github_pages_prompt")
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = tr(
                                            "Tetapkan URL GitHub Pages Anda",
                                            "Set Your GitHub Pages URL"
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = tr(
                                            "Aktifkan GitHub Pages (Branch: main, Folder: /docs) di repositori GitHub anda, kemudian masukkan pautan https://<username>.github.io/<repo> di bawah sebelum berkongsi kod QR.",
                                            "Enable GitHub Pages (Branch: main, Folder: /docs) in your GitHub repository, then enter your https://<username>.github.io/<repo> URL below before sharing the QR code."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    FilledTonalButton(
                                        onClick = { showEditPortalDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr("Masukkan URL GitHub Pages", "Enter GitHub Pages URL"))
                                    }
                                }
                            }
                        }

                        // Centered QR Code Preview Card
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isEnabled) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(228.dp)
                                    .clickable(onClick = { showEnlargedQr = true })
                                    .testTag("admin_reg_qr_preview")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp)
                                ) {
                                    val bmp = qrBitmap
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = tr(
                                                "Kod QR pendaftaran ahli gereja SIB Toburon",
                                                "SIB Toburon church member registration QR code"
                                            ),
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            Text(
                                text = tr(
                                    "Ketik kod QR untuk paparan penuh • Imbas dengan kamera iPhone/Android",
                                    "Tap QR code to enlarge • Scan with iPhone/Android camera"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Registration URL display box
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tr("PAUTAN WEB PENDAFTARAN AHLI", "MEMBER REGISTRATION WEB LINK"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = fullUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("admin_reg_full_url_text")
                                )
                            }
                        }

                        // Primary Actions Row: Copy Link + Share QR & Link
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onCopyLink(fullUrl) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_reg_copy_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Salin Pautan", "Copy Link"))
                            }

                            Button(
                                onClick = {
                                    if (isSharingQr) return@Button
                                    scope.launch {
                                        isSharingQr = true
                                        val shareUri = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val bmp = MemberQrUtils.generateQrBitmapRaw(fullUrl, sizePx = 900)
                                                    ?: return@runCatching null
                                                val dir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
                                                val outFile = File(dir, "sib_toburon_registration_qr.png")
                                                FileOutputStream(outFile).use { fos ->
                                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                                }
                                                FileProvider.getUriForFile(
                                                    context,
                                                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                    outFile
                                                )
                                            }.getOrNull()
                                        }
                                        isSharingQr = false
                                        val shareText = AppLanguageManager.tr(
                                            "Pendaftaran Ahli Gereja SIB Toburon\nImbas kod QR atau buka pautan ini di pelayar iPhone/telefon anda:\n$fullUrl",
                                            "SIB Toburon Church Member Registration\nScan the QR code or open this link in your iPhone/phone browser:\n$fullUrl"
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            if (shareUri != null) {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            } else {
                                                type = "text/plain"
                                            }
                                            putExtra(
                                                Intent.EXTRA_SUBJECT,
                                                AppLanguageManager.tr("Pendaftaran Ahli SIB Toburon", "SIB Toburon Member Registration")
                                            )
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        runCatching {
                                            context.startActivity(
                                                Intent.createChooser(
                                                    intent,
                                                    AppLanguageManager.tr("Kongsi QR & Pautan Pendaftaran", "Share Registration QR & Link")
                                                )
                                            )
                                        }
                                    }
                                },
                                enabled = !isSharingQr,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_reg_share_qr_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Kongsi QR", "Share QR"))
                            }
                        }

                        // Secondary Actions Row: Open Browser + Regenerate QR
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, fullUrl.toUri()))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_reg_open_browser_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Buka Pelayar", "Open Browser"))
                            }

                            OutlinedButton(
                                onClick = { showRegenerateConfirm = true },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_reg_regenerate_token_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Jana Semula", "Regenerate"))
                            }
                        }

                        TextButton(
                            onClick = { showEditPortalDialog = true },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_reg_edit_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Ubah URL GitHub Pages", "Change GitHub Pages URL"))
                        }
                    }
                }
            }

            // Guidance / How-It-Works Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("Cara Penggunaan (GitHub Pages + iPhone / Bukan Android)", "How It Works (GitHub Pages + iPhone / Non-Android)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = tr(
                                "1. Tolak projek ke GitHub dan aktifkan Settings -> Pages -> Branch: main, Folder: /docs.\n2. Masukkan pautan GitHub Pages anda di atas (contoh: https://nama.github.io/sib-toburon).\n3. Kongsi kod QR ini kepada jemaat. Imbasan kamera iPhone/telefon lain akan membuka borang web dan menyimpan ahli terus ke senarai Admin.",
                                "1. Push this project to GitHub and enable Settings -> Pages -> Branch: main, Folder: /docs.\n2. Enter your GitHub Pages URL above (e.g., https://name.github.io/sib-toburon).\n3. Share this QR code with members. Scanning with an iPhone or any phone camera opens the web form and adds members straight to the Admin list."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showEnlargedQr && qrBitmap != null) {
        MemberRegistrationEnlargedQrDialog(
            qrBitmap = qrBitmap!!,
            isEnabled = isEnabled,
            onDismiss = { showEnlargedQr = false }
        )
    }

    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = {
                Text(tr("Jana Semula Kod QR?", "Regenerate QR Code?"))
            },
            text = {
                Text(
                    tr(
                        "Token keselamatan baharu akan dicipta. Sebarang kod QR atau pautan lama yang pernah dicetak tidak lagi boleh digunakan.",
                        "A new security token will be generated. Any previously printed QR codes or old links will stop working."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateConfirm = false
                        onRegenerateToken()
                    },
                    modifier = Modifier.testTag("admin_reg_confirm_regenerate_button")
                ) {
                    Text(tr("Jana Semula", "Regenerate"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirm = false }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }

    if (showEditPortalDialog && config != null) {
        var portalInput by remember(config.portalUrl) {
            mutableStateOf(
                if (config.isUnconfiguredGitHubPagesUrl()) {
                    ""
                } else {
                    config.portalUrl
                }
            )
        }
        var urlError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditPortalDialog = false },
            title = {
                Text(tr("URL GitHub Pages Pendaftaran", "GitHub Pages Registration URL"))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(
                            "Masukkan URL GitHub Pages anda yang menghoskan folder /docs (contoh: https://raxxon47.github.io/sib-toburon).",
                            "Enter your GitHub Pages URL serving the /docs folder (e.g. https://raxxon47.github.io/sib-toburon)."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = portalInput,
                        onValueChange = {
                            portalInput = it
                            urlError = null
                        },
                        label = { Text("https://username.github.io/repo") },
                        placeholder = { Text("https://username.github.io/sib-toburon") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { err -> { Text(err) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_reg_portal_url_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = MemberRegistrationConfig.normalizePortalUrl(portalInput)
                        if (!cleaned.startsWith("https://") || cleaned.length <= 10 || '.' !in cleaned) {
                            urlError = AppLanguageManager.tr(
                                "URL mestilah pautan HTTPS yang sah (contoh: https://nama.github.io/repo)",
                                "URL must be a valid HTTPS link (e.g. https://name.github.io/repo)"
                            )
                            return@Button
                        }
                        showEditPortalDialog = false
                        onSaveConfig(cleaned, config.accessToken, config.enabled)
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_reg_save_portal_url_button")
                ) {
                    Text(tr("Simpan", "Save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditPortalDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }
}
