package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.CommunityService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.communityRoute(service: CommunityService) {

    authenticate("auth-jwt") {
        get("/community/posts") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            call.respond(HttpStatusCode.OK, service.getPosts(page))
        }

        post("/community/posts") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            call.respond(HttpStatusCode.Created, service.createPost(userId, call.receive()))
        }

        post("/community/posts/{id}/like") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val postId = call.parameters["id"]!!.toLong()
            call.respond(HttpStatusCode.OK, service.toggleLike(postId, userId))
        }

        get("/community/posts/{id}/comments") {
            val postId = call.parameters["id"]!!.toLong()
            call.respond(HttpStatusCode.OK, service.getComments(postId))
        }

        post("/community/posts/{id}/comments") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val postId = call.parameters["id"]!!.toLong()
            call.respond(HttpStatusCode.Created, service.createComment(postId, userId, call.receive()))
        }

        post("/community/posts/{id}/repost") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val postId = call.parameters["id"]!!.toLong()
            call.respond(HttpStatusCode.Created, service.repost(userId, postId))
        }

        post("/user/favorites/{messageId}") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            val messageId = call.parameters["messageId"]!!.toLong()
            call.respond(HttpStatusCode.OK, service.toggleFavorite(userId, messageId))
        }

        get("/user/favorites") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asLong()
            call.respond(HttpStatusCode.OK, mapOf("messageIds" to service.getFavorites(userId)))
        }

        get("/user/profile/{id}") {
            val profileId = call.parameters["id"]!!.toLong()
            call.respond(HttpStatusCode.OK, service.getProfile(profileId))
        }
    }
}
