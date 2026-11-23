package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.AppVersionConfig
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.tr

@Composable
fun AppVersionFooterCard(
    versionConfig: AppVersionConfig?,
    onOpenUpdateNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentName = BuildConfig.VERSION_NAME
    val currentCode = BuildConfig.VERSION_CODE
    val hasUpdate = versionConfig?.isUpdateAvailable(currentCode) == true

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (hasUpdate) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasUpdate) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_version_footer_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (hasUpdate) Icons.Filled.SystemUpdate else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (hasUpdate) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = tr("Versi Aplikasi", "App Version"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr(
                                "Dipasang: v$currentName (Kod $currentCode)",
                                "Installed: v$currentName (Build $currentCode)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (hasUpdate) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (hasUpdate) Icons.Filled.NewReleases else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (hasUpdate) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (hasUpdate) {
                                tr("v${versionConfig?.latestVersionName}", "v${versionConfig?.latestVersionName}")
                            } else {
                                tr("Terkini", "Up to date")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasUpdate) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
            }

            if (hasUpdate && versionConfig != null) {
                Text(
                    text = tr(
                        "Kemas kini baharu v${versionConfig.latestVersionName} (Kod ${versionConfig.latestVersionCode}) kini tersedia untuk jemaat.",
                        "A new update v${versionConfig.latestVersionName} (Build ${versionConfig.latestVersionCode}) is now available."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (hasUpdate) {
                Button(
                    onClick = onOpenUpdateNotice,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_update_notice_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Lihat Kemas Kini", "View Update"))
                }
            }
        }
    }
}

