package com.ancientpoet.server.route

import com.ancientpoet.server.service.MovementService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.movementRoute() {
    val movementService: MovementService by inject()

    authenticate("auth-jwt") {
        post("/user/location/{dynastyId}/move") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val dynastyId = call.parameters["dynastyId"]!!
            val request = call.receive<MoveRequest>()
            val result = movementService.startMoving(userId, dynastyId, request.toName, request.toLat, request.toLng)
            call.respond(HttpStatusCode.OK, MovementResponse(
                status = result.status,
                currentName = result.currentName, currentLat = result.currentLat, currentLng = result.currentLng,
                movingToName = result.movingToName, movingToLat = result.movingToLat, movingToLng = result.movingToLng,
                remainingSeconds = result.remainingSeconds,
            ))
        }

        get("/user/location/{dynastyId}/status") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val dynastyId = call.parameters["dynastyId"]!!
            val result = movementService.getStatus(userId, dynastyId)
            call.respond(HttpStatusCode.OK, MovementResponse(
                status = result.status,
                currentName = result.currentName, currentLat = result.currentLat, currentLng = result.currentLng,
                movingToName = result.movingToName, movingToLat = result.movingToLat, movingToLng = result.movingToLng,
                remainingSeconds = result.remainingSeconds,
            ))
        }
    }
}

@Serializable data class MoveRequest(val toName: String, val toLat: Double, val toLng: Double)
@Serializable data class MovementResponse(
    val status: String,
    val currentName: String, val currentLat: Double, val currentLng: Double,
    val movingToName: String?, val movingToLat: Double?, val movingToLng: Double?,
    val remainingSeconds: Long,
)
