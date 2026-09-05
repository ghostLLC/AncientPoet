package com.ancientpoet.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.*

class AncientPoetApi(@PublishedApi internal val client: HttpClient) {
    suspend inline fun <reified T> get(path: String, expectedUserId: Long? = null): ApiResult<T> = execute { client.get(path) { expectedUserId?.let { header("X-Expected-User-Id", it) } } }

    suspend inline fun <reified T> post(
        path: String,
        expectedUserId: Long? = null,
        crossinline configureBody: HttpRequestBuilder.() -> Unit = {}
    ): ApiResult<T> = execute {
        client.post(path) {
            expectedUserId?.let { header("X-Expected-User-Id", it) }
            contentType(ContentType.Application.Json)
            configureBody()
        }
    }

    suspend inline fun <reified T> put(
        path: String,
        expectedUserId: Long? = null,
        crossinline configureBody: HttpRequestBuilder.() -> Unit = {}
    ): ApiResult<T> = execute {
        client.put(path) {
            expectedUserId?.let { header("X-Expected-User-Id", it) }
            contentType(ContentType.Application.Json)
            configureBody()
        }
    }

    suspend inline fun <reified T> delete(path: String, expectedUserId: Long? = null): ApiResult<T> = execute { client.delete(path) { expectedUserId?.let { header("X-Expected-User-Id", it) } } }

    @PublishedApi
    internal suspend inline fun <reified T> execute(
        crossinline request: suspend () -> HttpResponse
    ): ApiResult<T> = try {
        val response = request()
        if (!response.status.isSuccess()) {
            response.toFailure()
        } else {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(if (T::class == Unit::class) Unit as T else response.body<T>())
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        if (error.isNetworkFailure()) {
            ApiResult.Failure(
                kind = ApiErrorKind.NETWORK,
                message = "网络连接失败",
                retryable = true
            )
        } else {
            ApiResult.Failure(
                kind = ApiErrorKind.UNKNOWN,
                message = "请求失败，请稍后重试",
                retryable = false
            )
        }
    }

    @PublishedApi
    internal suspend fun HttpResponse.toFailure(): ApiResult.Failure {
        val code = status.value
        val payload = runCatching { Json.parseToJsonElement(bodyAsText()).jsonObject }.getOrNull()
        val detail = payload?.get("error")?.jsonPrimitive?.contentOrNull?.take(200)
        val errorCode = payload?.get("code")?.jsonPrimitive?.contentOrNull
        val fallback = when {
            code == HttpStatusCode.Unauthorized.value -> ApiResult.Failure(
                kind = ApiErrorKind.UNAUTHORIZED,
                message = "登录状态已失效",
                retryable = false,
                statusCode = code
            )

            code == HttpStatusCode.BadRequest.value || code == 409 || code == 422 -> ApiResult.Failure(
                kind = ApiErrorKind.VALIDATION,
                message = "请求参数有误",
                retryable = false,
                statusCode = code
            )

            code == 429 -> ApiResult.Failure(ApiErrorKind.SERVER, "请求过于频繁，请稍后重试", true, code)

            code in 500..599 -> ApiResult.Failure(
                kind = ApiErrorKind.SERVER,
                message = "服务器暂时不可用",
                retryable = true,
                statusCode = code
            )

            else -> ApiResult.Failure(
                kind = ApiErrorKind.UNKNOWN,
                message = "请求失败，请稍后重试",
                retryable = false,
                statusCode = code
            )
        }
        return fallback.copy(message = detail ?: fallback.message, errorCode = errorCode)
    }

    @PublishedApi
    internal fun Throwable.isNetworkFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException ||
                current is ConnectTimeoutException ||
                current is HttpRequestTimeoutException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
