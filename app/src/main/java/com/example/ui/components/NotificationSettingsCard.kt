package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.BulletinNotificationManager
import com.example.ui.i18n.tr

@Composable
fun NotificationSettingsCard(
    isAdmin: Boolean,
    onPreferencesUpdated: (newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(BulletinNotificationManager.hasNotificationPermission(context))
    }
    var masterEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isMasterPushEnabled(context))
    }
    var newsEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isNewsNotificationsEnabled(context))
    }
    var schedulesEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isScheduleNotificationsEnabled(context))
    }
    var shouldOpenSettings by remember {
        mutableStateOf(BulletinNotificationManager.shouldOpenSystemSettingsForPermission(context))
    }
    var testSentFeedback by remember { mutableStateOf(false) }

    fun refreshLocalState(syncIfPermitted: Boolean = false) {
        val prevPerm = hasPermission
        hasPermission = BulletinNotificationManager.hasNotificationPermission(context)
        masterEnabled = BulletinNotificationManager.isMasterPushEnabled(context)
        newsEnabled = BulletinNotificationManager.isNewsNotificationsEnabled(context)
        schedulesEnabled = BulletinNotificationManager.isScheduleNotificationsEnabled(context)
        shouldOpenSettings = BulletinNotificationManager.shouldOpenSystemSettingsForPermission(context)
        if (syncIfPermitted && !prevPerm && hasPermission && masterEnabled) {
            BulletinNotificationManager.ensureNotificationChannel(context)
            BulletinNotificationManager.syncTopicSubscriptions(
                context = context,
                newsEnabled = newsEnabled,
                schedulesEnabled = schedulesEnabled,
                isAdmin = isAdmin,
                masterEnabled = true,
                optedOut = false
            )
            onPreferencesUpdated(newsEnabled, schedulesEnabled)
        }
    }

    // Refresh permission & saved preferences whenever user returns from Android system settings
    DisposableEffect(lifecycleOwner, context, isAdmin) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshLocalState(syncIfPermitted = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        BulletinNotificationManager.markPermissionRequested(context)
        hasPermission = granted && BulletinNotificationManager.hasNotificationPermission(context)
        shouldOpenSettings = BulletinNotificationManager.shouldOpenSystemSettingsForPermission(context)
        if (hasPermission) {
            masterEnabled = true
            if (!newsEnabled && !schedulesEnabled) {
                newsEnabled = true
                schedulesEnabled = true
            }
            BulletinNotificationManager.optInPushNotifications(
                context = context,
                newsEnabled = newsEnabled,
                schedulesEnabled = schedulesEnabled,
                isAdmin = isAdmin
            )
            onPreferencesUpdated(newsEnabled, schedulesEnabled)
        }
    }

    fun requestOrOpenNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !BulletinNotificationManager.shouldOpenSystemSettingsForPermission(context)
        ) {
            BulletinNotificationManager.markPermissionRequested(context)
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            BulletinNotificationManager.openAppNotificationSettings(context)
        }
    }

    val anyCategoryEnabled = newsEnabled || schedulesEnabled
    val isPushActive = hasPermission && masterEnabled && anyCategoryEnabled

    val statusBadgeText = when {
        !masterEnabled -> tr("Dimatikan", "Opted Out")
        !hasPermission -> tr("Belum Aktif", "Inactive")
        !anyCategoryEnabled -> tr("Tiada Kategori", "Muted")
        else -> tr("Aktif", "Active")
    }
    val statusBadgeBg = when {
        isPushActive -> MaterialTheme.colorScheme.primaryContainer
        !masterEnabled -> MaterialTheme.colorScheme.surfaceVariant
        !hasPermission -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusBadgeFg = when {
        isPushActive -> MaterialTheme.colorScheme.onPrimaryContainer
        !masterEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        !hasPermission -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusDescription = when {
        !masterEnabled -> tr(
            "Anda telah memilih keluar daripada notifikasi tolak. Hidupkan suis di bawah bila-bila masa untuk menerima berita dan jadual baharu SIB Toburon.",
            "You have opted out of push notifications. Turn on the switch below anytime to receive new SIB Toburon bulletins and events."
        )
        !hasPermission -> tr(
            "Aktifkan kebenaran notifikasi peranti agar tidak terlepas warta jemaat dan jadual ibadah terbaharu SIB Toburon.",
            "Enable device notification permission so you never miss the latest SIB Toburon bulletins and worship events."
        )
        !anyCategoryEnabled -> tr(
            "Notifikasi utama aktif tetapi tiada kategori dipilih. Hidupkan sekurang-kurangnya satu kategori di bawah.",
            "Push notifications are on, but no categories are selected. Turn on at least one category below."
        )
        newsEnabled && schedulesEnabled -> tr(
            "Notifikasi masa nyata aktif untuk Berita & Jadual Kegiatan.",
            "Real-time push alerts are active for News & Event Schedules."
        )
        newsEnabled -> tr(
            "Notifikasi masa nyata aktif untuk Berita & Warta sahaja.",
            "Real-time push alerts are active for News & Bulletins only."
        )
        else -> tr(
            "Notifikasi masa nyata aktif untuk Jadual Kegiatan sahaja.",
            "Real-time push alerts are active for Event Schedules only."
        )
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_settings_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPushActive) Icons.Filled.NotificationsActive
                        else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (isPushActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = tr("Notifikasi Tolak (FCM)", "Push Notifications (FCM)"),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBadgeBg,
                    modifier = Modifier.testTag("notif_status_badge")
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusBadgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Prominent Master Opt-In / Opt-Out Switch Panel
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isPushActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isPushActive) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Terima Notifikasi Tolak", "Receive Push Notifications"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (masterEnabled && hasPermission) {
                                tr("Hidup • Matikan di sini untuk berhenti menerima", "On • Turn off here to opt out anytime")
                            } else if (!masterEnabled) {
                                tr("Dimatikan (Keluar) • Hidupkan untuk kekal terkini", "Disabled (Opted out) • Turn on to stay updated")
                            } else {
                                tr("Hidupkan kebenaran peranti untuk mengaktifkan", "Allow device permission to activate")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = masterEnabled && hasPermission,
                        onCheckedChange = { enable ->
                            testSentFeedback = false
                            if (!enable) {
                                // Explicit opt-out
                                masterEnabled = false
                                newsEnabled = false
                                schedulesEnabled = false
                                BulletinNotificationManager.optOutPushNotifications(context)
                                onPreferencesUpdated(false, false)
                            } else {
                                masterEnabled = true
                                if (!newsEnabled && !schedulesEnabled) {
                                    newsEnabled = true
                                    schedulesEnabled = true
                                }
                                if (hasPermission) {
                                    BulletinNotificationManager.optInPushNotifications(
                                        context = context,
                                        newsEnabled = newsEnabled,
                                        schedulesEnabled = schedulesEnabled,
                                        isAdmin = isAdmin
                                    )
                                    onPreferencesUpdated(newsEnabled, schedulesEnabled)
                                } else {
                                    BulletinNotificationManager.savePreferences(
                                        context = context,
                                        newsEnabled = newsEnabled,
                                        schedulesEnabled = schedulesEnabled,
                                        masterEnabled = true,
                                        optedOut = false
                                    )
                                    requestOrOpenNotificationSettings()
                                }
                            }
                        },
                        modifier = Modifier.testTag("master_push_switch")
                    )
                }
            }

            // Action button whenever OS notification permission is missing
            if (!hasPermission) {
                Button(
                    onClick = {
                        masterEnabled = true
                        if (!newsEnabled && !schedulesEnabled) {
                            newsEnabled = true
                            schedulesEnabled = true
                        }
                        BulletinNotificationManager.savePreferences(
                            context = context,
                            newsEnabled = newsEnabled,
                            schedulesEnabled = schedulesEnabled,
                            masterEnabled = true,
                            optedOut = false
                        )
                        requestOrOpenNotificationSettings()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("enable_notif_permission_button")
                ) {
                    Icon(
                        imageVector = if (shouldOpenSettings) Icons.Filled.Settings else Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (shouldOpenSettings) {
                            tr("Buka Tetapan Notifikasi Peranti", "Open Device Notification Settings")
                        } else {
                            tr("Aktifkan Notifikasi Tolak", "Enable Push Notifications")
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Category switches (shown when user has not opted out)
            AnimatedVisibility(visible = masterEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // News push toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Newspaper,
                                contentDescription = null,
                                tint = if (newsEnabled && hasPermission) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tr("Berita & Warta Terkini", "Latest Church News & Bulletins"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (newsEnabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (newsEnabled && hasPermission) {
                                        tr("Aktif • ", "Active • ") + BulletinNotificationManager.TOPIC_NEWS
                                    } else {
                                        tr("Dinyahaktifkan • ", "Disabled • ") + BulletinNotificationManager.TOPIC_NEWS
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = newsEnabled,
                            onCheckedChange = { checked ->
                                newsEnabled = checked
                                val stillEnabled = newsEnabled || schedulesEnabled
                                masterEnabled = stillEnabled
                                BulletinNotificationManager.syncTopicSubscriptions(
                                    context = context,
                                    newsEnabled = newsEnabled,
                                    schedulesEnabled = schedulesEnabled,
                                    isAdmin = isAdmin,
                                    masterEnabled = stillEnabled,
                                    optedOut = !stillEnabled
                                )
                                onPreferencesUpdated(newsEnabled, schedulesEnabled)
                            },
                            modifier = Modifier.testTag("notif_news_switch")
                        )
                    }

                    // Schedules push toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = if (schedulesEnabled && hasPermission) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tr("Jadual Kegiatan Gereja", "Church Event Schedules"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (schedulesEnabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (schedulesEnabled && hasPermission) {
                                        tr("Aktif • ", "Active • ") + BulletinNotificationManager.TOPIC_SCHEDULES
                                    } else {
                                        tr("Dinyahaktifkan • ", "Disabled • ") + BulletinNotificationManager.TOPIC_SCHEDULES
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = schedulesEnabled,
                            onCheckedChange = { checked ->
                                schedulesEnabled = checked
                                val stillEnabled = newsEnabled || schedulesEnabled
                                masterEnabled = stillEnabled
                                BulletinNotificationManager.syncTopicSubscriptions(
                                    context = context,
                                    newsEnabled = newsEnabled,
                                    schedulesEnabled = schedulesEnabled,
                                    isAdmin = isAdmin,
                                    masterEnabled = stillEnabled,
                                    optedOut = !stillEnabled
                                )
                                onPreferencesUpdated(newsEnabled, schedulesEnabled)
                            },
                            modifier = Modifier.testTag("notif_schedules_switch")
                        )
                    }
                }
            }

            if (isPushActive) {
                OutlinedButton(
                    onClick = {
                        testSentFeedback = BulletinNotificationManager.showTestNotification(context)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("test_push_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (testSentFeedback) {
                            tr("Notifikasi Ujian Dihantar!", "Test Notification Sent!")
                        } else {
                            tr("Hantar Notifikasi Ujian", "Send Test Notification")
                        }
                    )
                }
            }

            if (isAdmin) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "Info Admin: Setiap Berita atau Jadual yang diterbitkan akan mencetuskan notifikasi masa nyata secara automatik kepada jemaat. Anda juga dilanggan kepada makluman pendaftaran ahli baharu.",
                            "Admin Info: Every published Bulletin or Event automatically triggers real-time push notifications to the congregation. You are also subscribed to new member registration alerts."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PushNotificationOptInDialog(
    isAdmin: Boolean,
    onEnabled: (newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit,
    onOptOut: () -> Unit,
    onDismissForNow: () -> Unit
) {
    val context = LocalContext.current
    var newsSelected by remember {
        mutableStateOf(BulletinNotificationManager.isNewsNotificationsEnabled(context))
    }
    var schedulesSelected by remember {
        mutableStateOf(BulletinNotificationManager.isScheduleNotificationsEnabled(context))
    }
    val needsSystemSettings = remember(context) {
        BulletinNotificationManager.shouldOpenSystemSettingsForPermission(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        BulletinNotificationManager.markPermissionRequested(context)
        if (granted && BulletinNotificationManager.hasNotificationPermission(context)) {
            BulletinNotificationManager.optInPushNotifications(
                context = context,
                newsEnabled = newsSelected,
                schedulesEnabled = schedulesSelected,
                isAdmin = isAdmin
            )
            onEnabled(newsSelected, schedulesSelected)
        } else {
            onDismissForNow()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissForNow,
        modifier = Modifier.testTag("push_notification_opt_in_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = tr(
                    "Aktifkan Notifikasi SIB Toburon",
                    "Enable SIB Toburon Notifications"
                ),
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
                Text(
                    text = tr(
                        "Terima makluman masa nyata setiap kali warta gereja atau jadual kegiatan baharu diterbitkan agar anda sentiasa terkini.",
                        "Receive real-time push alerts whenever a new church bulletin or event schedule is published so you stay up to date."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = newsSelected,
                                onCheckedChange = { newsSelected = it },
                                modifier = Modifier.testTag("opt_in_news_checkbox")
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = tr("Berita & Warta Gereja", "Church Bulletins & News"),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = tr("Pengumuman dan warta terbaharu", "Latest announcements & bulletins"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = schedulesSelected,
                                onCheckedChange = { schedulesSelected = it },
                                modifier = Modifier.testTag("opt_in_schedules_checkbox")
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = tr("Jadual Ibadah & Kegiatan", "Worship & Event Schedules"),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = tr("Peringatan ibadah dan aktiviti gereja", "Service times & church events"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = tr(
                        "Anda boleh menyahaktifkan notifikasi bila-bila masa melalui tab Akaun.",
                        "You can opt out or change categories anytime in the Account tab."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        BulletinNotificationManager.optOutPushNotifications(context)
                        onOptOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opt_in_disable_button")
                ) {
                    Text(
                        text = tr("Tidak mahu notifikasi (Matikan)", "Don't enable (Opt out)"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = newsSelected || schedulesSelected,
                onClick = {
                    BulletinNotificationManager.savePreferences(
                        context = context,
                        newsEnabled = newsSelected,
                        schedulesEnabled = schedulesSelected,
                        masterEnabled = true,
                        optedOut = false
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !needsSystemSettings) {
                        BulletinNotificationManager.markPermissionRequested(context)
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        BulletinNotificationManager.openAppNotificationSettings(context)
                        onDismissForNow()
                    }
                },
                modifier = Modifier.testTag("opt_in_enable_push_button")
            ) {
                Text(
                    text = if (needsSystemSettings) {
                        tr("Buka Tetapan", "Open Settings")
                    } else {
                        tr("Aktifkan Sekarang", "Enable Now")
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissForNow,
                modifier = Modifier.testTag("opt_in_later_button")
            ) {
                Text(tr("Nanti", "Not Now"))
            }
        }
    )
}
