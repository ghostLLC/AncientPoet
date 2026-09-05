package com.ancientpoet.server.plugin

import com.ancientpoet.server.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.request.path
import kotlin.time.Duration.Companion.seconds

internal fun trustedClientAddress(peer: String, forwarded: String?, trusted: List<String>): String = if (peer in trusted && forwarded != null && forwarded.matches(Regex("[0-9a-fA-F:.]{2,45}"))) forwarded else peer

private fun clientAddress(call: ApplicationCall, trusted: List<String>) = trustedClientAddress(call.request.local.remoteHost, call.request.headers["X-Real-IP"], trusted)

fun Application.configureRateLimit(config: AppConfig) {
    val verifier = JWT.require(Algorithm.HMAC256(config.jwtSecret)).withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience).withClaim("type", "access").build()
    install(RateLimit) {
        global {
            requestKey { call ->
                // Never trust an unverified claim or a caller-supplied forwarded IP.
                val token = call.request.headers["Authorization"]?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
                val user = token?.let { runCatching { verifier.verify(it).getClaim("userId").asLong() }.getOrNull() }
                if (user != null && user > 0) "user:" + user else "ip:" + clientAddress(call, config.trustedProxyHosts)
            }
            requestWeight { call, _ -> if (call.request.path() in setOf("/api/v1/health/live", "/api/v1/health/ready")) 0 else 1 }
            rateLimiter(limit = 240, refillPeriod = 60.seconds)
        }
        register(RateLimitName("auth")) {
            requestKey { clientAddress(it, config.trustedProxyHosts) }
            rateLimiter(limit = 20, refillPeriod = 60.seconds)
        }
    }
}
