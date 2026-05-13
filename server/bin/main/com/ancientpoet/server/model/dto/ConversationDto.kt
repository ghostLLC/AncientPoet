package com.ancientpoet.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateConversationRequest(
    val poetId: Long,
    val mode: String = "open",
    val backgroundSetting: String? = null,
    val dynastyId: String? = null,
)

@Serializable
data class ConversationResponse(
    val id: Long,
    val poet: PoetBrief = PoetBrief("", ""),
    val mode: String,
    val dynastyId: String,
    val backgroundSetting: String? = null,
    val userLocation: LocationBrief? = null,
    val poetLocation: LocationBrief? = null,
    val createdAt: String? = null,
)

@Serializable
data class PoetBrief(
    val name: String,
    val dynasty: String = "",
    val id: Long = 0,
    val portraitUrl: String? = null,
)

@Serializable
data class LocationBrief(
    val name: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "settled",
    val event: String? = null,
)
