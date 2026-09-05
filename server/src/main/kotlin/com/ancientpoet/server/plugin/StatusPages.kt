package com.ancientpoet.server.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Exception> { call, failure ->
            if (failure is CancellationException) throw failure
            val requestId = UUID.randomUUID().toString()
            val error = when (failure) {
                is ApiException -> failure

                is SecurityException -> ApiException(HttpStatusCode.NotFound, "not_found", "内容不存在或无权访问")

                is BadRequestException, is SerializationException, is IllegalArgumentException ->
                    ApiException(HttpStatusCode.BadRequest, "invalid_request", "请求参数不符合要求")

                else -> ApiException(HttpStatusCode.InternalServerError, "internal_error", "暂时无法完成，请稍后重试")
            }
            if (error.status.value >= 500) {
                // SQL errors may contain private letter text. Keep only a diagnostic identity.
                this@configureStatusPages.log.error("Request {} failed: {}", requestId, failure.javaClass.simpleName)
            }
            call.response.headers.append("X-Request-Id", requestId)
            call.respond(error.status, ApiError(error.message!!, error.code, requestId))
        }
    }
}
