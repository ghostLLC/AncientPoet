package com.ancientpoet.server.push

class PushNotificationService(private val jpushClient: JPushClient) {

    suspend fun sendLetterArrival(userId: Long, locationName: String) {
        jpushClient.send(
            title = "一封来信",
            body = "一封来自${locationName}的信已到达",
            alias = "user_$userId",
        )
    }
}
