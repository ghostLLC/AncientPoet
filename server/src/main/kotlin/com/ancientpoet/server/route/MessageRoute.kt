package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.MessageService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.messageRoute() {
    val messageService: MessageService by inject()

    authenticate("auth-jwt") {
        post("/conversations/{id}/messages") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val convId = call.parameters["id"]!!.toLong()
            val request = call.receive<SendMessageRequest>()

            val delivery = messageService.sendMessage(
                conversationId = convId,
                userId = userId,
                contentText = request.contentText,
                contentImageUrl = request.contentImageUrl,
            )

            call.respond(HttpStatusCode.OK, MessageResponse(
                messageId = 0, // Actual message IDs are internal
                status = "sent",
                estimatedDelivery = EstimatedDeliveryDto(
                    delaySeconds = delivery.delaySeconds,
                    deliverAt = delivery.deliverAt,
                    distanceKm = delivery.distanceKm,
                    fromLocation = delivery.fromLocation,
                    toLocation = delivery.toLocation,
                    factors = delivery.factors,
                ),
            ))
        }

        get("/conversations/{id}/messages") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val convId = call.parameters["id"]!!.toLong()
            val messages = messageService.getMessages(convId, userId)
            call.respond(HttpStatusCode.OK, messages.map { msg ->
                MessageItem(
                    id = msg.id, conversationId = msg.conversationId,
                    senderType = msg.senderType,
                    contentText = msg.contentText,
                    translation = msg.translation,
                    isDelivered = msg.isDelivered,
                    scheduledDeliveryAt = msg.scheduledDeliveryAt,
                    deliveredAt = msg.deliveredAt,
                    createdAt = msg.createdAt,
                )
            })
        }

        get("/conversations/{id}/pending") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val convId = call.parameters["id"]!!.toLong()
            val pending = messageService.getPending(convId, userId)
            val now = java.time.Instant.now()
            call.respond(HttpStatusCode.OK, pending.map { msg ->
                PendingMessageItem(
                    id = msg.id, senderType = msg.senderType,
                    contentText = msg.contentText,
                    scheduledDeliveryAt = msg.scheduledDeliveryAt,
                    delaySeconds = msg.delaySeconds,
                    estimatedSecondsRemaining = msg.scheduledDeliveryAt?.let {
                        val scheduled = java.time.Instant.parse(it)
                        (scheduled.epochSecond - now.epochSecond).coerceAtLeast(0)
                    },
                )
            })
        }
    }
}
