package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.LocationStatus
import com.ancientpoet.server.model.domain.UserLocation
import com.ancientpoet.server.repository.UserRepository
import java.time.Instant

class MovementService(private val userRepository: UserRepository) {
    companion object {
        const val USER_TRAVEL_SPEED_KM_PER_DAY = 50.0
    }

    suspend fun startMoving(
        userId: Long, dynastyId: String,
        toName: String, toLat: Double, toLng: Double,
    ): MovementResult {
        val currentLoc = userRepository.getLocation(userId, dynastyId)
        if (currentLoc == null) {
            // Auto-create a location for the user
            userRepository.upsertLocation(userId, dynastyId, toName, toLat, toLng, "settled")
            return MovementResult(
                status = "settled",
                currentName = toName, currentLat = toLat, currentLng = toLng,
                movingToName = null, movingToLat = null, movingToLng = null,
                remainingSeconds = 0,
            )
        }

        val distanceKm = DelayCalculationService.haversineDistance(
            currentLoc.lat, currentLoc.lng, toLat, toLng
        )
        val travelDays = distanceKm / USER_TRAVEL_SPEED_KM_PER_DAY
        val travelSeconds = maxOf(3600L, (travelDays * 86400).toLong())

        val now = Instant.now()
        val arrivalTime = now.plusSeconds(travelSeconds)

        userRepository.setMoving(
            userId, dynastyId,
            toName, toLat, toLng, now, arrivalTime,
        )

        return MovementResult(
            status = "moving",
            currentName = currentLoc.locationName,
            currentLat = currentLoc.lat,
            currentLng = currentLoc.lng,
            movingToName = toName,
            movingToLat = toLat,
            movingToLng = toLng,
            remainingSeconds = travelSeconds,
        )
    }

    suspend fun completeMovement(userId: Long, dynastyId: String): MovementResult {
        val loc = userRepository.getLocation(userId, dynastyId)
            ?: throw IllegalArgumentException("No location found")

        if (loc.status != LocationStatus.MOVING) {
            return getStatus(userId, dynastyId)
        }

        val toName = loc.movingToName!!
        val toLat = loc.movingToLat!!
        val toLng = loc.movingToLng!!

        userRepository.upsertLocation(userId, dynastyId, toName, toLat, toLng, "settled")

        return MovementResult(
            status = "settled",
            currentName = toName, currentLat = toLat, currentLng = toLng,
            movingToName = null, movingToLat = null, movingToLng = null,
            remainingSeconds = 0,
        )
    }

    suspend fun getStatus(userId: Long, dynastyId: String): MovementResult {
        val loc = userRepository.getLocation(userId, dynastyId)
            ?: return MovementResult(
                status = "unknown",
                currentName = "未知", currentLat = 0.0, currentLng = 0.0,
                movingToName = null, movingToLat = null, movingToLng = null,
                remainingSeconds = 0,
            )

        if (loc.status == LocationStatus.MOVING && loc.movingArrivalTime != null) {
            val now = Instant.now()
            val arrival = Instant.parse(loc.movingArrivalTime)
            val remaining = maxOf(0L, arrival.epochSecond - now.epochSecond)

            if (remaining == 0L) {
                return completeMovement(userId, dynastyId)
            }

            return MovementResult(
                status = "moving",
                currentName = loc.locationName,
                currentLat = loc.lat, currentLng = loc.lng,
                movingToName = loc.movingToName,
                movingToLat = loc.movingToLat,
                movingToLng = loc.movingToLng,
                remainingSeconds = remaining,
            )
        }

        return MovementResult(
            status = "settled",
            currentName = loc.locationName,
            currentLat = loc.lat, currentLng = loc.lng,
            movingToName = null, movingToLat = null, movingToLng = null,
            remainingSeconds = 0,
        )
    }

    suspend fun checkAutoArrivals(userId: Long) {
        val loc = userRepository.getLocation(userId, "tang") // check all dynasties
        // In production, check all user's dynasties
        // For Phase 2, auto-complete is triggered on next API call via getStatus
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
    val remainingSeconds: Long,
)
