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
import kotlinx.coroutines.CancellationException
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
        val protectedPathPrefix = BuildConfig.API_BASE_URL
            .substringAfter("://", BuildConfig.API_BASE_URL)
            .substringAfter('/', "")
            .substringBefore('?')
            .trim('/')
            .let { path -> if (path.isEmpty()) "/" else "/$path/" }

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
                        val snapshot = sessionController.captureSession() ?: return@withLock null
                        oldTokens?.accessToken?.let { oldAccessToken ->
                            if (snapshot.tokens.accessToken != oldAccessToken) {
                                return@withLock snapshot.tokens.asBearerTokens()
                            }
                        }
                        try {
                            val response = client.post("auth/refresh") {
                                markAsRefreshTokenRequest()
                                setBody(RefreshRequest(snapshot.tokens.refreshToken))
                            }
                            if (!response.status.isSuccess()) {
                                return@withLock sessionController
                                    .clearAfterRefreshFailure(snapshot)
                                    ?.asBearerTokens()
                            }
                            val refreshed = response.body<RefreshResponse>()
                            sessionController.applyRefresh(
                                snapshot = snapshot,
                                accessToken = refreshed.accessToken,
                            )?.asBearerTokens()
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                            sessionController
                                .clearAfterRefreshFailure(snapshot)
                                ?.asBearerTokens()
                        }
                    }
                }
                sendWithoutRequest { request ->
                    val path = request.url.build().encodedPath
                    path.startsWith(protectedPathPrefix) &&
                        !path.startsWith("${protectedPathPrefix}auth/")
                }
            }
        }
    }

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(val accessToken: String)
}

private fun com.ancientpoet.shared.auth.AuthTokens.asBearerTokens() =
    BearerTokens(accessToken, refreshToken)
