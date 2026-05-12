package com.ancientpoet.server.service

import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.plugin.generateAccessToken
import com.ancientpoet.server.plugin.generateRefreshToken
import com.ancientpoet.server.repository.UserRepository

class AuthService(
    private val config: AppConfig,
    private val userRepository: UserRepository,
) {
    // In-memory store for SMS codes (Phase 1 MVP — use Redis in production)
    private val smsCodes = mutableMapOf<String, SmsCodeEntry>()

    fun sendSms(phone: String): Boolean {
        val code = (100000..999999).random().toString()
        smsCodes[phone] = SmsCodeEntry(code, System.currentTimeMillis() + 300_000)
        // In production: call SMS provider here
        return true
    }

    suspend fun verifySms(phone: String, code: String): TokenPair? {
        // Development bypass: "123456" works in dev mode only
        val isDev = System.getenv("KTOR_DEVELOPMENT")?.toBoolean() ?: false
        if (code == "123456" && isDev) {
            val user = userRepository.findByPhone(phone) ?: userRepository.create(phone)
            return generateTokens(user.id)
        }

        val entry = smsCodes[phone] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            smsCodes.remove(phone)
            return null
        }
        if (entry.code != code) return null

        smsCodes.remove(phone)
        val user = userRepository.findByPhone(phone) ?: userRepository.create(phone)
        return generateTokens(user.id)
    }

    fun verifyRefreshToken(token: String): Long? {
        return try {
            val algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(config.jwtSecret)
            val verifier = com.auth0.jwt.JWT.require(algorithm)
                .withIssuer(config.jwtIssuer)
                .withAudience(config.jwtAudience)
                .build()
            val jwt = verifier.verify(token)
            if (jwt.getClaim("type").asString() != "refresh") return null
            jwt.getClaim("userId").asLong()
        } catch (e: Exception) {
            null
        }
    }

    fun refreshAccessToken(userId: Long): String {
        return generateAccessToken(config, userId)
    }

    private fun generateTokens(userId: Long) = TokenPair(
        accessToken = generateAccessToken(config, userId),
        refreshToken = generateRefreshToken(config, userId),
        expiresIn = config.jwtAccessExpireMinutes * 60,
        userId = userId,
    )
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userId: Long,
)

private data class SmsCodeEntry(val code: String, val expiresAt: Long)
