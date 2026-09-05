package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.AuthService
import com.ancientpoet.server.service.TokenPair
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.authRoute(authService: AuthService) {
    rateLimit(RateLimitName("auth")) {
        post("/auth/sms/send") {
            authService.sendSms(call.receive<SmsSendRequest>().phone)
            call.respond(mapOf("message" to "验证码请求已受理"))
        }
        post("/auth/sms/verify") {
            val request = call.receive<SmsVerifyRequest>()
            val tokens = authService.verifySms(request.phone, request.code)
            if (tokens == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "验证码无效或已过期"))
            } else {
                call.respond(tokens.dto())
            }
        }
    }
    post("/auth/refresh") {
        val tokens = authService.refresh(call.receive<RefreshRequest>().refreshToken)
        if (tokens == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "登录已过期，请重新登录"))
        } else {
            call.respond(tokens.dto())
        }
    }
    authenticate("auth-jwt") {
        post("/auth/logout") {
            val jwt = call.principal<JWTPrincipal>()!!.payload
            authService.logout(jwt.getClaim("sid").asString(), jwt.getClaim("userId").asLong())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun TokenPair.dto() = TokenResponse(accessToken, refreshToken, expiresIn, userId)
