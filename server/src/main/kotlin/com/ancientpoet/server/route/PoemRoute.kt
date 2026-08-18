package com.ancientpoet.server.route

import com.ancientpoet.server.repository.PoetRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.poemRoute(poetRepository: PoetRepository) {

    get("/poems/search") {
        val query = call.request.queryParameters["q"] ?: ""
        if (query.isBlank()) {
            call.respond(HttpStatusCode.OK, emptyList<Any>())
            return@get
        }
        // Search across all poets' poems by title/content matching
        val results = poetRepository.findAll().flatMap { poet ->
            poetRepository.findPoemsByPoetId(poet.id)
                .filter { p ->
                    p.title.contains(query, ignoreCase = true) ||
                    p.content.contains(query, ignoreCase = true)
                }
                .map { p ->
                    mapOf(
                        "id" to p.id, "title" to p.title,
                        "poetId" to poet.id, "poetName" to poet.name,
                        "content" to p.content.take(50),
                        "yearWritten" to p.yearWritten,
                        "tags" to p.tags,
                    )
                }
        }.take(20)
        call.respond(HttpStatusCode.OK, results)
    }
}
