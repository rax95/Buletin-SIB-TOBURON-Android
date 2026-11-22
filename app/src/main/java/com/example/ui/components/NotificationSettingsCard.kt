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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.notifications.BulletinNotificationManager
import com.example.ui.i18n.tr

@Composable
fun NotificationSettingsCard(
    isAdmin: Boolean,
    onPreferencesUpdated: (newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(BulletinNotificationManager.hasNotificationPermission(context))
    }
    var newsEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isNewsNotificationsEnabled(context))
    }
    var schedulesEnabled by remember {
        mutableStateOf(BulletinNotificationManager.isScheduleNotificationsEnabled(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            BulletinNotificationManager.ensureNotificationChannel(context)
            BulletinNotificationManager.syncTopicSubscriptions(context, newsEnabled, schedulesEnabled)
            onPreferencesUpdated(newsEnabled, schedulesEnabled)
        }
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
                        imageVector = if (hasPermission) Icons.Filled.NotificationsActive
                        else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (hasPermission) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = tr("Notifikasi Tolak Masa Nyata (FCM)", "Real-Time Push Notifications (FCM)"),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (hasPermission) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (hasPermission) tr("Aktif", "Active") else tr("Belum Aktif", "Inactive"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = if (hasPermission) {
                    tr("Kebenaran Notifikasi Aktif", "Notification Permission Active")
                } else {
                    tr(
                        "Aktifkan kebenaran notifikasi peranti agar tidak terlepas warta jemaat dan jadual ibadah terbaharu SIB Toburon.",
                        "Enable device notification permission so you never miss the latest SIB Toburon news and worship schedules."
                    )
                },
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("Berita & Warta Terkini", "Latest Church News"),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = tr("Topik FCM: ", "FCM Topic: ") + BulletinNotificationManager.TOPIC_NEWS,
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
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("Jadual Kegiatan Gereja", "Church Event Schedules"),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = tr("Topik FCM: ", "FCM Topic: ") + BulletinNotificationManager.TOPIC_SCHEDULES,
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
