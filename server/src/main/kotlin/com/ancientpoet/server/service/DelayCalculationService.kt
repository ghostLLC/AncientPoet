package com.ancientpoet.server.service

import kotlin.math.*

object DelayCalculationService {
    private const val ANCIENT_TRAVEL_SPEED_KM_PER_DAY = 80.0
    private const val SETTLED_MULTIPLIER = 0.8
    private const val MIN_DELAY_HOURS = 2L
    private const val MAX_DELAY_DAYS = 7L

    fun calculate(
        userLat: Double, userLng: Double,
        poetLat: Double, poetLng: Double,
        settled: Boolean = true,
        eventDelayMultiplier: Double = 1.0,
    ): DelayResult {
        val distanceKm = haversineDistance(userLat, userLng, poetLat, poetLng)
        val baseDays = distanceKm / ANCIENT_TRAVEL_SPEED_KM_PER_DAY

        val baseDelayHours = when {
            distanceKm < 30 -> 2.0
            distanceKm < 150 -> 24.0
            else -> baseDays * 24.0
        }

        val settledCoefficient = if (settled) SETTLED_MULTIPLIER else 1.0
        val adjustedHours = baseDelayHours * settledCoefficient * eventDelayMultiplier

        val finalSeconds = (adjustedHours * 3600).toLong()
        val minSeconds = MIN_DELAY_HOURS * 3600
        val maxSeconds = MAX_DELAY_DAYS * 86400

        val clampedSeconds = finalSeconds.coerceIn(minSeconds, maxSeconds)

        return DelayResult(
            distanceKm = roundTo2(distanceKm),
            baseDelayHours = roundTo2(adjustedHours),
            finalDelaySeconds = clampedSeconds,
        )
    }

    fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun roundTo2(value: Double): Double = round(value * 100) / 100
}

data class DelayResult(
    val distanceKm: Double,
    val baseDelayHours: Double,
    val finalDelaySeconds: Long,
)
