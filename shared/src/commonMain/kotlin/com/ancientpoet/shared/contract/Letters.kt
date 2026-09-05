package com.ancientpoet.shared.contract

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val contentText: String? = null,
    val contentImageUrl: String? = null,
    val clientMessageId: String? = null
)

@Serializable
data class MessageResponse(
    val messageId: Long,
    val status: String,
    val estimatedDelivery: EstimatedDeliveryDto? = null,
    val clientMessageId: String? = null
)

@Serializable
data class EstimatedDeliveryDto(
    val delaySeconds: Int,
    val deliverAt: String,
    val distanceKm: Double,
    val fromLocation: String,
    val toLocation: String,
    val factors: Map<String, String> = emptyMap(),
    val policyVersion: String = "1"
)

@Serializable
data class MessageItem(
    val id: Long,
    val conversationId: Long,
    val senderType: String,
    val contentText: String? = null,
    val contentImageUrl: String? = null,
    val translation: String? = null,
    val isDelivered: Boolean = true,
    val scheduledDeliveryAt: String? = null,
    val deliveredAt: String? = null,
    val createdAt: String? = null,
    val clientMessageId: String? = null,
    val readAt: String? = null
)

/** Unarrived letters never carry their body, translation or private attachment URL. */
@Serializable
data class PendingMessageItem(
    val id: Long,
    val senderType: String = "poet",
    val status: String,
    val scheduledDeliveryAt: String? = null,
    val delaySeconds: Int? = null,
    val estimatedSecondsRemaining: Long? = null,
    val canRetry: Boolean = false
)

@Serializable
data class CreateConversationRequest(
    val poetId: Long,
    val mode: String = "open",
    val backgroundSetting: String? = null,
    val dynastyId: String? = null,
    val startYear: Int? = null
)

@Serializable
data class ConversationResponse(
    val id: Long,
    val poet: PoetBrief = PoetBrief(""),
    val mode: String = "open",
    val dynastyId: String,
    val backgroundSetting: String? = null,
    val userLocation: LocationBrief? = null,
    val poetLocation: LocationBrief? = null,
    val createdAt: String? = null,
    val currentYear: Int? = null,
    val lastMessage: String = "",
    val lastActivityAt: String? = null,
    val unreadCount: Int = 0,
    val latestUnreadMessageId: Long? = null,
    val pendingCount: Int = 0,
    val archived: Boolean = false
)

@Serializable
data class PoetBrief(val name: String, val dynasty: String = "", val id: Long = 0, val portraitUrl: String? = null)

@Serializable
data class LocationBrief(
    val name: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "settled",
    val event: String? = null
)

@Serializable
data class PoemDetail(
    val id: Long,
    val poetId: Long,
    val poetName: String,
    val dynasty: String,
    val title: String,
    val content: String,
    val yearWritten: Int? = null,
    val context: String? = null,
    val translation: String? = null,
    val appreciation: String? = null,
    val tags: List<String> = emptyList(),
    val sourceUrl: String? = null
)

@Serializable
data class AppInfo(
    val development: Boolean = false,
    val demoAi: Boolean = false,
    val acceleratedDelivery: Boolean = false,
    val communityEnabled: Boolean = false,
    val uploadsEnabled: Boolean = false
)
