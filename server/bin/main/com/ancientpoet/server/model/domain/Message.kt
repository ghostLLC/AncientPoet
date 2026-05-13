package com.ancientpoet.server.model.domain

data class Message(
    val id: Long,
    val conversationId: Long,
    val senderType: String,
    val contentText: String?,
    val contentImageUrl: String?,
    val translation: String?,
    val isDelivered: Boolean,
    val scheduledDeliveryAt: String?,
    val deliveredAt: String?,
    val delaySeconds: Int?,
    val delayFactors: Map<String, String>?,
    val createdAt: String?,
)

data class EstimatedDelivery(
    val delaySeconds: Int,
    val deliverAt: String,
    val distanceKm: Double,
    val fromLocation: String,
    val toLocation: String,
    val factors: Map<String, String>,
)
