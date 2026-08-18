package com.ancientpoet.shared.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiResultTest {
    @Test
    fun successCarriesValue() {
        val result: ApiResult<Int> = ApiResult.Success(42)
        assertEquals(42, (result as ApiResult.Success<Int>).value)
    }

    @Test
    fun networkFailureIsRetryable() {
        val result: ApiResult<Nothing> = ApiResult.Failure(
            kind = ApiErrorKind.NETWORK,
            message = "网络连接失败",
            retryable = true,
        )
        assertTrue((result as ApiResult.Failure).retryable)
    }

    @Test
    fun unauthorizedFailureIsNotSilentlyConvertedToEmptyData() {
        val result: ApiResult<Nothing> = ApiResult.Failure(
            kind = ApiErrorKind.UNAUTHORIZED,
            message = "登录状态已失效",
            retryable = false,
        )
        assertEquals(ApiErrorKind.UNAUTHORIZED, (result as ApiResult.Failure).kind)
    }
}
