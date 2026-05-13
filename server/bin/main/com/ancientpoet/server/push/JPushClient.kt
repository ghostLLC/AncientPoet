package com.ancientpoet.server.push

import com.ancientpoet.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

class JPushClient(private val config: AppConfig) {
    private val client = HttpClient()
    private val json = Json { encodeDefaults = false }
    private val pushUrl = "https://api.jpush.cn/v3/push"

    private fun authHeader(): String {
        val credential = "${config.jpushAppKey}:${config.jpushMasterSecret}"
        return "Basic " + Base64.getEncoder().encodeToString(credential.toByteArray())
    }

    suspend fun send(title: String, body: String, alias: String? = null) {
        val notification = JPushNotification(alert = body, android = JPushAndroid(title = title, channelId = "letter_arrival"))
        val audience = if (alias != null) mapOf("alias" to listOf(alias)) else mapOf("tag" to listOf("all"))
        val payload = JPushPayload(
            platform = listOf("android"),
            audience = audience,
            notification = notification,
            options = JPushOptions(apnsProduction = false),
        )

        try {
            val response = client.post(pushUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", authHeader())
                setBody(json.encodeToString(payload))
            }
            // Log response for debugging
            response.bodyAsText()
        } catch (e: Exception) {
            // Push delivery failure is non-critical; log and continue
            e.printStackTrace()
        }
    }
}

@Serializable data class JPushPayload(val platform: List<String>, val audience: Map<String, List<String>>, val notification: JPushNotification, val options: JPushOptions)
@Serializable data class JPushNotification(val alert: String, val android: JPushAndroid)
@Serializable data class JPushAndroid(val title: String, @kotlinx.serialization.SerialName("channel_id") val channelId: String)
@Serializable data class JPushOptions(@kotlinx.serialization.SerialName("apns_production") val apnsProduction: Boolean)
