package com.example.notifications

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.data.AppVersionConfig
import com.example.ui.i18n.AppLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object BulletinNotificationManager {
    private const val TAG = "BulletinFCM"
    private const val PREFS_NAME = "prefs_bulletin_notifications"
    private const val KEY_MASTER_ENABLED = "master_push_enabled"
    private const val KEY_USER_OPTED_OUT = "user_opted_out"
    private const val KEY_PERMISSION_REQUESTED_ONCE = "permission_requested_once"
    private const val KEY_NEWS_ENABLED = "news_enabled"
    private const val KEY_SCHEDULES_ENABLED = "schedules_enabled"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_NOTIFIED_VERSION_CODE = "last_notified_update_version_code"

    const val CHANNEL_ID = "bulletin_updates_channel"
    const val TOPIC_NEWS = "sib_toburon_news"
    const val TOPIC_SCHEDULES = "sib_toburon_schedules"
    const val TOPIC_ADMIN_MEMBERS = "sib_toburon_admin_members"

    const val EXTRA_NAV_TYPE = "extra_nav_type"
    const val EXTRA_NAV_ITEM_ID = "extra_nav_item_id"

    const val TYPE_NEWS = "NEWS"
    const val TYPE_SCHEDULE = "SCHEDULE"
    const val TYPE_MEMBER_REGISTERED = "MEMBER_REGISTERED"
    const val TYPE_APP_UPDATE = "APP_UPDATE"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isMasterPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER_ENABLED, true)

    fun hasUserOptedOut(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USER_OPTED_OUT, false)

    fun isNewsNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NEWS_ENABLED, true)

    fun isScheduleNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCHEDULES_ENABLED, true)

    fun wasPermissionRequestedBefore(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PERMISSION_REQUESTED_ONCE, false)

    fun markPermissionRequested(context: Context) {
        prefs(context).edit { putBoolean(KEY_PERMISSION_REQUESTED_ONCE, true) }
    }

    fun getSavedFcmToken(context: Context): String? =
        prefs(context).getString(KEY_FCM_TOKEN, null)

    fun saveFcmTokenLocally(context: Context, token: String) {
        prefs(context).edit { putString(KEY_FCM_TOKEN, token) }
    }

    fun savePreferences(
        context: Context,
        newsEnabled: Boolean,
        schedulesEnabled: Boolean,
        masterEnabled: Boolean = isMasterPushEnabled(context),
        optedOut: Boolean = hasUserOptedOut(context)
    ) {
        prefs(context).edit {
            putBoolean(KEY_MASTER_ENABLED, masterEnabled)
            putBoolean(KEY_USER_OPTED_OUT, optedOut)
            putBoolean(KEY_NEWS_ENABLED, newsEnabled)
            putBoolean(KEY_SCHEDULES_ENABLED, schedulesEnabled)
        }
    }

    fun optOutPushNotifications(context: Context) {
        savePreferences(
            context = context,
            newsEnabled = false,
            schedulesEnabled = false,
            masterEnabled = false,
            optedOut = true
        )
        syncTopicSubscriptions(
            context = context,
            newsEnabled = false,
            schedulesEnabled = false,
            isAdmin = false,
            masterEnabled = false,
            optedOut = true
        )
    }

    fun optInPushNotifications(
        context: Context,
        newsEnabled: Boolean = true,
        schedulesEnabled: Boolean = true,
        isAdmin: Boolean = false
    ) {
        val resolvedNews = if (!newsEnabled && !schedulesEnabled) true else newsEnabled
        val resolvedSchedules = if (!newsEnabled && !schedulesEnabled) true else schedulesEnabled
        ensureNotificationChannel(context)
        syncTopicSubscriptions(
            context = context,
            newsEnabled = resolvedNews,
            schedulesEnabled = resolvedSchedules,
            isAdmin = isAdmin,
            masterEnabled = true,
            optedOut = false
        )
    }

    fun buildTokenDocId(token: String): String {
        val hexHash = token.hashCode().toUInt().toString(16)
        return "token_$hexHash"
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun shouldShowOptInPopup(context: Context): Boolean {
        if (hasUserOptedOut(context)) return false
        if (!isMasterPushEnabled(context)) return false
        return !hasNotificationPermission(context)
    }

    fun shouldOpenSystemSettingsForPermission(context: Context): Boolean {
        if (hasNotificationPermission(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Below Android 13, notifications are only disabled if toggled off in OS settings
            return true
        }
        if (!wasPermissionRequestedBefore(context)) return false
        val activity = context.findActivity() ?: return false
        val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        )
        // If we already asked once and Android won't show rationale, user permanently denied or blocked in settings
        return !canShowRationale
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }.onFailure { err ->
                Log.w(TAG, "Gagal membuka tetapan notifikasi peranti: ${err.message}")
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun isAlertEnabledForType(context: Context, type: String): Boolean {
        if (!isMasterPushEnabled(context)) return false
        return when {
            type.equals(TYPE_MEMBER_REGISTERED, ignoreCase = true) -> true
            type.equals(TYPE_APP_UPDATE, ignoreCase = true) -> true
            type.equals(TYPE_SCHEDULE, ignoreCase = true) -> isScheduleNotificationsEnabled(context)
            else -> isNewsNotificationsEnabled(context)
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.notif_channel_desc)
            enableLights(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun syncTopicSubscriptions(
        context: Context,
        newsEnabled: Boolean,
        schedulesEnabled: Boolean,
        isAdmin: Boolean = false,
        masterEnabled: Boolean = isMasterPushEnabled(context),
        optedOut: Boolean = hasUserOptedOut(context)
    ) {
        savePreferences(
            context = context,
            newsEnabled = newsEnabled,
            schedulesEnabled = schedulesEnabled,
            masterEnabled = masterEnabled,
            optedOut = optedOut
        )
        val effectiveNews = masterEnabled && newsEnabled
        val effectiveSchedules = masterEnabled && schedulesEnabled
        val effectiveAdmin = masterEnabled && isAdmin

        try {
            val messaging = Firebase.messaging
            if (effectiveNews) {
                messaging.subscribeToTopic(TOPIC_NEWS)
                    .addOnFailureListener { Log.w(TAG, "Gagal melanggan topik $TOPIC_NEWS", it) }
            } else {
                messaging.unsubscribeFromTopic(TOPIC_NEWS)
                    .addOnFailureListener { Log.w(TAG, "Gagal berhenti melanggan $TOPIC_NEWS", it) }
            }

            if (effectiveSchedules) {
                messaging.subscribeToTopic(TOPIC_SCHEDULES)
                    .addOnFailureListener { Log.w(TAG, "Gagal melanggan topik $TOPIC_SCHEDULES", it) }
            } else {
                messaging.unsubscribeFromTopic(TOPIC_SCHEDULES)
                    .addOnFailureListener { Log.w(TAG, "Gagal berhenti melanggan $TOPIC_SCHEDULES", it) }
            }

            if (effectiveAdmin) {
                messaging.subscribeToTopic(TOPIC_ADMIN_MEMBERS)
                    .addOnFailureListener { Log.w(TAG, "Gagal melanggan topik $TOPIC_ADMIN_MEMBERS", it) }
            } else {
                messaging.unsubscribeFromTopic(TOPIC_ADMIN_MEMBERS)
                    .addOnFailureListener { Log.w(TAG, "Gagal berhenti melanggan $TOPIC_ADMIN_MEMBERS", it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM topic sync skipped: ${e.message}")
        }
    }

    suspend fun fetchAndSyncFcmToken(
        context: Context,
        onTokenReady: suspend (token: String, newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit
    ): String? {
        val masterEnabled = isMasterPushEnabled(context)
        val newsEnabled = masterEnabled && isNewsNotificationsEnabled(context)
        val schedulesEnabled = masterEnabled && isScheduleNotificationsEnabled(context)
        return try {
            @Suppress("DEPRECATION")
            val token = Firebase.messaging.token.await()
            if (token.length in 10..512) {
                saveFcmTokenLocally(context, token)
                onTokenReady(token, newsEnabled, schedulesEnabled)
                token
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengambil token FCM: ${e.message}")
            val cached = getSavedFcmToken(context)
            if (cached != null && cached.length in 10..512) {
                onTokenReady(cached, newsEnabled, schedulesEnabled)
            }
            cached
        }
    }

    fun showTestNotification(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext) || !isMasterPushEnabled(appContext)) return false
        showBulletinNotification(
            context = appContext,
            type = TYPE_NEWS,
            itemId = "",
            title = AppLanguageManager.tr(
                "Notifikasi SIB Toburon Aktif",
                "SIB Toburon Notifications Active"
            ),
            body = AppLanguageManager.tr(
                "Anda akan menerima makluman masa nyata apabila berita atau jadual baharu diterbitkan.",
                "You will receive real-time alerts whenever a new news post or church event is published."
            ),
            forceBypassCategoryCheck = true
        )
        return true
    }

    fun notifyAppUpdateAvailable(context: Context, config: AppVersionConfig): Boolean {
        val appContext = context.applicationContext
        if (!config.isUpdateAvailable(BuildConfig.VERSION_CODE)) return false
        val lastNotifiedCode = prefs(appContext).getInt(KEY_NOTIFIED_VERSION_CODE, 0)
        if (config.latestVersionCode <= lastNotifiedCode) return false

        prefs(appContext).edit {
            putInt(KEY_NOTIFIED_VERSION_CODE, config.latestVersionCode)
        }

        if (!hasNotificationPermission(appContext) || !isMasterPushEnabled(appContext)) {
            return false
        }

        val title = AppLanguageManager.tr(
            "Kemas Kini SIB Toburon v${config.latestVersionName} Tersedia",
            "SIB Toburon Update v${config.latestVersionName} Available"
        )
        val body = config.releaseNotes.takeIf { it.isNotBlank() } ?: AppLanguageManager.tr(
            "Versi baharu v${config.latestVersionName} (Kod ${config.latestVersionCode}) kini tersedia. Ketik untuk muat turun & kemas kini.",
            "New version v${config.latestVersionName} (Build ${config.latestVersionCode}) is now available. Tap to download & update."
        )

        showBulletinNotification(
            context = appContext,
            type = TYPE_APP_UPDATE,
            itemId = config.latestVersionCode.toString(),
            title = title,
            body = body,
            forceBypassCategoryCheck = true
        )
        return true
    }

    fun showBulletinNotification(
        context: Context,
        type: String,
        itemId: String,
        title: String,
        body: String,
        forceBypassCategoryCheck: Boolean = false
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        if (!isMasterPushEnabled(appContext)) return

        val normalizedType = when {
            type.equals(TYPE_MEMBER_REGISTERED, ignoreCase = true) -> TYPE_MEMBER_REGISTERED
            type.equals(TYPE_APP_UPDATE, ignoreCase = true) -> TYPE_APP_UPDATE
            type.equals(TYPE_SCHEDULE, ignoreCase = true) -> TYPE_SCHEDULE
            else -> TYPE_NEWS
        }

        if (!forceBypassCategoryCheck) {
            if (normalizedType == TYPE_NEWS && !isNewsNotificationsEnabled(appContext)) return
            if (normalizedType == TYPE_SCHEDULE && !isScheduleNotificationsEnabled(appContext)) return
        }
        if (!hasNotificationPermission(appContext)) return

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TYPE, normalizedType)
            if (itemId.isNotBlank()) {
                putExtra(EXTRA_NAV_ITEM_ID, itemId)
            }
        }

        val notificationId = ((normalizedType + ":" + itemId + ":" + title).hashCode() and 0x7FFFFFFF)
            .coerceAtLeast(1001)

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = ContextCompat.getColor(appContext, R.color.notification_accent)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bulletin)
            .setColor(accentColor)
            .setContentTitle(title.ifBlank { appContext.getString(R.string.app_name) })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Izin notifikasi belum diberikan saat menampilkan notifikasi", e)
        }
    }
}
