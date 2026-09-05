package com.ancientpoet.server

import com.ancientpoet.server.ai.DeepSeekClient
import com.ancientpoet.server.config.*
import com.ancientpoet.server.di.ServerModule
import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.push.JPushClient
import com.ancientpoet.server.route.*
import com.ancientpoet.server.scheduler.MessageDeliveryScheduler
import com.ancientpoet.server.service.*
import com.ancientpoet.shared.contract.AppInfo
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.core.context.startKoin

fun main() {
    val config = AppConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = config.host) { module(config) }.start(wait = true)
}
fun Application.module(config: AppConfig) {
    config.validate()
    DatabaseConfig.init(config)
    val application = startKoin { modules(ServerModule.module(config)) }
    val koin = application.koin
    val auth = koin.get<AuthService>()
    val worker = koin.get<MessageDeliveryScheduler>()
    configureSerialization()
    configureAuthentication(config, auth::isSessionActive)
    configureCORS(config)
    configureRateLimit(config)
    configureStatusPages()
    routing {
        route("/api/v1") {
            get("/info") { call.respond(AppInfo(config.development, config.aiMode == "demo", config.demoDeliverySeconds != null, false, false)) }
            healthRoute(koin.get())
            authRoute(auth)
            userRoute(koin.get())
            poetRoute(koin.get(), koin.get())
            conversationRoute(koin.get(), koin.get(), koin.get(), koin.get())
            messageRoute(koin.get())
            storylineRoute(koin.get(), koin.get())
            movementRoute(koin.get())
            mapRoute(koin.get())
            poemRoute(koin.get())
            // Community and media are intentionally unavailable in this text-letter release.
        }
    }
    monitor.subscribe(ApplicationStarted) { worker.start() }
    monitor.subscribe(ApplicationStopPreparing) {
        worker.close()
        koin.get<DeepSeekClient>().close()
        koin.get<JPushClient>().close()
        application.close()
        DatabaseConfig.shutdown()
    }
}
