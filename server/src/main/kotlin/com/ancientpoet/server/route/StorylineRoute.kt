package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.CreateConversationRequest
import com.ancientpoet.server.service.ConversationService
import com.ancientpoet.server.service.StorylineService
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

fun Route.storylineRoute() {
    val storylineService: StorylineService by inject()
    val conversationService: ConversationService by inject()

    authenticate("auth-jwt") {
        get("/conversations/{id}/storyline/state") {
            val convId = call.parameters["id"]!!.toLong()
            val state = storylineService.getState(convId)
            call.respond(HttpStatusCode.OK, StorylineStateResponse(
                currentYear = state.currentYear,
                poetAge = state.poetAge,
                locationName = state.locationName,
                lat = state.lat, lng = state.lng,
                activeEvent = state.activeEvent,
                eventDescription = state.eventDescription,
                eventType = state.eventType,
                delayMultiplier = state.delayMultiplier,
            ))
        }

        post("/conversations/{id}/storyline/jump") {
            val convId = call.parameters["id"]!!.toLong()
            val request = call.receive<JumpYearRequest>()
            val state = storylineService.jumpToYear(convId, request.year)
            call.respond(HttpStatusCode.OK, StorylineStateResponse(
                currentYear = state.currentYear,
                poetAge = state.poetAge,
                locationName = state.locationName,
                lat = state.lat, lng = state.lng,
                activeEvent = state.activeEvent,
                eventDescription = state.eventDescription,
                eventType = state.eventType,
                delayMultiplier = state.delayMultiplier,
            ))
        }
    }
}

@Serializable data class StorylineStateResponse(
    val currentYear: Int, val poetAge: Int,
    val locationName: String, val lat: Double, val lng: Double,
    val activeEvent: String?, val eventDescription: String?,
    val eventType: String, val delayMultiplier: Double,
)
@Serializable data class JumpYearRequest(val year: Int)
