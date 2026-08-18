package com.ancientpoet.android.ui.screen.conversation

import com.ancientpoet.shared.data.api.AncientPoetApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRetryTest {
    @Test
    fun retryReplaysFailedSendInsteadOfInitialLoad() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var sendCount = 0
            val sendBodies = mutableListOf<String>()
            var initialLoadCount = 0
            val engine = MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    when {
                        request.method == HttpMethod.Post && request.url.encodedPath.endsWith("/messages") -> {
                            sendCount++
                            (request.body as? TextContent)?.let { sendBodies += it.text }
                            if (sendCount == 1) {
                                respondError(HttpStatusCode.InternalServerError)
                            } else {
                                respondJson("""{"messageId":2,"status":"ok"}""")
                            }
                        }
                        request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/messages") -> {
                            initialLoadCount++
                            respondJson("[]")
                        }
                        request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/storyline/state") -> {
                            initialLoadCount++
                            respondJson("""{"currentYear":850,"poetAge":20,"locationName":"长安","activeEvent":null,"eventDescription":null,"eventType":"peace"}""")
                        }
                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            })
            val client = testClient(engine)
            val viewModel = ConversationViewModel(AncientPoetApi(client))

            viewModel.sendMessage(44, "hello")
            advanceUntilIdle()
            assertEquals(1, sendCount, engine.requestHistory.joinToString { "${it.method} ${it.url}" })
            assertEquals(0, initialLoadCount)
            assertEquals(
                "服务器暂时不可用",
                viewModel.state.value.actionError?.message,
                engine.requestHistory.joinToString { "${it.method} ${it.url}" },
            )

            viewModel.retry(44)
            advanceUntilIdle()

            assertEquals(2, sendCount)
            assertEquals(2, initialLoadCount)
            assertEquals(2, sendBodies.size)
            assertEquals(sendBodies[0], sendBodies[1])
            assertEquals(
                "hello",
                Json.parseToJsonElement(sendBodies[1]).jsonObject["contentText"]?.jsonPrimitive?.content,
            )
            assertNull(viewModel.state.value.actionError)
            client.close()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryReplaysFailedJumpWithTheSameYear() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var jumpCount = 0
            val jumpBodies = mutableListOf<String>()
            val engine = MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    if (request.method == HttpMethod.Post && request.url.encodedPath.endsWith("/storyline/jump")) {
                        jumpCount++
                        (request.body as? TextContent)?.let { jumpBodies += it.text }
                        if (jumpCount == 1) {
                            respondError(HttpStatusCode.InternalServerError)
                        } else {
                            respondJson("""{"currentYear":850,"poetAge":20,"locationName":"长安","activeEvent":null,"eventDescription":null,"eventType":"peace"}""")
                        }
                    } else {
                        respondError(HttpStatusCode.NotFound)
                    }
                }
            })
            val client = testClient(engine)
            val viewModel = ConversationViewModel(AncientPoetApi(client))

            viewModel.jumpToYear(44, 850)
            advanceUntilIdle()
            assertEquals(1, jumpCount)
            assertEquals(
                "服务器暂时不可用",
                viewModel.state.value.actionError?.message,
                engine.requestHistory.joinToString { "${it.method} ${it.url}" },
            )

            viewModel.retry(44)
            advanceUntilIdle()

            assertEquals(2, jumpCount)
            assertEquals(2, jumpBodies.size)
            assertEquals(jumpBodies[0], jumpBodies[1])
            assertEquals(
                "850",
                Json.parseToJsonElement(jumpBodies[1]).jsonObject["year"]?.jsonPrimitive?.content,
            )
            assertNull(viewModel.state.value.actionError)
            client.close()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun testClient(engine: MockEngine) = HttpClient(engine) {
        install(DefaultRequest) {
            url("http://localhost/api/v1/")
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun MockRequestHandleScope.respondJson(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}
