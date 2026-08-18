package com.ancientpoet.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

class AncientPoetApi(@PublishedApi internal val client: HttpClient) {
    suspend inline fun <reified T> get(path: String): ApiResult<T> =
        execute { client.get(path) }

    suspend inline fun <reified T> post(path: String, body: Any? = null): ApiResult<T> =
        execute {
            client.post(path) {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
            }
        }

    suspend inline fun <reified T> put(path: String, body: Any? = null): ApiResult<T> =
        execute {
            client.put(path) {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
            }
        }

    suspend inline fun <reified T> delete(path: String): ApiResult<T> =
        execute { client.delete(path) }

    @PublishedApi
    internal suspend inline fun <reified T> execute(
        crossinline request: suspend () -> HttpResponse,
    ): ApiResult<T> {
        return try {
            val response = request()
            if (!response.status.isSuccess()) {
                response.toFailure()
            } else {
                ApiResult.Success(response.body<T>())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            if (error.isNetworkFailure()) {
                ApiResult.Failure(
                    kind = ApiErrorKind.NETWORK,
                    message = "网络连接失败",
                    retryable = true,
                )
            } else {
                ApiResult.Failure(
                    kind = ApiErrorKind.UNKNOWN,
                    message = "请求失败，请稍后重试",
                    retryable = false,
                )
            }
        }
    }

    @PublishedApi
    internal fun HttpResponse.toFailure(): ApiResult.Failure {
        val code = status.value
        return when {
            code == HttpStatusCode.Unauthorized.value -> ApiResult.Failure(
                kind = ApiErrorKind.UNAUTHORIZED,
                message = "登录状态已失效",
                retryable = false,
                statusCode = code,
            )
            code == HttpStatusCode.BadRequest.value || code == 422 -> ApiResult.Failure(
                kind = ApiErrorKind.VALIDATION,
                message = "请求参数有误",
                retryable = false,
                statusCode = code,
            )
            code in 500..599 -> ApiResult.Failure(
                kind = ApiErrorKind.SERVER,
                message = "服务器暂时不可用",
                retryable = true,
                statusCode = code,
            )
            else -> ApiResult.Failure(
                kind = ApiErrorKind.UNKNOWN,
                message = "请求失败，请稍后重试",
                retryable = false,
                statusCode = code,
            )
        }
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
