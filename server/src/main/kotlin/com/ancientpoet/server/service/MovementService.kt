package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.LocationStatus
import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.repository.CityRepository
import com.ancientpoet.server.repository.UserRepository
import io.ktor.http.HttpStatusCode
import java.time.Instant

class MovementService(private val users: UserRepository, private val cities: CityRepository) {
    companion object {
        const val USER_TRAVEL_SPEED_KM_PER_DAY = 50.0
    }
    suspend fun preview(userId: Long, dynastyId: String, toName: String, toLat: Double, toLng: Double): com.ancientpoet.shared.contract.MovementPreview {
        val city = cities.requireCity(dynastyId, toName, toLat, toLng)
        getStatus(userId, dynastyId)
        val current = users.getLocation(userId, dynastyId)
            ?: return com.ancientpoet.shared.contract.MovementPreview(0.0, 0, true)
        val distance = DelayCalculationService.haversineDistance(current.lat, current.lng, city.lat, city.lng)
        return com.ancientpoet.shared.contract.MovementPreview(distance, if (distance < 1) 0 else travelSeconds(distance), false)
    }
    private fun travelSeconds(distance: Double) = (distance / USER_TRAVEL_SPEED_KM_PER_DAY * 86400).toLong().coerceIn(3600, 7 * 86400)
    suspend fun startMoving(userId: Long, dynastyId: String, toName: String, toLat: Double, toLng: Double): MovementResult {
        val city = cities.requireCity(dynastyId, toName, toLat, toLng)
        getStatus(userId, dynastyId)
        val current = users.getLocation(userId, dynastyId)
        if (current == null) {
            users.upsertLocation(userId, dynastyId, city.name, city.lat, city.lng, "settled")
            return getStatus(userId, dynastyId)
        }
        if (current.status == LocationStatus.MOVING) throw ApiException(HttpStatusCode.Conflict, "already_moving", "旅途已开始，请抵达后再选择下一站")
        val distance = DelayCalculationService.haversineDistance(current.lat, current.lng, city.lat, city.lng)
        if (distance < 1) return getStatus(userId, dynastyId)
        val seconds = travelSeconds(distance)
        val now = Instant.now()
        if (!users.setMoving(userId, dynastyId, city.name, city.lat, city.lng, now, now.plusSeconds(seconds))) {
            throw ApiException(HttpStatusCode.Conflict, "already_moving", "旅途状态已变化，请刷新")
        }
        return getStatus(userId, dynastyId)
    }
    suspend fun getStatus(userId: Long, dynastyId: String): MovementResult {
        cities.requireDynasty(dynastyId)
        users.settleArrived(userId, dynastyId)
        val loc = users.getLocation(userId, dynastyId)
            ?: return MovementResult("unknown", "未选择落脚地", 0.0, 0.0, null, null, null, 0)
        return MovementResult(
            loc.status.name.lowercase(),
            loc.locationName,
            loc.lat,
            loc.lng,
            loc.movingToName,
            loc.movingToLat,
            loc.movingToLng,
            loc.movingArrivalTime?.let { (Instant.parse(it).epochSecond - Instant.now().epochSecond).coerceAtLeast(0) } ?: 0
        )
    }
}
data class MovementResult(
    val status: String,
    val currentName: String,
    val currentLat: Double,
    val currentLng: Double,
    val movingToName: String?,
    val movingToLat: Double?,
    val movingToLng: Double?,
    val remainingSeconds: Long
)
