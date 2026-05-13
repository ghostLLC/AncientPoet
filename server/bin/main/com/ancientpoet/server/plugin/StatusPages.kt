package com.ancientpoet.server.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    val logger = this@configureStatusPages.environment.log
    install(StatusPages) {
        exception<Exception> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Internal server error")))
        }
    }
}
