package com.ancientpoet.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val contentText: String? = null,
    val contentImageUrl: String? = null,
)

@Serializable
data class MessageResponse(
    val messageId: Long,
    val status: String,
    val estimatedDelivery: EstimatedDeliveryDto? = null,
)

@Serializable
data class EstimatedDeliveryDto(
    val delaySeconds: Int,
    val deliverAt: String,
    val distanceKm: Double,
    val fromLocation: String,
    val toLocation: String,
    val factors: Map<String, String>,
)

@Serializable
data class MessageItem(
    val id: Long,
    val conversationId: Long,
    val senderType: String,
    val contentText: String? = null,
    val translation: String? = null,
    val isDelivered: Boolean,
    val scheduledDeliveryAt: String? = null,
    val deliveredAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class PendingMessageItem(
    val id: Long,
    val senderType: String,
    val contentText: String? = null,
    val scheduledDeliveryAt: String? = null,
    val delaySeconds: Int? = null,
    val estimatedSecondsRemaining: Long? = null,
)
