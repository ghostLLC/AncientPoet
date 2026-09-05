package com.ancientpoet.server.plugin

import com.ancientpoet.server.config.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCORS(config: AppConfig) {
    install(CORS) {
        config.corsAllowedHosts.forEach { allowHost(it, schemes = listOf("https")) }
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Expected-User-Id")
        listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete, HttpMethod.Options).forEach { allowMethod(it) }
    }
}
