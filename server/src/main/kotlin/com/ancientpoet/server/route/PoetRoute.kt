package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.server.service.PoetLocationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.poetRoute(
    poetRepository: PoetRepository,
    poetLocationService: PoetLocationService,
) {

    get("/poets") {
        val poets = poetRepository.findAll()
        call.respond(HttpStatusCode.OK, PoetListResponse(
            poets = poets.map { p ->
                PoetItem(
                    id = p.id, name = p.name,
                    courtesyName = p.courtesyName, artName = p.artName,
                    dynastyId = p.dynastyId, dynastyName = p.dynastyName,
                    birthYear = p.birthYear, deathYear = p.deathYear,
                    isFree = p.isFree, portraitUrl = p.portraitUrl,
                )
            }
        ))
    }

    get("/poets/{id}") {
        val poetId = call.parameters["id"]!!.toLong()
        val poet = poetRepository.findById(poetId)
        if (poet != null) {
            call.respond(HttpStatusCode.OK, PoetDetailResponse(
                id = poet.id, name = poet.name,
                courtesyName = poet.courtesyName, artName = poet.artName,
                dynastyId = poet.dynastyId, dynastyName = poet.dynastyName,
                birthYear = poet.birthYear, deathYear = poet.deathYear,
                biographySummary = poet.biographySummary,
                personalityProfile = PersonalityProfileDto(
                    traits = poet.personalityProfile.traits,
                    mbti = poet.personalityProfile.mbti,
                    speakingStyle = poet.personalityProfile.speakingStyle,
                ),
                writingStyle = poet.writingStyle,
                portraitUrl = poet.portraitUrl, isFree = poet.isFree,
            ))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "诗人不存在"))
        }
    }

    get("/poets/{id}/location") {
        val poetId = call.parameters["id"]!!.toLong()
        val year = call.request.queryParameters["year"]?.toIntOrNull()
            ?: poetLocationService.getDefaultYear(poetId)
        val location = poetLocationService.getPoetLocation(poetId, year)
        val poet = poetRepository.findById(poetId)
        if (location != null && poet != null) {
            call.respond(HttpStatusCode.OK, PoetLocationResponse(
                poetId = poetId, poetName = poet.name, year = year,
                locationName = location.locationName,
                lat = location.lat, lng = location.lng,
                eventDescription = location.eventDescription,
                eventType = location.eventType,
            ))
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "诗人位置不存在"))
        }
    }

    get("/poets/{id}/poems") {
        val poetId = call.parameters["id"]!!.toLong()
        val poems = poetRepository.findPoemsByPoetId(poetId)
        call.respond(HttpStatusCode.OK, poems.map { p ->
            PoemResponse(
                id = p.id, title = p.title, content = p.content,
                yearWritten = p.yearWritten, context = p.context,
                translation = p.translation, appreciation = p.appreciation,
                tags = p.tags,
            )
        })
    }

    get("/poets/{id}/life-events") {
        val poetId = call.parameters["id"]!!.toLong()
        val events = poetRepository.findLifeEventsByPoetId(poetId)
        call.respond(HttpStatusCode.OK, events.map { e ->
            LifeEventResponse(
                id = e.id, year = e.year, age = e.age,
                title = e.title, description = e.description,
                locationName = e.locationName, eventType = e.eventType,
                delayMultiplier = e.delayMultiplier,
            )
        })
    }
}
