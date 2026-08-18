package com.ancientpoet.android.data.session

import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.auth.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionController(
    private val delegate: SessionStore,
    clientProvider: (() -> HttpClient?)? = null,
) : SessionStore {
    private val mutex = Mutex()
    private var client: HttpClient? = null
    private val lazyClient = clientProvider
    private val _sessionEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val sessionEvents: SharedFlow<Boolean> = _sessionEvents.asSharedFlow()

    constructor(delegate: SessionStore, client: HttpClient) : this(delegate) {
        this.client = client
    }

    override suspend fun load(): AuthTokens? = mutex.withLock {
        delegate.load()
    }

    override suspend fun save(tokens: AuthTokens) {
        mutex.withLock {
            delegate.save(tokens)
            clearBearerCache()
            _sessionEvents.tryEmit(true)
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            delegate.clear()
            clearBearerCache()
            _sessionEvents.tryEmit(false)
        }
    }

    suspend fun logout() = clear()

    /** Atomically applies a refresh only when the session has not changed or been logged out. */
    suspend fun updateAccessToken(expectedAccessToken: String, accessToken: String): AuthTokens? = mutex.withLock {
        val current = delegate.load() ?: return@withLock null
        if (current.accessToken != expectedAccessToken) {
            return@withLock current
        }
        val updated = current.copy(accessToken = accessToken)
        delegate.save(updated)
        clearBearerCache()
        _sessionEvents.tryEmit(true)
        updated
    }

    fun attachClient(client: HttpClient) {
        this.client = client
    }

    private fun clearBearerCache() {
        val configuredClient = client ?: lazyClient?.invoke() ?: return
        runCatching { configuredClient.authProvider<BearerAuthProvider>()?.clearToken() }
    }
}
