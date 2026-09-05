package com.ancientpoet.android.data.session

import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.auth.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionController(
    private val delegate: SessionStore,
    clientProvider: (() -> HttpClient?)? = null
) : SessionStore {
    private val mutex = Mutex()
    private var client: HttpClient? = null
    private val lazyClient = clientProvider
    private var generation = 0L
    private val _sessionEvents = MutableStateFlow<Boolean?>(null)
    val sessionEvents: StateFlow<Boolean?> = _sessionEvents
    private val _identityRevision = MutableStateFlow(0L)
    val identityRevision: StateFlow<Long> = _identityRevision

    constructor(delegate: SessionStore, client: HttpClient) : this(delegate) {
        this.client = client
    }

    override suspend fun load(): AuthTokens? = mutex.withLock {
        delegate.load()
    }

    override suspend fun save(tokens: AuthTokens) {
        mutex.withLock {
            delegate.save(tokens)
            generation++
            clearBearerCache()
            _sessionEvents.value = true
            _identityRevision.value++
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            delegate.clear()
            generation++
            clearBearerCache()
            _sessionEvents.value = false
            _identityRevision.value++
        }
    }

    suspend fun logout() = clear()

    /** Makes persisted session state authoritative immediately before a protected request. */
    suspend fun currentSessionForRequest(): AuthTokens? = mutex.withLock {
        delegate.load().also { clearBearerCache() }
    }

    /** Captures the session identity used to conditionally apply one refresh response. */
    suspend fun captureSession(): SessionSnapshot? = mutex.withLock {
        delegate.load()?.let { SessionSnapshot(generation, it) }
    }

    /**
     * Applies a refresh only if the captured session is still current. A newer session is
     * returned so Ktor can retry with it; a logout returns null and stops the retry.
     */
    suspend fun applyRefresh(snapshot: SessionSnapshot, accessToken: String, refreshToken: String? = null): AuthTokens? = mutex.withLock {
        val current = delegate.load() ?: return@withLock null
        if (generation != snapshot.generation || current != snapshot.tokens) {
            return@withLock current
        }
        val updated = current.copy(accessToken = accessToken, refreshToken = refreshToken ?: current.refreshToken)
        delegate.save(updated)
        generation++
        clearBearerCache()
        _sessionEvents.value = true
        updated
    }

    /**
     * Clears the session after a failed refresh only if the refresh still owns the captured
     * session. If a save happened while the request was in flight, preserve and return it.
     */
    suspend fun clearAfterRefreshFailure(snapshot: SessionSnapshot): AuthTokens? = mutex.withLock {
        val current = delegate.load() ?: return@withLock null
        if (generation != snapshot.generation || current != snapshot.tokens) {
            return@withLock current
        }
        delegate.clear()
        generation++
        clearBearerCache()
        _sessionEvents.value = false
        _identityRevision.value++
        null
    }

    fun attachClient(client: HttpClient) {
        this.client = client
    }

    private fun clearBearerCache() {
        val configuredClient = client ?: lazyClient?.invoke() ?: return
        runCatching { configuredClient.authProvider<BearerAuthProvider>()?.clearToken() }
    }
}

data class SessionSnapshot(
    val generation: Long,
    val tokens: AuthTokens
)
