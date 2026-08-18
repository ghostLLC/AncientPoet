package com.ancientpoet.shared.auth

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
)

interface SessionStore {
    suspend fun load(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}
