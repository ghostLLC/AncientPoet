package com.ancientpoet.server.model.domain

data class User(
    val id: Long,
    val phone: String,
    val nickname: String?,
    val avatarUrl: String?,
    val bio: String?,
)

data class UserLocation(
    val id: Long,
    val userId: Long,
    val dynastyId: String,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val status: LocationStatus,
    val movingToName: String? = null,
    val movingToLat: Double? = null,
    val movingToLng: Double? = null,
    val movingStartTime: String? = null,
    val movingArrivalTime: String? = null,
)

enum class LocationStatus {
    SETTLED, MOVING;

    companion object {
        fun fromString(s: String): LocationStatus = when (s.lowercase()) {
            "settled" -> SETTLED
            "moving" -> MOVING
            else -> SETTLED
        }
    }
}
