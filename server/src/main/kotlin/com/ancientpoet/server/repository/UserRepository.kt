package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.UserLocationsTable
import com.ancientpoet.server.model.db.UsersTable
import com.ancientpoet.server.model.domain.LocationStatus
import com.ancientpoet.server.model.domain.User
import com.ancientpoet.server.model.domain.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserRepository {
    suspend fun findByPhone(phone: String): User? = withContext(Dispatchers.IO) {
        transaction {
            UsersTable.select { UsersTable.phone eq phone }
                .singleOrNull()
                ?.toUser()
        }
    }

    suspend fun findById(id: Long): User? = withContext(Dispatchers.IO) {
        transaction {
            UsersTable.select { UsersTable.id eq id }
                .singleOrNull()
                ?.toUser()
        }
    }

    suspend fun create(phone: String): User = withContext(Dispatchers.IO) {
        transaction {
            val id = UsersTable.insertAndGetId {
                it[UsersTable.phone] = phone
            }
            User(id = id.value, phone = phone, nickname = null, avatarUrl = null, bio = null)
        }
    }

    suspend fun updateProfile(userId: Long, nickname: String?, avatarUrl: String?, bio: String?) {
        withContext(Dispatchers.IO) {
            transaction {
                UsersTable.update({ UsersTable.id eq userId }) {
                    if (nickname != null) it[UsersTable.nickname] = nickname
                    if (avatarUrl != null) it[UsersTable.avatarUrl] = avatarUrl
                    if (bio != null) it[UsersTable.bio] = bio
                }
            }
        }
    }

    suspend fun getLocation(userId: Long, dynastyId: String): UserLocation? = withContext(Dispatchers.IO) {
        transaction {
            UserLocationsTable.select {
                (UserLocationsTable.userId eq userId) and (UserLocationsTable.dynastyId eq dynastyId)
            }.singleOrNull()?.let { row ->
                UserLocation(
                    id = row[UserLocationsTable.id].value,
                    userId = row[UserLocationsTable.userId].value,
                    dynastyId = row[UserLocationsTable.dynastyId],
                    locationName = row[UserLocationsTable.locationName],
                    lat = row[UserLocationsTable.lat],
                    lng = row[UserLocationsTable.lng],
                    status = LocationStatus.fromString(row[UserLocationsTable.status]),
                )
            }
        }
    }

    suspend fun upsertLocation(userId: Long, dynastyId: String, locationName: String, lat: Double, lng: Double, status: String) {
        withContext(Dispatchers.IO) {
            transaction {
                val existing = UserLocationsTable.select {
                    (UserLocationsTable.userId eq userId) and (UserLocationsTable.dynastyId eq dynastyId)
                }.singleOrNull()
                if (existing != null) {
                    UserLocationsTable.update({ UserLocationsTable.id eq existing[UserLocationsTable.id] }) {
                        it[UserLocationsTable.locationName] = locationName
                        it[UserLocationsTable.lat] = lat
                        it[UserLocationsTable.lng] = lng
                        it[UserLocationsTable.status] = status
                    }
                } else {
                    UserLocationsTable.insertAndGetId {
                        it[UserLocationsTable.userId] = userId
                        it[UserLocationsTable.dynastyId] = dynastyId
                        it[UserLocationsTable.locationName] = locationName
                        it[UserLocationsTable.lat] = lat
                        it[UserLocationsTable.lng] = lng
                        it[UserLocationsTable.status] = status
                    }
                }
            }
        }
    }

    suspend fun setMoving(userId: Long, dynastyId: String, toName: String, toLat: Double, toLng: Double, startTime: java.time.Instant, arrivalTime: java.time.Instant) {
        withContext(Dispatchers.IO) {
            transaction {
                UserLocationsTable.update({
                    (UserLocationsTable.userId eq userId) and (UserLocationsTable.dynastyId eq dynastyId)
                }) {
                    it[status] = "moving"
                    it[movingToName] = toName
                    it[movingToLat] = toLat
                    it[movingToLng] = toLng
                    it[movingStartTime] = startTime
                    it[movingArrivalTime] = arrivalTime
                }
            }
        }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toUser() = User(
        id = this[UsersTable.id].value,
        phone = this[UsersTable.phone],
        nickname = this[UsersTable.nickname],
        avatarUrl = this[UsersTable.avatarUrl],
        bio = this[UsersTable.bio],
    )
}