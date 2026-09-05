package com.ancientpoet.server.plugin

import com.ancientpoet.server.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.time.Instant
import java.util.Date
import java.util.UUID

fun Application.configureAuthentication(
    config: AppConfig,
    sessionValidator: suspend (String, Long) -> Boolean = { _, _ -> true }
) {
    val verifier = JWT.require(Algorithm.HMAC256(config.jwtSecret))
        .withIssuer(config.jwtIssuer).withAudience(config.jwtAudience)
        .withClaim("type", "access").build()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "AncientPoet"
            verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asLong()
                val sessionId = credential.payload.getClaim("sid").asString()
                val expected = request.headers["X-Expected-User-Id"]
                if ((expected == null || expected.toLongOrNull() == userId) && userId != null && userId > 0 && sessionId != null && sessionValidator(sessionId, userId)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

fun generateAccessToken(config: AppConfig, userId: Long, sessionId: String = UUID.randomUUID().toString()): String = JWT.create().withIssuer(config.jwtIssuer).withAudience(config.jwtAudience)
    .withClaim("userId", userId).withClaim("sid", sessionId).withClaim("type", "access")
    .withIssuedAt(Date.from(Instant.now()))
    .withExpiresAt(Date(System.currentTimeMillis() + config.jwtAccessExpireMinutes * 60_000))
    .sign(Algorithm.HMAC256(config.jwtSecret))

fun generateRefreshToken(config: AppConfig, userId: Long, sessionId: String = UUID.randomUUID().toString()): String = JWT.create().withIssuer(config.jwtIssuer).withAudience(config.jwtAudience)
    .withClaim("userId", userId).withClaim("sid", sessionId).withClaim("type", "refresh")
    .withJWTId(UUID.randomUUID().toString()).withIssuedAt(Date.from(Instant.now()))
    .withExpiresAt(Date(System.currentTimeMillis() + config.jwtRefreshExpireDays * 86_400_000))
    .sign(Algorithm.HMAC256(config.jwtSecret))
