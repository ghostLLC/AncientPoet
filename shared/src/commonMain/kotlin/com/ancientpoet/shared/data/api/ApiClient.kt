package com.ancientpoet.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AncientPoetApi(private val client: HttpClient, private val baseUrl: String) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var accessToken: String? = null
    private var refreshToken: String? = null

    fun setTokens(access: String, refresh: String) { accessToken = access; refreshToken = refresh }

    suspend fun postJson<T>(path: String, body: Any): T {
        return client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            accessToken?.let { header("Authorization", "Bearer $it") }
            setBody(body)
        }.body()
    }

    suspend fun getJson<T>(path: String): T {
        return client.get("$baseUrl$path") {
            accessToken?.let { header("Authorization", "Bearer $it") }
        }.body()
    }

    suspend fun putJson<T>(path: String, body: Any): T {
        return client.put("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            accessToken?.let { header("Authorization", "Bearer $it") }
            setBody(body)
        }.body()
    }
}
