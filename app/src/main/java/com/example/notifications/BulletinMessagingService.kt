package com.example.notifications

import android.util.Log
import com.example.R
import com.example.data.BulletinRepository
import com.example.ui.i18n.AppLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BulletinMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val appContext = applicationContext
        BulletinNotificationManager.saveFcmTokenLocally(appContext, token)

        val masterEnabled = BulletinNotificationManager.isMasterPushEnabled(appContext)
        val newsEnabled = BulletinNotificationManager.isNewsNotificationsEnabled(appContext)
        val schedulesEnabled = BulletinNotificationManager.isScheduleNotificationsEnabled(appContext)
        BulletinNotificationManager.syncTopicSubscriptions(
            context = appContext,
            newsEnabled = newsEnabled,
            schedulesEnabled = schedulesEnabled,
            masterEnabled = masterEnabled
        )

        if (Firebase.auth.currentUser != null && token.length in 10..512) {
            serviceScope.launch {
                val repo = BulletinRepository(appContext)
                repo.saveFcmToken(
                    token = token,
                    newsEnabled = masterEnabled && newsEnabled,
                    schedulesEnabled = masterEnabled && schedulesEnabled
                ).onFailure { err ->
                    Log.w(TAG, "Gagal menyegerakkan token FCM baharu: ${err.message}")
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        AppLanguageManager.init(applicationContext)
        val data = message.data
        val type = data["eventType"]
            ?: data["type"]
            ?: BulletinNotificationManager.TYPE_NEWS
        val itemId = data["targetId"] ?: data["itemId"] ?: ""
        val title = data["title"]
            ?: message.notification?.title
            ?: applicationContext.getString(R.string.app_name)
        val body = data["body"]
            ?: message.notification?.body
            ?: AppLanguageManager.tr(
                "Terdapat kemas kini terbaharu di Buletin SIB Toburon.",
                "There is a new update in Buletin SIB Toburon."
            )

        BulletinNotificationManager.showBulletinNotification(
            context = applicationContext,
            type = type,
            itemId = itemId,
            title = title,
            body = body
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BulletinFCMService"
    }
}
