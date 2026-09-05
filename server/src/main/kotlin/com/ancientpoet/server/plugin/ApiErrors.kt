package com.ancientpoet.server.plugin

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

class ApiException(val status: HttpStatusCode, val code: String, message: String) : RuntimeException(message)

fun notFound(): Nothing = throw ApiException(HttpStatusCode.NotFound, "not_found", "内容不存在或无权访问")
fun invalid(message: String): Nothing = throw ApiException(HttpStatusCode.BadRequest, "invalid_request", message)
fun unavailable(message: String): Nothing = throw ApiException(HttpStatusCode.ServiceUnavailable, "service_unavailable", message)

@Serializable
data class ApiError(val error: String, val code: String, val requestId: String)
