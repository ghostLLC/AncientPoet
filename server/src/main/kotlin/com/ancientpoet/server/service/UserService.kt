package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.LocationStatus
import com.ancientpoet.server.model.domain.User
import com.ancientpoet.server.model.domain.UserLocation
import com.ancientpoet.server.repository.UserRepository

class UserService(private val userRepository: UserRepository) {

    suspend fun getProfile(userId: Long): User? = userRepository.findById(userId)

    suspend fun updateProfile(userId: Long, nickname: String?, avatarUrl: String?, bio: String?) {
        userRepository.updateProfile(userId, nickname, avatarUrl, bio)
    }

    suspend fun getLocation(userId: Long, dynastyId: String): UserLocation? {
        return userRepository.getLocation(userId, dynastyId)
    }

    suspend fun updateLocation(
        userId: Long, dynastyId: String, locationName: String,
        lat: Double, lng: Double, status: String,
    ) {
        userRepository.upsertLocation(userId, dynastyId, locationName, lat, lng, status)
    }
}
