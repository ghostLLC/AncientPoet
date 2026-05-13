package com.ancientpoet.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AncientPoetApi(private val client: HttpClient, private val baseUrl: String) {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    fun setTokens(access: String, refresh: String) { accessToken = access; refreshToken = refresh }

    suspend fun post(path: String, body: Any): HttpResponse {
        return client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            accessToken?.let { header("Authorization", "Bearer $it") }
            setBody(body)
        }
    }

    suspend fun get(path: String): HttpResponse {
        return client.get("$baseUrl$path") {
            accessToken?.let { header("Authorization", "Bearer $it") }
        }
    }

    suspend fun put(path: String, body: Any): HttpResponse {
        return client.put("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            accessToken?.let { header("Authorization", "Bearer $it") }
            setBody(body)
        }
    }
}
