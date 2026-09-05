package com.ancientpoet.server.route

import com.ancientpoet.server.plugin.configureSerialization
import com.ancientpoet.server.service.ReadinessChecker
import com.ancientpoet.server.service.ReadinessReport
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun liveEndpointReturnsOk() = testApplication {
        application {
            configureSerialization()
            routing {
                route("/api/v1") {
                    healthRoute(FakeReadinessChecker(ReadinessReport(true, true, true)))
                }
            }
        }

        val response = client.get("/api/v1/health/live")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun readyEndpointReturnsServiceUnavailableWhenPostgresFails() = testApplication {
        application {
            configureSerialization()
            routing {
                route("/api/v1") {
                    healthRoute(FakeReadinessChecker(ReadinessReport(false, true, true)))
                }
            }
        }

        val response = client.get("/api/v1/health/ready")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    private class FakeReadinessChecker(
        private val report: ReadinessReport
    ) : ReadinessChecker {
        override suspend fun check(): ReadinessReport = report
    }
}
