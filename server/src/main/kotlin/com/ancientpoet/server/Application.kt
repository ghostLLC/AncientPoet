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
import com.ancientpoet.server.route.communityRoute
import com.ancientpoet.server.route.conversationRoute
import com.ancientpoet.server.route.healthRoute
import com.ancientpoet.server.route.mapRoute
import com.ancientpoet.server.route.messageRoute
import com.ancientpoet.server.route.movementRoute
import com.ancientpoet.server.route.poemRoute
import com.ancientpoet.server.route.poetRoute
import com.ancientpoet.server.route.storylineRoute
import com.ancientpoet.server.route.uploadRoute
import com.ancientpoet.server.route.userRoute
import com.ancientpoet.server.scheduler.MessageDeliveryScheduler
import com.ancientpoet.server.service.ReadinessChecker
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.core.context.startKoin

fun main() {
    val appConfig = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = appConfig.port, host = appConfig.host) {
        module(appConfig)
    }.start(wait = true)
}

fun Application.module(appConfig: AppConfig) {
    DatabaseConfig.init(appConfig)
    RedisConfig.init(appConfig)

    val koinApplication = startKoin {
        modules(ServerModule.module(appConfig))
    }
    val koin = koinApplication.koin

    configureSerialization()
    configureAuthentication(appConfig)
    configureCORS()
    configureRateLimit()
    configureStatusPages()

    val readinessChecker: ReadinessChecker = koin.get()
    routing {
        route("/api/v1") {
            healthRoute(readinessChecker)
            authRoute(koin.get())
            userRoute(koin.get())
            poetRoute(koin.get(), koin.get())
            conversationRoute(koin.get(), koin.get(), koin.get(), koin.get())
            messageRoute(koin.get())
            storylineRoute(koin.get(), koin.get())
            movementRoute(koin.get())
            mapRoute()
            poemRoute(koin.get())
            uploadRoute(appConfig)
            communityRoute(koin.get())
        }
    }

    monitor.subscribe(ApplicationStarted) {
        val scheduler: MessageDeliveryScheduler = koin.get()
        scheduler.start()
    }

    monitor.subscribe(ApplicationStopPreparing) {
        DatabaseConfig.shutdown()
        koinApplication.close()
    }
}
