package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    var newsEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isNewsNotificationsEnabled(context))
    }
    var schedulesEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isScheduleNotificationsEnabled(context))
    }

    // Refresh permission & saved preferences whenever user returns from Android system settings
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = BulletinNotificationManager.hasNotificationPermission(context)
                newsEnabled = BulletinNotificationManager.isNewsNotificationsEnabled(context)
                schedulesEnabled = BulletinNotificationManager.isScheduleNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted && BulletinNotificationManager.hasNotificationPermission(context)
        if (hasPermission) {
            BulletinNotificationManager.ensureNotificationChannel(context)
            BulletinNotificationManager.syncTopicSubscriptions(context, newsEnabled, schedulesEnabled)
            onPreferencesUpdated(newsEnabled, schedulesEnabled)
        }
    }

    val anyTopicEnabled = newsEnabled || schedulesEnabled
    val isPushActive = hasPermission && anyTopicEnabled

    val statusBadgeText = when {
        !hasPermission -> tr("Belum Aktif", "Inactive")
        !anyTopicEnabled -> tr("Dinyahaktifkan", "Disabled")
        else -> tr("Aktif", "Active")
    }
    val statusBadgeBg = when {
        isPushActive -> MaterialTheme.colorScheme.primaryContainer
        !hasPermission -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusBadgeFg = when {
        isPushActive -> MaterialTheme.colorScheme.onPrimaryContainer
        !hasPermission -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusDescription = when {
        !hasPermission -> tr(
            "Aktifkan kebenaran notifikasi peranti agar tidak terlepas warta jemaat dan jadual ibadah terbaharu SIB Toburon.",
            "Enable device notification permission so you never miss the latest SIB Toburon news and worship schedules."
        )
        !anyTopicEnabled -> tr(
            "Semua notifikasi dinyahaktifkan. Hidupkan sekurang-kurangnya satu kategori di bawah untuk menerima makluman.",
            "All push notifications are disabled. Turn on at least one category below to receive alerts."
        )
        newsEnabled && schedulesEnabled -> tr(
            "Notifikasi aktif untuk Berita & Jadual Kegiatan.",
            "Notifications active for News & Event Schedules."
        )
        newsEnabled -> tr(
            "Notifikasi aktif untuk Berita & Warta sahaja.",
            "Notifications active for News & Bulletin only."
        )
        else -> tr(
            "Notifikasi aktif untuk Jadual Kegiatan sahaja.",
            "Notifications active for Event Schedules only."
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
                        text = tr("Notifikasi Tolak Masa Nyata (FCM)", "Real-Time Push Notifications (FCM)"),
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

            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enable_notif_permission_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("Aktifkan Kebenaran Notifikasi", "Enable Notification Permission"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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
                            text = tr("Berita & Warta Terkini", "Latest Church News"),
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
                        BulletinNotificationManager.syncTopicSubscriptions(context, newsEnabled, schedulesEnabled)
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
                        BulletinNotificationManager.syncTopicSubscriptions(context, newsEnabled, schedulesEnabled)
                        onPreferencesUpdated(newsEnabled, schedulesEnabled)
                    },
                    modifier = Modifier.testTag("notif_schedules_switch")
                )
            }

            if (isAdmin) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "Info Admin: Setiap Berita atau Jadual yang diterbitkan akan mencetuskan notifikasi masa nyata secara automatik kepada jemaat.",
                            "Admin Info: Every published News article or Schedule automatically triggers real-time push notifications to the congregation."
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