@Composable
fun AppUpdateAvailableDialog(
    config: AppVersionConfig,
    onDismiss: () -> Unit,
    onOpenUrlError: (String) -> Unit
) {
    val context = LocalContext.current
    val hasValidUrl = config.updateUrl.startsWith("https://")

    AlertDialog(
        onDismissRequest = {
            if (!config.forceUpdate) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !config.forceUpdate,
            dismissOnClickOutside = !config.forceUpdate
        ),
        icon = {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (config.forceUpdate) {
                    tr("Kemas Kini Wajib Tersedia", "Required Update Available")
                } else {
                    tr("Kemas Kini Baharu Tersedia", "New Update Available")
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = tr(
                                "Versi Baharu: v${config.latestVersionName} (Kod ${config.latestVersionCode})",
                                "New Version: v${config.latestVersionName} (Build ${config.latestVersionCode})"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = tr(
                                "Versi semasa anda: v${BuildConfig.VERSION_NAME} (Kod ${BuildConfig.VERSION_CODE})",
                                "Your current version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                if (config.releaseNotes.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = tr("Apa yang baharu:", "What's new:"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = config.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!hasValidUrl) {
                    Text(
                        text = tr(
                            "Sila dapatkan fail kemas kini versi terkini daripada pentadbir gereja SIB Toburon.",
                            "Please obtain the latest update build from the SIB Toburon church administrator."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (hasValidUrl) {
                Button(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.updateUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            onOpenUrlError(
                                AppLanguageManager.tr(
                                    "Tidak dapat membuka pautan kemas kini.",
                                    "Unable to open update link."
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("app_update_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Muat Turun Kemas Kini", "Download Update"))
                }
            } else if (!config.forceUpdate) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("app_update_ok_button")
                ) {
                    Text(tr("Faham", "Got It"))
                }
            }
        },
        dismissButton = {
            if (!config.forceUpdate && hasValidUrl) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("app_update_later_button")
                ) {
                    Text(tr("Nanti", "Later"))
                }
            }
        }
    )
}

@Composable
fun AdminAppVersionDialog(
    currentConfig: AppVersionConfig?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        latestVersionCode: Int,
        latestVersionName: String,
        releaseNotes: String,
        updateUrl: String,
        forceUpdate: Boolean
    ) -> Unit
) {
    var versionCodeText by remember(currentConfig) {
        mutableStateOf((currentConfig?.latestVersionCode ?: BuildConfig.VERSION_CODE).toString())
    }
    var versionName by remember(currentConfig) {
        mutableStateOf(currentConfig?.latestVersionName?.ifBlank { BuildConfig.VERSION_NAME } ?: BuildConfig.VERSION_NAME)
    }
    var updateUrl by remember(currentConfig) {
        mutableStateOf(currentConfig?.updateUrl ?: "")
    }
    var releaseNotes by remember(currentConfig) {
        mutableStateOf(currentConfig?.releaseNotes ?: "")
    }
    var forceUpdate by remember(currentConfig) {
        mutableStateOf(currentConfig?.forceUpdate ?: false)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_app_version_dialog"),
        title = {
            Text(
                text = tr("Urus Versi & Kemas Kini App", "Manage App Version & Updates"),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "Binaan pada peranti ini: v${BuildConfig.VERSION_NAME} (Kod ${BuildConfig.VERSION_CODE}). Apabila anda terbitkan versi baharu, tetapkan Kod Versi lebih tinggi supaya semua pengguna menerima notis kemas kini.",
                            "This device's build: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}). When you release a newer build, set a higher Version Code so users automatically receive an update notice."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it.take(32); validationError = null },
                        label = { Text(tr("Nama Versi *", "Version Name *")) },
                        placeholder = { Text("1.1.0") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("admin_version_name_input")
                    )
                    OutlinedTextField(
                        value = versionCodeText,
                        onValueChange = {
                            versionCodeText = it.filter { ch -> ch.isDigit() }.take(7)
                            validationError = null
                        },
                        label = { Text(tr("Kod Versi *", "Version Code *")) },
                        placeholder = { Text("2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_version_code_input")
                    )
                }

                OutlinedTextField(
                    value = updateUrl,
                    onValueChange = { updateUrl = it.take(500); validationError = null },
                    label = { Text(tr("Pautan Muat Turun / Play Store (HTTPS)", "Download / Play Store URL (HTTPS)")) },
                    placeholder = { Text("https://play.google.com/store/apps/details?id=...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_version_url_input")
                )

                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it.take(1000); validationError = null },
                    label = { Text(tr("Nota Perubahan (Pilihan)", "Release Notes (Optional)")) },
                    placeholder = {
                        Text(
                            tr(
                                "Contoh: Penambahbaikan Kad Ahli, logo rasmi baharu & galeri foto.",
                                "Example: Member ID card improvements, new official logo & photo gallery."
                            )
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_version_notes_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Wajibkan Kemas Kini", "Require Update"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = tr(
                                "Halang pengguna menutup notis jika versi lama",
                                "Prevent dismissing notice on older versions"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = forceUpdate,
                        onCheckedChange = { forceUpdate = it },
                        modifier = Modifier.testTag("admin_version_force_switch")
                    )
                }

                if (validationError != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    val code = versionCodeText.trim().toIntOrNull()
                    val cleanName = versionName.trim()
                    val cleanUrl = updateUrl.trim()
                    val cleanNotes = releaseNotes.trim()
                    when {
                        code == null || code !in 1..1_000_000 -> validationError = AppLanguageManager.tr(
                            "Kod versi mestilah nombor bulat antara 1 hingga 1000000.",
                            "Version code must be a whole number between 1 and 1000000."
                        )
                        cleanName.isEmpty() -> validationError = AppLanguageManager.tr(
                            "Nama versi wajib diisi (contoh: 1.1.0).",
                            "Version name is required (e.g., 1.1.0)."
                        )
                        cleanUrl.isNotEmpty() && !cleanUrl.startsWith("https://") -> validationError = AppLanguageManager.tr(
                            "Pautan kemas kini mesti bermula dengan https://",
                            "Update URL must start with https://"
                        )
                        else -> onSave(code, cleanName, cleanNotes, cleanUrl, forceUpdate)
                    }
                },
                modifier = Modifier.testTag("admin_save_version_button")
            ) {
                Text(if (isBusy) tr("Menyimpan…", "Saving…") else tr("Simpan Versi", "Save Version"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}
