package com.ancientpoet.server.route

import com.ancientpoet.server.model.domain.Conversation
import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.server.service.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.conversationRoute(
    service: ConversationService,
    poets: PoetRepository,
    locations: PoetLocationService,
    users: UserService
) {
    suspend fun details(conversation: Conversation): ConversationResponse {
        val year = conversation.storylineCurrentYear ?: locations.getDefaultYear(conversation.poetId)
        val poetLocation = locations.getPoetLocation(conversation.poetId, year)
        val userLocation = users.getLocation(conversation.userId, conversation.dynastyId)
        return conversation.dto().copy(
            userLocation = userLocation?.let { LocationBrief(it.locationName, it.lat, it.lng, it.status.name.lowercase()) },
            poetLocation = poetLocation?.let { LocationBrief(it.locationName, it.lat, it.lng, event = it.eventDescription) }
        )
    }
    authenticate("auth-jwt") {
        post("/conversations") {
            val req = call.receive<CreateConversationRequest>()
            val conversation = service.createConversation(call.userId(), req.poetId, req.dynastyId, req.backgroundSetting, req.startYear, req.mode)
            call.respond(HttpStatusCode.Created, details(conversation))
        }
        get("/conversations") {
            val archived = call.request.queryParameters["archived"]?.toBooleanStrict() ?: false
            call.respond(service.listUserConversations(call.userId(), archived).map { it.dto() })
        }
        get("/conversations/{id}") {
            call.respond(details(service.getConversation(call.parameters["id"]!!.toLong(), call.userId())))
        }
        post("/conversations/{id}/archive") {
            service.archive(call.parameters["id"]!!.toLong(), call.userId(), call.receive<ArchiveRequest>().archived)
            call.respond(HttpStatusCode.NoContent)
        }
        delete("/conversations/{id}") {
            service.deleteConversation(call.parameters["id"]!!.toLong(), call.userId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Conversation.dto() = ConversationResponse(
    id = id, poet = PoetBrief(poetName.orEmpty(), dynastyName, poetId, portraitUrl), mode = mode, dynastyId = dynastyId,
    backgroundSetting = backgroundSetting, createdAt = createdAt, currentYear = storylineCurrentYear, lastMessage = lastMessage.take(120),
    lastActivityAt = lastActivityAt, unreadCount = unreadCount, pendingCount = pendingCount, archived = archived,
    latestUnreadMessageId = latestUnreadMessageId
)

@Serializable private data class ArchiveRequest(val archived: Boolean = true)
