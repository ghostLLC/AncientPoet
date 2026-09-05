package com.ancientpoet.server.repository

import com.ancientpoet.server.config.*
import com.ancientpoet.server.model.domain.*
import com.ancientpoet.server.plugin.*
import java.sql.ResultSet
import java.time.Instant

class UserRepository {
    suspend fun findByPhone(phone: String): User? = database { db ->
        db.rows("SELECT * FROM users WHERE phone=?", phone) { it.user() }.singleOrNull()
    }
    suspend fun findById(id: Long): User? = database { db ->
        db.rows("SELECT * FROM users WHERE id=?", id) { it.user() }.singleOrNull()
    }
    suspend fun create(phone: String): User = database { db ->
        db.rows(
            "INSERT INTO users(phone) VALUES (?) ON CONFLICT(phone) DO UPDATE SET phone=EXCLUDED.phone RETURNING *",
            phone
        ) { it.user() }.single()
    }
    suspend fun updateProfile(userId: Long, nickname: String?, avatarUrl: String?, bio: String?) = database { db ->
        db.update(
            "UPDATE users SET nickname=COALESCE(?,nickname),bio=COALESCE(?,bio),updated_at=now() WHERE id=?",
            nickname,
            bio,
            userId
        )
    }
    suspend fun deleteAccount(userId: Long) = database { db ->
        db.update("DELETE FROM users WHERE id=?", userId)
    }
    suspend fun exportAccount(userId: Long): String = database { db ->
        // Export only the caller's data, including their original letters and delivered replies.
        db.rows(
            """
            SELECT jsonb_build_object(
              'profile',(SELECT to_jsonb(u)-'id' FROM users u WHERE id=?),
              'locations',COALESCE((SELECT jsonb_agg(to_jsonb(l)) FROM user_locations l WHERE user_id=?),'[]'::jsonb),
              'conversations',COALESCE((SELECT jsonb_agg(to_jsonb(c)) FROM conversations c WHERE user_id=?),'[]'::jsonb),
              'messages',COALESCE((SELECT jsonb_agg(to_jsonb(m) ORDER BY m.id) FROM messages m
                JOIN conversations c ON c.id=m.conversation_id WHERE c.user_id=? AND m.is_delivered=true),'[]'::jsonb)
            )::text
            """.trimIndent(),
            userId,
            userId,
            userId,
            userId
        ) { it.getString(1) }.single()
    }
    suspend fun getLocation(userId: Long, dynastyId: String): UserLocation? = database { db ->
        db.rows("SELECT * FROM user_locations WHERE user_id=? AND dynasty_id=?", userId, dynastyId) { it.location() }.singleOrNull()
    }
    suspend fun upsertLocation(userId: Long, dynastyId: String, locationName: String, lat: Double, lng: Double, status: String) = database { db ->
        val changed = db.update(
            """
            INSERT INTO user_locations(user_id,dynasty_id,location_name,lat,lng,status) VALUES (?,?,?,?,?,'settled')
            ON CONFLICT(user_id,dynasty_id) DO NOTHING
            """.trimIndent(),
            userId,
            dynastyId,
            locationName,
            lat,
            lng
        )
        if (changed == 0) throw ApiException(io.ktor.http.HttpStatusCode.Conflict, "location_exists", "落脚地已设置，请刷新驿路")
    }
    suspend fun setMoving(userId: Long, dynastyId: String, toName: String, toLat: Double, toLng: Double, startTime: Instant, arrivalTime: Instant): Boolean = database { db ->
        db.update(
            """
                UPDATE user_locations SET status='moving',moving_to_name=?,moving_to_lat=?,moving_to_lng=?,
                    moving_start_time=?,moving_arrival_time=?,updated_at=now()
                WHERE user_id=? AND dynasty_id=? AND status='settled'
            """.trimIndent(),
            toName,
            toLat,
            toLng,
            startTime,
            arrivalTime,
            userId,
            dynastyId
        ) > 0
    }
    suspend fun settleArrived(userId: Long, dynastyId: String) = database { db ->
        db.update(
            """
            UPDATE user_locations SET location_name=moving_to_name,lat=moving_to_lat,lng=moving_to_lng,
              status='settled',moving_to_name=NULL,moving_to_lat=NULL,moving_to_lng=NULL,
              moving_start_time=NULL,moving_arrival_time=NULL,updated_at=now()
            WHERE user_id=? AND dynasty_id=? AND status='moving' AND moving_arrival_time<=now()
            """.trimIndent(),
            userId,
            dynastyId
        )
    }
    private fun ResultSet.user() = User(getLong("id"), getString("phone"), getString("nickname"), getString("avatar_url"), getString("bio"))
    private fun ResultSet.location() = UserLocation(
        getLong("id"), getLong("user_id"), getString("dynasty_id"), getString("location_name"),
        getDouble("lat"), getDouble("lng"), LocationStatus.fromString(getString("status")),
        getString("moving_to_name"), getObject("moving_to_lat")?.let { getDouble("moving_to_lat") },
        getObject("moving_to_lng")?.let { getDouble("moving_to_lng") }, instant("moving_start_time"), instant("moving_arrival_time")
    )
}
