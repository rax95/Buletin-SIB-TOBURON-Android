package com.example.notifications

import android.util.Log
import com.example.R
import com.example.data.BulletinRepository
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

        val newsEnabled = BulletinNotificationManager.isNewsNotificationsEnabled(appContext)
        val schedulesEnabled = BulletinNotificationManager.isScheduleNotificationsEnabled(appContext)
        BulletinNotificationManager.syncTopicSubscriptions(appContext, newsEnabled, schedulesEnabled)

        if (Firebase.auth.currentUser != null && token.length in 10..512) {
            serviceScope.launch {
                val repo = BulletinRepository(appContext)
                repo.saveFcmToken(token, newsEnabled, schedulesEnabled)
                    .onFailure { err ->
                        Log.w(TAG, "Gagal menyinkronkan token FCM baru: ${err.message}")
                    }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
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
            ?: "Ada pembaruan terbaru di Buletin SIB Toburon."

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
