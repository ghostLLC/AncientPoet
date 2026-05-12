package com.ancientpoet.server.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit

fun Application.configureRateLimit() {
    install(RateLimit) {
        // Global rate limit: 60 requests per minute per client
        global {
            rateLimiter(limit = 60, refillPeriod = 60_000)
        }
    }
}
