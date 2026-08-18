package com.ancientpoet.android.data.network

import com.ancientpoet.android.data.session.SessionController
import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.auth.SessionStore
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiErrorKind
import com.ancientpoet.shared.data.api.ApiResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AuthClientTest {
    @Test
    fun protectedRequestIncludesBearerAccessToken() = runTest {
        val store = InMemorySessionStore(AuthTokens("access-old", "refresh-old", 7))
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respondJson("""{"value":"ok"}""")
        }
        val controller = SessionController(store)
        val client = HttpClientFactory.create(controller, engine)

        val result = AncientPoetApi(client).get<TestPayload>("protected")

        assertIs<ApiResult.Success<TestPayload>>(result)
        assertEquals("Bearer access-old", requests.single().headers[HttpHeaders.Authorization])
        client.close()
    }

    @Test
    fun unauthorizedRequestRefreshesOnceAndRetriesWithNewAccessToken() = runTest {
        val store = InMemorySessionStore(AuthTokens("access-old", "refresh-current", 7))
        var protectedRequests = 0
        var refreshRequests = 0
        val protectedHeaders = mutableListOf<String?>()
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/auth/refresh") -> {
                    refreshRequests++
                    respondJson("""{"accessToken":"access-new"}""")
                }
                else -> {
                    protectedRequests++
                    protectedHeaders += request.headers[HttpHeaders.Authorization]
                    if (protectedRequests == 1) {
                        respondError(HttpStatusCode.Unauthorized)
                    } else {
                        respondJson("""{"value":"retried"}""")
                    }
                }
            }
        }
        val controller = SessionController(store)
        val client = HttpClientFactory.create(controller, engine)

        val result = AncientPoetApi(client).get<TestPayload>("protected")

        assertIs<ApiResult.Success<TestPayload>>(result)
        assertEquals(1, refreshRequests)
        assertEquals(2, protectedRequests)
        assertEquals(listOf<String?>("Bearer access-old", "Bearer access-new"), protectedHeaders)
        assertEquals(AuthTokens("access-new", "refresh-current", 7), store.value)
        client.close()
    }

    @Test
    fun failedRefreshIsMarkedAsRefreshRequestAndClearsSession() = runTest {
        val store = InMemorySessionStore(AuthTokens("access-old", "refresh-invalid", 7))
        var refreshRequests = 0
        var refreshAuthorization: String? = null
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                refreshRequests++
                refreshAuthorization = request.headers[HttpHeaders.Authorization]
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respondError(HttpStatusCode.Unauthorized)
            }
        }
        val controller = SessionController(store)
        val client = HttpClientFactory.create(controller, engine)

        val result = AncientPoetApi(client).get<TestPayload>("protected")

        val failure = assertIs<ApiResult.Failure>(result)
        assertEquals(ApiErrorKind.UNAUTHORIZED, failure.kind)
        assertEquals(1, refreshRequests)
        assertNull(refreshAuthorization)
        assertNull(store.value)
        client.close()
    }

    @Test
    fun logoutDuringRefreshDoesNotReinstallBearerTokens() = runTest {
        val store = InMemorySessionStore(AuthTokens("access-old", "refresh-current", 7))
        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        var protectedRequests = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                refreshStarted.complete(Unit)
                releaseRefresh.await()
                respondJson("""{"accessToken":"access-new"}""")
            } else {
                protectedRequests++
                respondError(HttpStatusCode.Unauthorized)
            }
        }
        val controller = SessionController(store)
        val client = HttpClientFactory.create(controller, engine)
        val call = async { AncientPoetApi(client).get<TestPayload>("protected") }

        refreshStarted.await()
        controller.logout()
        assertNull(store.value)
        releaseRefresh.complete(Unit)

        val failure = assertIs<ApiResult.Failure>(call.await())
        assertEquals(ApiErrorKind.UNAUTHORIZED, failure.kind)
        assertEquals(1, protectedRequests)
        assertNull(store.value)
        client.close()
    }

    @Test
    fun failedRefreshDoesNotClearSessionSavedWhileRefreshIsInFlight() = runTest {
        val oldTokens = AuthTokens("access-old", "refresh-old", 7)
        val newTokens = AuthTokens("access-new-session", "refresh-new-session", 8)
        val store = InMemorySessionStore(oldTokens)
        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        var protectedRequests = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                refreshStarted.complete(Unit)
                releaseRefresh.await()
                respondError(HttpStatusCode.Unauthorized)
            } else {
                protectedRequests++
                if (protectedRequests == 1) {
                    respondError(HttpStatusCode.Unauthorized)
                } else {
                    respondJson("""{"value":"retried-with-new-session"}""")
                }
            }
        }
        val controller = SessionController(store)
        val client = HttpClientFactory.create(controller, engine)
        val call = async { AncientPoetApi(client).get<TestPayload>("protected") }

        refreshStarted.await()
        controller.save(newTokens)
        releaseRefresh.complete(Unit)

        val success = assertIs<ApiResult.Success<TestPayload>>(call.await())
        assertEquals("retried-with-new-session", success.value.value)
        assertEquals(2, protectedRequests)
        assertEquals(newTokens, store.value)
        client.close()
    }

    @Test
    fun ioExceptionIsMappedToRetryableNetworkFailure() = runTest {
        val engine = MockEngine { throw IOException("offline") }
        val controller = SessionController(InMemorySessionStore(null))
        val client = HttpClientFactory.create(controller, engine)

        val failure = assertIs<ApiResult.Failure>(AncientPoetApi(client).get<TestPayload>("protected"))

        assertEquals(ApiErrorKind.NETWORK, failure.kind)
        assertEquals("网络连接失败", failure.message)
        assertEquals(true, failure.retryable)
        client.close()
    }

    @Test
    fun logoutClearsSessionAndBearerCache() = runTest {
        val store = InMemorySessionStore(AuthTokens("access-old", "refresh-old", 7))
        val headers = mutableListOf<String?>()
        val engine = MockEngine { request ->
            headers += request.headers[HttpHeaders.Authorization]
            respondJson("""{"value":"ok"}""")
        }
        val controller = SessionController(store)
        val client = HttpClientFactory.create(controller, engine)
        val api = AncientPoetApi(client)

        assertIs<ApiResult.Success<TestPayload>>(api.get<TestPayload>("protected"))
        controller.logout()
        assertNull(store.value)
        store.value = AuthTokens("access-after-logout", "refresh-after-logout", 7)
        assertIs<ApiResult.Success<TestPayload>>(api.get<TestPayload>("protected-again"))

        assertEquals(listOf<String?>("Bearer access-old", "Bearer access-after-logout"), headers)
        client.close()
    }

    private fun MockRequestHandleScope.respondJson(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    @Serializable
    data class TestPayload(val value: String)

    private class InMemorySessionStore(initial: AuthTokens?) : SessionStore {
        var value: AuthTokens? = initial

        override suspend fun load(): AuthTokens? = value

        override suspend fun save(tokens: AuthTokens) {
            value = tokens
        }

        override suspend fun clear() {
            value = null
        }
    }
}
