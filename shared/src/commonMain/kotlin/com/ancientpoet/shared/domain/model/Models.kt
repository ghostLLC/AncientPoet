package com.ancientpoet.shared.domain.model

data class User(val id: Long, val phone: String, val nickname: String?, val avatarUrl: String?, val bio: String?)

data class Poet(
    val id: Long, val name: String, val courtesyName: String?, val artName: String?,
    val dynastyId: String, val dynastyName: String?, val birthYear: Int, val deathYear: Int,
    val personalityProfile: PersonalityProfile, val writingStyle: String,
    val biographySummary: String?, val portraitUrl: String?, val isFree: Boolean,
)

data class PersonalityProfile(val traits: List<String>, val mbti: String?, val speakingStyle: String?)

data class Conversation(
    val id: Long, val userId: Long, val poetId: Long, val poetName: String?,
    val mode: String, val dynastyId: String, val backgroundSetting: String?,
    val createdAt: String?,
)

data class Message(
    val id: Long, val conversationId: Long, val senderType: String,
    val contentText: String?, val translation: String?, val isDelivered: Boolean,
    val scheduledDeliveryAt: String?, val deliveredAt: String?, val createdAt: String?,
)

data class Location(val name: String, val lat: Double, val lng: Double, val status: String = "settled")

data class EstimatedDelivery(
    val delaySeconds: Int, val deliverAt: String, val distanceKm: Double,
    val fromLocation: String, val toLocation: String, val factors: Map<String, String>,
)
