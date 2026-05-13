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
import org.koin.ktor.ext.inject

fun Route.communityRoute() {
    val communityService: CommunityService by inject()

    authenticate("auth-jwt") {
        get("/community/posts") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val posts = communityService.getPosts(page)
            call.respond(HttpStatusCode.OK, posts)
        }

        post("/community/posts") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val request = call.receive<CreatePostRequest>()
            val post = communityService.createPost(userId, request)
            call.respond(HttpStatusCode.Created, post)
        }

        get("/community/posts/{id}") {
            val post = communityService.getPosts(0) // TODO: single post endpoint
            call.respond(HttpStatusCode.OK, post)
        }

        post("/community/posts/{id}/like") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val postId = call.parameters["id"]!!.toLong()
            val result = communityService.toggleLike(postId, userId)
            call.respond(HttpStatusCode.OK, result)
        }

        get("/community/posts/{id}/comments") {
            val postId = call.parameters["id"]!!.toLong()
            val comments = communityService.getComments(postId)
            call.respond(HttpStatusCode.OK, comments)
        }

        post("/community/posts/{id}/comments") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val postId = call.parameters["id"]!!.toLong()
            val request = call.receive<CreateCommentRequest>()
            val comment = communityService.createComment(postId, userId, request)
            call.respond(HttpStatusCode.Created, comment)
        }

        post("/community/posts/{id}/repost") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val postId = call.parameters["id"]!!.toLong()
            val repost = communityService.repost(userId, postId)
            call.respond(HttpStatusCode.Created, repost)
        }

        // Favorites
        post("/user/favorites/{messageId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val messageId = call.parameters["messageId"]!!.toLong()
            val result = communityService.toggleFavorite(userId, messageId)
            call.respond(HttpStatusCode.OK, result)
        }

        get("/user/favorites") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong() ?: return@authenticate call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "未授权"))
            val favorites = communityService.getFavorites(userId)
            call.respond(HttpStatusCode.OK, mapOf("messageIds" to favorites))
        }

        // Profile
        get("/user/profile/{id}") {
            val profileId = call.parameters["id"]!!.toLong()
            val profile = communityService.getProfile(profileId)
            call.respond(HttpStatusCode.OK, profile)
        }
    }
}
