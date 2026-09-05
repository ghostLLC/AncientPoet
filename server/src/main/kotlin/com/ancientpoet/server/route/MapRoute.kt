package com.ancientpoet.server.route

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.plugin.invalid
import com.ancientpoet.server.repository.CityRepository
import com.ancientpoet.server.service.DelayCalculationService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.mapRoute(cities: CityRepository) {
    get("/map/{dynastyId}") { call.respond(cities.requireDynasty(call.parameters["dynastyId"]!!)) }
    get("/map/{dynastyId}/cities") { call.respond(cities.list(call.parameters["dynastyId"]!!)) }
    post("/map/delay-preview") {
        val r = call.receive<DelayPreviewRequest>()
        if (listOf(r.fromLat, r.toLat).any { !it.isFinite() || it !in -90.0..90.0 } ||
            listOf(r.fromLng, r.toLng).any { !it.isFinite() || it !in -180.0..180.0 }
        ) {
            invalid("坐标无效")
        }
        val result = DelayCalculationService.calculate(r.fromLat, r.fromLng, r.toLat, r.toLng)
        val hours = result.finalDelaySeconds / 3600.0
        call.respond(DelayPreviewResponse(result.distanceKm, hours, "约 " + hours.toInt() + " 小时；寄信时以通信详情为准"))
    }
}
