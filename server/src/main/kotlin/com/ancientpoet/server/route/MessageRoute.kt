package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.MessageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.messageRoute(service: MessageService) {
    authenticate("auth-jwt") {
        post("/conversations/{id}/messages") {
            val request = call.receive<SendMessageRequest>()
            call.respond(
                service.sendMessage(
                    call.parameters["id"]!!.toLong(),
                    call.userId(),
                    request.contentText,
                    request.contentImageUrl,
                    request.clientMessageId
                )
            )
        }
        get("/conversations/{id}/delivery-preview") {
            call.respond(service.preview(call.parameters["id"]!!.toLong(), call.userId()))
        }
        get("/conversations/{id}/messages") {
            val query = call.request.queryParameters
            val messages = service.getMessages(
                call.parameters["id"]!!.toLong(),
                call.userId(),
                query["limit"]?.toInt() ?: 50,
                query["beforeId"]?.toLong(),
                query["afterId"]?.toLong()
            )
            call.respond(
                messages.map { msg ->
                    MessageItem(
                        msg.id, msg.conversationId, msg.senderType, msg.contentText, msg.contentImageUrl, msg.translation,
                        msg.isDelivered, msg.scheduledDeliveryAt, msg.deliveredAt, msg.createdAt, msg.clientMessageId, msg.readAt
                    )
                }
            )
        }
        get("/conversations/{id}/pending") {
            call.respond(service.getPending(call.parameters["id"]!!.toLong(), call.userId()))
        }
        post("/conversations/{id}/read") {
            service.markRead(call.parameters["id"]!!.toLong(), call.userId(), call.receive<ReadRequest>().throughId)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/messages/{id}/retry") {
            val accepted = service.retry(call.parameters["id"]!!.toLong(), call.userId())
            call.respond(if (accepted) HttpStatusCode.Accepted else HttpStatusCode.Conflict, mapOf("accepted" to accepted))
        }
    }
}

fun io.ktor.server.application.ApplicationCall.userId(): Long = principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()

@Serializable private data class ReadRequest(val throughId: Long)
