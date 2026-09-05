package com.ancientpoet.server.route

import com.ancientpoet.server.config.*
import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.shared.contract.PoemDetail
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.sql.ResultSet
import kotlinx.serialization.json.Json

fun Route.poemRoute(poets: PoetRepository) {
    suspend fun search(query: String, poetId: Long?, offset: Int): List<PoemDetail> {
        if (query.length > 100 || offset !in 0..10_000) invalid("检索参数无效")
        return database { db ->
            val literal = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            val pattern = "%" + literal + "%"
            val filter = if (poetId == null) "" else " AND p.poet_id=?"
            val args = mutableListOf<Any?>(pattern, pattern, pattern)
            if (poetId != null) args.add(poetId)
            args.add(offset)
            db.rows(
                """SELECT p.*,a.name AS poet_name,d.name AS dynasty FROM poems p
                JOIN poets a ON a.id=p.poet_id JOIN dynasties d ON d.id=a.dynasty_id
                WHERE (p.title ILIKE ? OR p.content ILIKE ? OR a.name ILIKE ?)""" + filter +
                    " ORDER BY p.poet_id,p.id LIMIT 40 OFFSET ?",
                *args.toTypedArray()
            ) { it.poem() }
        }
    }
    get("/poems") {
        call.respond(
            search(
                call.request.queryParameters["q"].orEmpty(),
                call.request.queryParameters["poetId"]?.toLong(),
                call.request.queryParameters["offset"]?.toInt() ?: 0
            )
        )
    }
    get("/poems/search") {
        call.respond(
            search(
                call.request.queryParameters["q"].orEmpty(),
                call.request.queryParameters["poetId"]?.toLong(),
                call.request.queryParameters["offset"]?.toInt() ?: 0
            )
        )
    }
    get("/poems/{id}") {
        val id = call.parameters["id"]!!.toLong()
        val poem = database { db ->
            db.rows(
                """SELECT p.*,a.name AS poet_name,d.name AS dynasty FROM poems p
            JOIN poets a ON a.id=p.poet_id JOIN dynasties d ON d.id=a.dynasty_id WHERE p.id=?""",
                id
            ) { it.poem() }.singleOrNull()
        }
        call.respond(poem ?: notFound())
    }
}
private fun ResultSet.poem() = PoemDetail(
    id = getLong("id"), poetId = getLong("poet_id"), poetName = getString("poet_name"), dynasty = getString("dynasty"),
    title = getString("title"), content = getString("content"), yearWritten = getObject("year_written")?.let { getInt("year_written") },
    context = getString("context"), translation = getString("translation"), appreciation = getString("appreciation"),
    tags = getString("tags")?.let { Json.decodeFromString<List<String>>(it) }.orEmpty(), sourceUrl = getString("source_url")
)
