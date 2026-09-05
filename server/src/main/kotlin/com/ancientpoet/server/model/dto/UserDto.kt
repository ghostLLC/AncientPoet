package com.ancientpoet.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: Long,
    val phone: String,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null
)

@Serializable
data class UserLocationResponse(
    val id: Long,
    val dynastyId: String,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val status: String
)

@Serializable
data class UpdateLocationRequest(
    val dynastyId: String? = null,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val status: String = "settled"
)
