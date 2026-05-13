package com.ancientpoet.server.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 60, refillPeriod = 60.seconds)
        }
    }
}
