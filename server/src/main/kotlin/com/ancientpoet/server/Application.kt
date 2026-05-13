package com.ancientpoet.server

import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.config.DatabaseConfig
import com.ancientpoet.server.config.RedisConfig
import com.ancientpoet.server.di.ServerModule
import com.ancientpoet.server.plugin.configureAuthentication
import com.ancientpoet.server.plugin.configureCORS
import com.ancientpoet.server.plugin.configureRateLimit
import com.ancientpoet.server.plugin.configureSerialization
import com.ancientpoet.server.plugin.configureStatusPages
import com.ancientpoet.server.route.authRoute
import com.ancientpoet.server.route.conversationRoute
import com.ancientpoet.server.route.mapRoute
import com.ancientpoet.server.route.messageRoute
import com.ancientpoet.server.route.movementRoute
import com.ancientpoet.server.route.poemRoute
import com.ancientpoet.server.route.poetRoute
import com.ancientpoet.server.route.storylineRoute
import com.ancientpoet.server.route.userRoute
import com.ancientpoet.server.scheduler.MessageDeliveryScheduler
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.KoinApplicationStarted
import org.koin.ktor.plugin.KoinApplicationStopPreparing

fun main() {
    val appConfig = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = appConfig.port, host = appConfig.host) {
        module(appConfig)
    }.start(wait = true)
}

fun Application.module(appConfig: AppConfig) {
    DatabaseConfig.init(appConfig)
    RedisConfig.init(appConfig)

    install(Koin) {
        modules(ServerModule.module(appConfig))
    }

    configureSerialization()
    configureAuthentication(appConfig)
    configureCORS()
    configureRateLimit()
    configureStatusPages()

    routing {
        route("/api/v1") {
            authRoute()
            userRoute()
            poetRoute()
            conversationRoute()
            messageRoute()
            storylineRoute()
            movementRoute()
            mapRoute()
            poemRoute()
        }
    }

    monitor.subscribe(KoinApplicationStarted) {
        val scheduler: MessageDeliveryScheduler = getKoin().get()
        scheduler.start()
    }

    monitor.subscribe(KoinApplicationStopPreparing) {
        DatabaseConfig.shutdown()
    }
}
