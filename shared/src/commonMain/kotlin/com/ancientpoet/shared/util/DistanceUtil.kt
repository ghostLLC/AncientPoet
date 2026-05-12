package com.ancientpoet.shared.util

import kotlin.math.*

object DistanceUtil {
    private const val EARTH_RADIUS_KM = 6371.0

    fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1).toRadians()
        val dLng = (lng2 - lng1).toRadians()
        val a = sin(dLat / 2).pow(2) +
                cos(lat1.toRadians()) * cos(lat2.toRadians()) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    private fun Double.toRadians() = this * PI / 180.0
}
