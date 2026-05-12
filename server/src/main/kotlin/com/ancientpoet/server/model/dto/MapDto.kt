package com.ancientpoet.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class MapMetadataResponse(
    val dynasty: String,
    val name: String,
    val startYear: Int,
    val endYear: Int,
    val mapImageSize: MapImageSize,
    val cityCount: Int,
)

@Serializable
data class MapImageSize(
    val width: Int,
    val height: Int,
)

@Serializable
data class CityResponse(
    val name: String,
    val modernName: String? = null,
    val province: String? = null,
    val lat: Double,
    val lng: Double,
    val mapX: Int,
    val mapY: Int,
    val isCapital: Boolean,
)

@Serializable
data class DelayPreviewRequest(
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
)

@Serializable
data class DelayPreviewResponse(
    val distanceKm: Double,
    val baseDelayHours: Double,
    val estimatedDelay: String,
)
