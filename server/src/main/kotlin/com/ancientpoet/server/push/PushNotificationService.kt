package com.ancientpoet.server.push

class PushNotificationService(private val fcmClient: FCMClient) {

    suspend fun sendLetterArrival(userId: Long, locationName: String) {
        fcmClient.send(
            userId = userId,
            title = "📜 一封来信",
            body = "一封来自${locationName}的信已到达",
            data = mapOf("type" to "letter_arrival"),
        )
    }
}
