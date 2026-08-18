package com.ancientpoet.server.route

import com.ancientpoet.server.service.ReadinessChecker
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoute(readinessChecker: ReadinessChecker) {
    get("/health/live") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "alive"))
    }

    get("/health/ready") {
        val report = readinessChecker.check()
        val status = if (report.ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(status, report)
    }
}
