package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.service.MapService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.mapRoute() {
    // In Phase 1 MVP, serve map data from static JSON files
    // For now, respond with placeholder data

    get("/map/{dynastyId}") {
        val dynastyId = call.parameters["dynastyId"]!!
        call.respond(HttpStatusCode.OK, MapMetadataResponse(
            dynasty = dynastyId,
            name = if (dynastyId == "tang") "唐朝" else "宋朝",
            startYear = if (dynastyId == "tang") 618 else 960,
            endYear = if (dynastyId == "tang") 907 else 1279,
            mapImageSize = MapImageSize(width = 2000, height = 1500),
            cityCount = if (dynastyId == "tang") 16 else 16,
        ))
    }

    get("/map/{dynastyId}/cities") {
        val dynastyId = call.parameters["dynastyId"]!!
        // In production, load from DB; for MVP, return from JSON
        val cities = loadCities(dynastyId)
        call.respond(HttpStatusCode.OK, cities)
    }

    post("/map/delay-preview") {
        val request = call.receive<DelayPreviewRequest>()
        val result = MapService.getDelayPreview(
            request.fromLat, request.fromLng,
            request.toLat, request.toLng,
        )
        call.respond(HttpStatusCode.OK, result)
    }
}

private fun loadCities(dynastyId: String): List<CityResponse> {
    // Phase 1: load from static JSON files in data/cities/
    // For now, return hardcoded Tang cities
    return listOf(
        CityResponse("长安", "西安", "京畿道", 34.26, 108.94, 1050, 680, true),
        CityResponse("洛阳", "洛阳", "都畿道", 34.62, 112.45, 1180, 665, true),
        CityResponse("成都", "成都", "剑南道", 30.57, 104.07, 920, 820, false),
        CityResponse("江陵", "荆州", "山南东道", 30.35, 112.19, 1170, 830, false),
        CityResponse("扬州", "扬州", "淮南道", 32.39, 119.42, 1400, 740, false),
        CityResponse("金陵", "南京", "江南东道", 32.06, 118.80, 1380, 755, false),
        CityResponse("杭州", "杭州", "江南东道", 30.25, 120.17, 1420, 820, false),
        CityResponse("幽州", "北京", "河北道", 39.90, 116.40, 1280, 460, false),
        CityResponse("太原", "太原", "河东道", 37.87, 112.55, 1180, 530, false),
        CityResponse("绵州", "绵阳", "剑南道", 31.46, 104.73, 940, 790, false),
    )
}
