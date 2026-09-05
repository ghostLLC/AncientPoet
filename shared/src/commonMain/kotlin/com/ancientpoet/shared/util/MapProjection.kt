package com.ancientpoet.shared.util

import kotlin.math.cos
import kotlin.math.min
data class GeographicPoint(val lat: Double, val lng: Double)
data class MapPoint(val x: Double, val y: Double)

/** Shared projection for cities, people and route lines; schematic, not a historical boundary map. */
class MapProjection(points: List<GeographicPoint>) {
    private val positions = points.ifEmpty { listOf(GeographicPoint(30.0, 110.0), GeographicPoint(40.0, 120.0)) }
    private val correction = cos(positions.map { it.lat }.average() * kotlin.math.PI / 180.0)
    private val minX = positions.minOf { it.lng * correction } - 1
    private val maxX = positions.maxOf { it.lng * correction } + 1
    private val minY = positions.minOf { -it.lat } - 1
    private val maxY = positions.maxOf { -it.lat } + 1
    fun project(lat: Double, lng: Double, width: Double, height: Double, padding: Double = 24.0): MapPoint {
        val scale = min((width - 2 * padding).coerceAtLeast(1.0) / (maxX - minX), (height - 2 * padding).coerceAtLeast(1.0) / (maxY - minY))
        return MapPoint(width / 2 + (lng * correction - (minX + maxX) / 2) * scale, height / 2 + (-lat - (minY + maxY) / 2) * scale)
    }
}
