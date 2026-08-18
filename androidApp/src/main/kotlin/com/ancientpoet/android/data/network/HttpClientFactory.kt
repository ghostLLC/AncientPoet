package com.ancientpoet.android.data.network

import com.ancientpoet.android.BuildConfig
import com.ancientpoet.android.data.session.SessionController
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(
        sessionController: SessionController,
        engine: HttpClientEngine? = null,
    ): HttpClient {
        val client = if (engine == null) {
            HttpClient(OkHttp) {
                configure(sessionController)
            }
        } else {
            HttpClient(engine) {
                configure(sessionController)
            }
        }
        sessionController.attachClient(client)
        return client
    }

    private fun io.ktor.client.HttpClientConfig<*>.configure(
        sessionController: SessionController,
    ) {
        val refreshMutex = Mutex()

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
        }
        install(DefaultRequest) {
            url(BuildConfig.API_BASE_URL.let { if (it.endsWith('/')) it else "$it/" })
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    sessionController.load()?.let { tokens ->
                        BearerTokens(tokens.accessToken, tokens.refreshToken)
                    }
                }
                refreshTokens {
                    refreshMutex.withLock {
                        val current = sessionController.load() ?: return@withLock null
                        oldTokens?.accessToken?.let { oldAccessToken ->
                            if (current.accessToken != oldAccessToken) {
                                return@withLock BearerTokens(current.accessToken, current.refreshToken)
                            }
                        }
                        try {
                            val response = client.post("auth/refresh") {
                                markAsRefreshTokenRequest()
                                setBody(RefreshRequest(current.refreshToken))
                            }
                            if (!response.status.isSuccess()) {
                                sessionController.clear()
                                return@withLock null
                            }
                            val refreshed = response.body<RefreshResponse>()
                            val updated = sessionController.updateAccessToken(
                                expectedAccessToken = current.accessToken,
                                accessToken = refreshed.accessToken,
                            ) ?: return@withLock null
                            BearerTokens(updated.accessToken, updated.refreshToken)
                        } catch (_: Throwable) {
                            sessionController.clear()
                            null
                        }
                    }
                }
                sendWithoutRequest { request ->
                    request.url.build().encodedPath.startsWith("/api/v1/") &&
                        !request.url.build().encodedPath.startsWith("/api/v1/auth/")
                }
            }
        }
    }

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(val accessToken: String)
}
