package com.ancientpoet.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmsSendRequest(val phone: String)

@Serializable
data class SmsVerifyRequest(val phone: String, val code: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userId: Long,
)

@Serializable
data class RefreshRequest(val refreshToken: String)
