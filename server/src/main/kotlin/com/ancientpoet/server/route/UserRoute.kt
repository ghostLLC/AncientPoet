package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.UserService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put

fun Route.userRoute(userService: UserService) {
    authenticate("auth-jwt") {
        get("/user/export") {
            call.respondText(userService.exportAccount(call.userId()), ContentType.Application.Json)
        }
        delete("/user/account") {
            userService.deleteAccount(call.userId())
            call.respond(HttpStatusCode.NoContent)
        }
        get("/user/profile") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val user = userService.getProfile(userId)
            if (user != null) {
                call.respond(
                    HttpStatusCode.OK,
                    UserProfileResponse(
                        id = user.id,
                        phone = user.phone,
                        nickname = user.nickname,
                        avatarUrl = user.avatarUrl,
                        bio = user.bio
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "用户不存在"))
            }
        }

        put("/user/profile") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val request = call.receive<UpdateProfileRequest>()
            userService.updateProfile(userId, request.nickname, request.avatarUrl, request.bio)
            call.respond(HttpStatusCode.OK, mapOf("message" to "更新成功"))
        }

        get("/user/location/{dynastyId}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val dynastyId = call.parameters["dynastyId"]!!
            val location = userService.getLocation(userId, dynastyId)
            if (location != null) {
                call.respond(
                    HttpStatusCode.OK,
                    UserLocationResponse(
                        id = location.id,
                        dynastyId = location.dynastyId,
                        locationName = location.locationName,
                        lat = location.lat,
                        lng = location.lng,
                        status = location.status.name.lowercase()
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "未设置位置"))
            }
        }

        put("/user/location/{dynastyId}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val dynastyId = call.parameters["dynastyId"]!!
            val request = call.receive<UpdateLocationRequest>()
            require(request.dynastyId == null || request.dynastyId == dynastyId)
            userService.updateLocation(userId, dynastyId, request.locationName, request.lat, request.lng, request.status)
            call.respond(HttpStatusCode.OK, mapOf("message" to "位置已更新"))
        }
    }
}
