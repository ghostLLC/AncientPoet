package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.server.service.ConversationService
import com.ancientpoet.server.service.PoetLocationService
import com.ancientpoet.server.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.conversationRoute(
    conversationService: ConversationService,
    poetRepository: PoetRepository,
    poetLocationService: PoetLocationService,
    userService: UserService,
) {

    authenticate("auth-jwt") {
        post("/conversations") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val request = call.receive<CreateConversationRequest>()
            val conv = conversationService.createConversation(
                userId, request.poetId, request.dynastyId, request.backgroundSetting,
            )
            val poet = poetRepository.findById(conv.poetId)
            val year = poetLocationService.getDefaultYear(conv.poetId)
            val poetLoc = poetLocationService.getPoetLocation(conv.poetId, year)
            val userLoc = userService.getLocation(userId, conv.dynastyId)

            call.respond(HttpStatusCode.Created, ConversationResponse(
                id = conv.id,
                poet = PoetBrief(
                    id = poet?.id ?: 0, name = poet?.name ?: "",
                    dynasty = poet?.dynastyName ?: "",
                    portraitUrl = poet?.portraitUrl,
                ),
                mode = conv.mode,
                dynastyId = conv.dynastyId,
                backgroundSetting = conv.backgroundSetting,
                userLocation = userLoc?.let {
                    LocationBrief(name = it.locationName, lat = it.lat, lng = it.lng, status = it.status.name.lowercase())
                },
                poetLocation = poetLoc?.let {
                    LocationBrief(name = it.locationName, lat = it.lat, lng = it.lng, event = it.eventDescription)
                },
            ))
        }

        get("/conversations") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val conversations = conversationService.listUserConversations(userId)
            call.respond(HttpStatusCode.OK, conversations.map { conv ->
                val poet = poetRepository.findById(conv.poetId)
                ConversationResponse(
                    id = conv.id,
                    poet = PoetBrief(id = poet?.id ?: 0, name = poet?.name ?: "", dynasty = poet?.dynastyName ?: ""),
                    mode = conv.mode, dynastyId = conv.dynastyId,
                    backgroundSetting = conv.backgroundSetting, createdAt = conv.createdAt,
                )
            })
        }

        get("/conversations/{id}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val convId = call.parameters["id"]!!.toLong()
            val conv = conversationService.getConversation(convId)
            if (conv != null && conv.userId == userId) {
                val poet = poetRepository.findById(conv.poetId)
                call.respond(HttpStatusCode.OK, ConversationResponse(
                    id = conv.id,
                    poet = PoetBrief(id = poet?.id ?: 0, name = poet?.name ?: "", dynasty = poet?.dynastyName ?: ""),
                    mode = conv.mode, dynastyId = conv.dynastyId,
                    backgroundSetting = conv.backgroundSetting, createdAt = conv.createdAt,
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "会话不存在"))
            }
        }

        delete("/conversations/{id}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val convId = call.parameters["id"]!!.toLong()
            conversationService.deleteConversation(convId, userId)
            call.respond(HttpStatusCode.OK, mapOf("message" to "已删除"))
        }
    }
}
