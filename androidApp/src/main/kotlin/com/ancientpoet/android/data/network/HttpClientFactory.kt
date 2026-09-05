package com.ancientpoet.android.data.network

import com.ancientpoet.android.BuildConfig
import com.ancientpoet.android.data.session.SessionController
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(
        sessionController: SessionController,
        engine: HttpClientEngine? = null,
        refreshReturnHook: (suspend () -> Unit)? = null
    ): HttpClient {
        val client = if (engine == null) {
            HttpClient(OkHttp) {
                configure(sessionController, refreshReturnHook)
            }
        } else {
            HttpClient(engine) {
                configure(sessionController, refreshReturnHook)
            }
        }
        sessionController.attachClient(client)
        installSessionHeaderGuard(client, sessionController)
        return client
    }

    private fun installSessionHeaderGuard(
        client: HttpClient,
        sessionController: SessionController
    ) {
        val protectedPathPrefix = protectedPathPrefix()
        client.plugin(HttpSend).intercept { request ->
            require(sameOrigin(request.url.build())) { "API requests must stay on the configured origin" }
            val path = request.url.build().encodedPath
            if (sameOrigin(request.url.build()) && path.startsWith(protectedPathPrefix) &&
                (!path.startsWith("${protectedPathPrefix}auth/") || path.endsWith("auth/logout"))
            ) {
                val current = sessionController.currentSessionForRequest()
                val expected = request.headers["X-Expected-User-Id"]?.toLongOrNull()
                if (expected != null && expected != current?.userId) throw kotlinx.io.IOException("Account changed before request")
                request.headers.remove(HttpHeaders.Authorization)
                current?.let { tokens ->
                    request.headers.append(
                        HttpHeaders.Authorization,
                        "Bearer ${tokens.accessToken}"
                    )
                }
            } else {
                request.headers.remove(HttpHeaders.Authorization)
            }
            execute(request)
        }
    }

    private fun io.ktor.client.HttpClientConfig<*>.configure(
        sessionController: SessionController,
        refreshReturnHook: (suspend () -> Unit)?
    ) {
        val refreshMutex = Mutex()
        val protectedPathPrefix = protectedPathPrefix()

        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 25_000
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
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
                    if (!sameOrigin(response.request.url)) return@refreshTokens null
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
                                if (response.status.value != 401 && response.status.value != 403) throw kotlinx.io.IOException("Refresh temporarily unavailable")
                                return@withLock sessionController
                                    .clearAfterRefreshFailure(snapshot)
                                    ?.asBearerTokens()
                            }
                            val refreshed = response.body<RefreshResponse>()
                            val updated = sessionController.applyRefresh(
                                snapshot = snapshot,
                                accessToken = refreshed.accessToken,
                                refreshToken = refreshed.refreshToken
                            )
                            refreshReturnHook?.invoke()
                            updated?.asBearerTokens()
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                            // A timeout or outage is not evidence that the account was revoked.
                            throw kotlinx.io.IOException("Session could not be refreshed; retry later")
                        }
                    }
                }
                sendWithoutRequest { request ->
                    val path = request.url.build().encodedPath
                    sameOrigin(request.url.build()) && path.startsWith(protectedPathPrefix) &&
                        (!path.startsWith("${protectedPathPrefix}auth/") || path.endsWith("auth/logout"))
                }
            }
        }
    }

    private fun protectedPathPrefix(): String = BuildConfig.API_BASE_URL
        .substringAfter("://", BuildConfig.API_BASE_URL)
        .substringAfter('/', "")
        .substringBefore('?')
        .trim('/')
        .let { path -> if (path.isEmpty()) "/" else "/$path/" }

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    private data class RefreshResponse(val accessToken: String, val refreshToken: String)

    private fun sameOrigin(url: Url): Boolean {
        val base = Url(BuildConfig.API_BASE_URL)
        return url.protocol == base.protocol && url.host == base.host && url.port == base.port
    }
}

private fun com.ancientpoet.shared.auth.AuthTokens.asBearerTokens() = BearerTokens(accessToken, refreshToken)
