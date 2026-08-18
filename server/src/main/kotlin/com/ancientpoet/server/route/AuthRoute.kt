package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.authRoute(authService: AuthService) {

    post("/auth/sms/send") {
        val request = call.receive<SmsSendRequest>()
        authService.sendSms(request.phone)
        call.respond(HttpStatusCode.OK, mapOf("message" to "验证码已发送"))
    }

    post("/auth/sms/verify") {
        val request = call.receive<SmsVerifyRequest>()
        val tokens = authService.verifySms(request.phone, request.code)
        if (tokens != null) {
            call.respond(HttpStatusCode.OK, TokenResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresIn = tokens.expiresIn,
                userId = tokens.userId,
            ))
        } else {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "验证码无效或已过期"))
        }
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()
        val userId = authService.verifyRefreshToken(request.refreshToken)
        if (userId != null) {
            val newAccessToken = authService.refreshAccessToken(userId)
            call.respond(HttpStatusCode.OK, mapOf("accessToken" to newAccessToken))
        } else {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "无效的刷新令牌"))
        }
    }
}
