package com.ancientpoet.server.push

import com.ancientpoet.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JPushClient(private val config: AppConfig) : AutoCloseable {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
        }
    }
    suspend fun send(title: String, body: String, alias: String? = null) {
        if (config.jpushAppKey.isBlank() || config.jpushMasterSecret.isBlank()) return
        require(!alias.isNullOrBlank())
        val payload = JPushPayload(
            listOf("android"),
            mapOf("alias" to listOf(alias)),
            JPushNotification(body, JPushAndroid(title, "letter_arrival"))
        )
        val credential = config.jpushAppKey + ":" + config.jpushMasterSecret
        val response = client.post("https://api.jpush.cn/v3/push") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Basic " + Base64.getEncoder().encodeToString(credential.toByteArray()))
            setBody(Json.encodeToString(payload))
        }
        if (response.status.value !in 200..299) error("PushHttp" + response.status.value)
    }
    override fun close() = client.close()
}

@Serializable data class JPushPayload(val platform: List<String>, val audience: Map<String, List<String>>, val notification: JPushNotification)

@Serializable data class JPushNotification(val alert: String, val android: JPushAndroid)

@Serializable data class JPushAndroid(val title: String, @kotlinx.serialization.SerialName("channel_id") val channelId: String)
