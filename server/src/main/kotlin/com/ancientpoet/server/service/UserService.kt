package com.ancientpoet.server.service

import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.repository.*
import io.ktor.http.HttpStatusCode

class UserService(private val users: UserRepository, private val cities: CityRepository, private val movement: MovementService) {
    suspend fun getProfile(userId: Long) = users.findById(userId)
    suspend fun updateProfile(userId: Long, nickname: String?, avatarUrl: String?, bio: String?) {
        if ((nickname?.length ?: 0) > 50 || (bio?.length ?: 0) > 500) invalid("昵称最多 50 字，介绍最多 500 字")
        if (!avatarUrl.isNullOrBlank()) invalid("头像上传尚未开放")
        users.updateProfile(userId, nickname?.trim(), null, bio?.trim())
    }
    suspend fun getLocation(userId: Long, dynastyId: String) = movement.getStatus(userId, dynastyId).let {
        users.getLocation(userId, dynastyId)
    }
    suspend fun updateLocation(userId: Long, dynastyId: String, locationName: String, lat: Double, lng: Double, status: String) {
        val city = cities.requireCity(dynastyId, locationName, lat, lng)
        if (status != "settled") invalid("初次选择的位置必须为落脚地")
        if (users.getLocation(userId, dynastyId) != null) {
            throw ApiException(HttpStatusCode.Conflict, "location_exists", "已有落脚地，请使用启程功能前往其他城市")
        }
        users.upsertLocation(userId, dynastyId, city.name, city.lat, city.lng, "settled")
    }
    suspend fun exportAccount(userId: Long) = users.exportAccount(userId)
    suspend fun deleteAccount(userId: Long) = users.deleteAccount(userId)
}
