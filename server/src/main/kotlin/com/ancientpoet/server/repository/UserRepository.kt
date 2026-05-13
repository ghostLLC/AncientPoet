package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.UserLocationsTable
import com.ancientpoet.server.model.db.UsersTable
import com.ancientpoet.server.model.domain.LocationStatus
import com.ancientpoet.server.model.domain.User
import com.ancientpoet.server.model.domain.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class UserRepository {
    suspend fun findByPhone(phone: String): User? = withContext(Dispatchers.IO) {
        transaction {
            UsersTable.selectAll().where { UsersTable.phone eq phone }.singleOrNull()?.toUser()
        }
    }

    suspend fun findById(id: Long): User? = withContext(Dispatchers.IO) {
        transaction {
            UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
        }
    }

    suspend fun create(phone: String): User = withContext(Dispatchers.IO) {
        transaction {
            val result = UsersTable.insert {
                it[UsersTable.phone] = phone
            }
            val id = result[UsersTable.id]
            User(id = id, phone = phone, nickname = null, avatarUrl = null, bio = null)
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
            UserLocationsTable.selectAll().where { (UserLocationsTable.userId eq userId) and (UserLocationsTable.dynastyId eq dynastyId) }
                .singleOrNull()?.let { row ->
                    UserLocation(
                        id = row[UserLocationsTable.id],
                        userId = row[UserLocationsTable.userId],
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
                val existing = UserLocationsTable.selectAll().where { (UserLocationsTable.userId eq userId) and (UserLocationsTable.dynastyId eq dynastyId) }.singleOrNull()
                if (existing != null) {
                    UserLocationsTable.update({ UserLocationsTable.id eq existing[UserLocationsTable.id] }) {
                        it[UserLocationsTable.locationName] = locationName
                        it[UserLocationsTable.lat] = lat
                        it[UserLocationsTable.lng] = lng
                        it[UserLocationsTable.status] = status
                    }
                } else {
                    UserLocationsTable.insert {
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

    suspend fun setMoving(userId: Long, dynastyId: String, toName: String, toLat: Double, toLng: Double, startTime: Instant, arrivalTime: Instant) {
        withContext(Dispatchers.IO) {
            transaction {
                UserLocationsTable.update({ (UserLocationsTable.userId eq userId) and (UserLocationsTable.dynastyId eq dynastyId) }) {
                    it[status] = "moving"
                    it[movingToName] = toName
                    it[movingToLat] = toLat
                    it[movingToLng] = toLng
                    it[movingStartTime] = startTime.toString()
                    it[movingArrivalTime] = arrivalTime.toString()
                }
            }
        }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toUser() = User(
        id = this[UsersTable.id], phone = this[UsersTable.phone],
        nickname = this[UsersTable.nickname], avatarUrl = this[UsersTable.avatarUrl], bio = this[UsersTable.bio],
    )
}
