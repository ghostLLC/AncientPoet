package com.ancientpoet.shared.data.api

enum class ApiErrorKind { NETWORK, UNAUTHORIZED, VALIDATION, SERVER, UNKNOWN }

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(
        val kind: ApiErrorKind,
        val message: String,
        val retryable: Boolean,
        val statusCode: Int? = null,
        val errorCode: String? = null
    ) : ApiResult<Nothing>
}
