package com.ancientpoet.server.plugin

import com.ancientpoet.server.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.Date

fun Application.configureAuthentication(config: AppConfig) {
    val jwtAlgorithm = Algorithm.HMAC256(config.jwtSecret)
    val jwtVerifier = JWT.require(jwtAlgorithm)
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "AncientPoet"
            verifier(jwtVerifier)
            validate { credential ->
                if (credential.payload.getClaim("userId").isNull) null
                else JWTPrincipal(credential.payload)
            }
        }
    }
}

fun generateAccessToken(config: AppConfig, userId: Long): String {
    val algorithm = Algorithm.HMAC256(config.jwtSecret)
    return JWT.create()
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + config.jwtAccessExpireMinutes * 60_000))
        .sign(algorithm)
}

fun generateRefreshToken(config: AppConfig, userId: Long): String {
    val algorithm = Algorithm.HMAC256(config.jwtSecret)
    return JWT.create()
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .withClaim("userId", userId)
        .withClaim("type", "refresh")
        .withExpiresAt(Date(System.currentTimeMillis() + config.jwtRefreshExpireDays * 86_400_000))
        .sign(algorithm)
}
