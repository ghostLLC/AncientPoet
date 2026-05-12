package com.ancientpoet.server.push

class FCMClient {
    // In Phase 1 MVP, FCM is stubbed.
    // Production implementation would use Firebase Admin SDK.
    // The Android app listens for push notifications with FCMService.

    suspend fun send(userId: Long, title: String, body: String, data: Map<String, String>) {
        // TODO: Integrate Firebase Admin SDK
        // val message = Message.builder()
        //     .setToken(deviceToken)
        //     .setNotification(Notification.builder().setTitle(title).setBody(body).build())
        //     .putAllData(data)
        //     .setAndroidConfig(...)
        //     .build()
        // FirebaseMessaging.getInstance().send(message)
    }
}
