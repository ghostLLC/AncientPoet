package com.ancientpoet.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send token to server
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Notification is handled by Firebase SDK automatically
    }

    companion object {
        const val CHANNEL_LETTER = "letter_arrival"
        fun createChannels(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_LETTER, "书信到达",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "当诗人的回信到达时通知" }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
