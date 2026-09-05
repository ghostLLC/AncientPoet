package com.ancientpoet.android.ui.screen.auth

import com.ancientpoet.android.TestSession
import com.ancientpoet.android.data.session.SessionController
import com.ancientpoet.shared.data.api.AncientPoetApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @Test fun failedSmsDoesNotEnableCodeSentStateAndRetryCanSucceed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        var attempts = 0
        val engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler {
                    if (it.url.encodedPath.endsWith("info")) {
                        respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    } else {
                        attempts++
                        if (attempts == 1) {
                            gate.await()
                            respond("""{"error":"短信暂不可用"}""", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/json"))
                        } else {
                            respond("""{"message":"ok"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                        }
                    }
                }
            }
        )
        val client = HttpClient(engine) {
            install(DefaultRequest) {
                url("http://localhost/api/v1/")
                contentType(ContentType.Application.Json)
            }
            install(ContentNegotiation) { json() }
        }
        try {
            val vm = LoginViewModel(AncientPoetApi(client), SessionController(TestSession(null)))
            vm.sendSms("13800000001")
            runCurrent()
            assertTrue(vm.state.value.isLoading)
            assertNull(vm.state.value.sentPhone)
            gate.complete(Unit)
            advanceUntilIdle()
            assertNull(vm.state.value.sentPhone)
            assertFalse(vm.state.value.isLoading)
            vm.sendSms("13800000001")
            runCurrent()
            // Await completion without sleeping through the entire resend countdown.
            while (vm.state.value.isLoading) {
                yield()
                runCurrent()
            }
            assertEquals("13800000001", vm.state.value.sentPhone)
            assertTrue(vm.state.value.cooldown > 0)
            advanceUntilIdle()
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }
}
