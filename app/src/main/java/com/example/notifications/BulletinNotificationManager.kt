package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object BulletinNotificationManager {
    private const val TAG = "BulletinFCM"
    private const val PREFS_NAME = "prefs_bulletin_notifications"
    private const val KEY_NEWS_ENABLED = "news_enabled"
    private const val KEY_SCHEDULES_ENABLED = "schedules_enabled"
    private const val KEY_FCM_TOKEN = "fcm_token"

    const val CHANNEL_ID = "bulletin_updates_channel"
    const val TOPIC_NEWS = "sib_toburon_news"
    const val TOPIC_SCHEDULES = "sib_toburon_schedules"

    const val EXTRA_NAV_TYPE = "extra_nav_type"
    const val EXTRA_NAV_ITEM_ID = "extra_nav_item_id"

    const val TYPE_NEWS = "NEWS"
    const val TYPE_SCHEDULE = "SCHEDULE"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isNewsNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NEWS_ENABLED, true)

    fun isScheduleNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCHEDULES_ENABLED, true)

    fun getSavedFcmToken(context: Context): String? =
        prefs(context).getString(KEY_FCM_TOKEN, null)

    fun saveFcmTokenLocally(context: Context, token: String) {
        prefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun savePreferences(context: Context, newsEnabled: Boolean, schedulesEnabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_NEWS_ENABLED, newsEnabled)
            .putBoolean(KEY_SCHEDULES_ENABLED, schedulesEnabled)
            .apply()
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

    fun isAlertEnabledForType(context: Context, type: String): Boolean {
        val normalizedType = if (type.equals(TYPE_SCHEDULE, ignoreCase = true)) {
            TYPE_SCHEDULE
        } else {
            TYPE_NEWS
        }
        return if (normalizedType == TYPE_SCHEDULE) {
            isScheduleNotificationsEnabled(context)
        } else {
            isNewsNotificationsEnabled(context)
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
        schedulesEnabled: Boolean
    ) {
        savePreferences(context, newsEnabled, schedulesEnabled)
        try {
            val messaging = Firebase.messaging
            if (newsEnabled) {
                messaging.subscribeToTopic(TOPIC_NEWS)
                    .addOnFailureListener { Log.w(TAG, "Gagal berlangganan topik $TOPIC_NEWS", it) }
            } else {
                messaging.unsubscribeFromTopic(TOPIC_NEWS)
                    .addOnFailureListener { Log.w(TAG, "Gagal berhenti berlangganan $TOPIC_NEWS", it) }
            }

            if (schedulesEnabled) {
                messaging.subscribeToTopic(TOPIC_SCHEDULES)
                    .addOnFailureListener { Log.w(TAG, "Gagal berlangganan topik $TOPIC_SCHEDULES", it) }
            } else {
                messaging.unsubscribeFromTopic(TOPIC_SCHEDULES)
                    .addOnFailureListener { Log.w(TAG, "Gagal berhenti berlangganan $TOPIC_SCHEDULES", it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM topic sync skipped: ${e.message}")
        }
    }

    suspend fun fetchAndSyncFcmToken(
        context: Context,
        onTokenReady: suspend (token: String, newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit
    ): String? {
        val newsEnabled = isNewsNotificationsEnabled(context)
        val schedulesEnabled = isScheduleNotificationsEnabled(context)
        return try {
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

    fun showBulletinNotification(
        context: Context,
        type: String,
        itemId: String,
        title: String,
        body: String
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        val normalizedType = if (type.equals(TYPE_SCHEDULE, ignoreCase = true)) {
            TYPE_SCHEDULE
        } else {
            TYPE_NEWS
        }

        if (normalizedType == TYPE_NEWS && !isNewsNotificationsEnabled(appContext)) return
        if (normalizedType == TYPE_SCHEDULE && !isScheduleNotificationsEnabled(appContext)) return
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
