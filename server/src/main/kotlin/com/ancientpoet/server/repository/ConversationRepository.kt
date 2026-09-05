package com.ancientpoet.server.repository

import com.ancientpoet.server.config.*
import com.ancientpoet.server.model.domain.Conversation
import com.ancientpoet.server.plugin.notFound
import java.sql.ResultSet

class ConversationRepository {
    suspend fun create(
        userId: Long,
        poetId: Long,
        mode: String,
        dynastyId: String,
        backgroundSetting: String?,
        storylineStartYear: Int? = null
    ): Conversation {
        val id = database { db ->
            db.rows(
                """
                INSERT INTO conversations(user_id,poet_id,mode,dynasty_id,background_setting,storyline_current_year)
                VALUES (?,?,?,?,?,?) RETURNING id
                """.trimIndent(),
                userId,
                poetId,
                mode,
                dynastyId,
                backgroundSetting,
                storylineStartYear
            ) { it.getLong(1) }.single()
        }
        return findById(id)!!
    }

    suspend fun requireOwned(conversationId: Long, userId: Long): Conversation = findById(conversationId)?.takeIf { it.userId == userId } ?: notFound()

    suspend fun updateStorylineYear(conversationId: Long, year: Int, userId: Long) {
        database { db ->
            if (db.update(
                    "UPDATE conversations SET storyline_current_year=?,updated_at=now() WHERE id=? AND user_id=?",
                    year,
                    conversationId,
                    userId
                ) == 0
            ) {
                notFound()
            }
        }
    }

    suspend fun findByUserId(userId: Long, archived: Boolean = false): List<Conversation> = database { db ->
        db.rows(
            select + " WHERE c.user_id=? AND c.archived=? ORDER BY c.updated_at DESC,c.id DESC",
            userId,
            archived
        ) { it.conversation() }
    }

    suspend fun findById(conversationId: Long): Conversation? = database { db ->
        db.rows(select + " WHERE c.id=?", conversationId) { it.conversation() }.singleOrNull()
    }

    suspend fun archive(conversationId: Long, userId: Long, archived: Boolean) {
        database { db ->
            if (db.update(
                    "UPDATE conversations SET archived=?,updated_at=now() WHERE id=? AND user_id=?",
                    archived,
                    conversationId,
                    userId
                ) == 0
            ) {
                notFound()
            }
        }
    }

    suspend fun delete(conversationId: Long, userId: Long) {
        database { db -> if (db.update("DELETE FROM conversations WHERE id=? AND user_id=?", conversationId, userId) == 0) notFound() }
    }

    private val select = """
        SELECT c.*,p.name AS poet_name,p.portrait_url,d.name AS dynasty_name,
            COALESCE(last_letter.content_text,'') AS last_message,last_letter.activity_at,
            (SELECT count(*) FROM messages m WHERE m.conversation_id=c.id AND m.sender_type='poet'
                AND m.is_delivered=true AND m.read_at IS NULL) AS unread_count,
            (SELECT max(m.id) FROM messages m WHERE m.conversation_id=c.id AND m.sender_type='poet'
                AND m.is_delivered=true AND m.read_at IS NULL) AS latest_unread_message_id,
            (SELECT count(*) FROM message_jobs j JOIN messages u ON u.id=j.message_id
                LEFT JOIN messages r ON r.reply_to_message_id=u.id
                WHERE u.conversation_id=c.id AND j.kind='generate'
                    AND (j.state<>'done' OR r.is_delivered=false)) AS pending_count
        FROM conversations c JOIN poets p ON p.id=c.poet_id JOIN dynasties d ON d.id=c.dynasty_id
        LEFT JOIN LATERAL (
            SELECT content_text,COALESCE(delivered_at,created_at) AS activity_at
            FROM messages WHERE conversation_id=c.id AND is_delivered=true
            ORDER BY id DESC LIMIT 1
        ) last_letter ON true
    """.trimIndent()

    private fun ResultSet.conversation() = Conversation(
        id = getLong("id"), userId = getLong("user_id"), poetId = getLong("poet_id"), poetName = getString("poet_name"),
        mode = getString("mode"), dynastyId = getString("dynasty_id"),
        storylineCurrentYear = getInt("storyline_current_year").let { if (wasNull()) null else it },
        storylineCompleted = getBoolean("storyline_completed"), backgroundSetting = getString("background_setting"),
        createdAt = instant("created_at"), updatedAt = instant("updated_at"), dynastyName = getString("dynasty_name"),
        portraitUrl = getString("portrait_url"), lastMessage = getString("last_message"),
        lastActivityAt = instant("activity_at") ?: instant("updated_at"),
        unreadCount = getInt("unread_count"), pendingCount = getInt("pending_count"), archived = getBoolean("archived"),
        latestUnreadMessageId = getLong("latest_unread_message_id").let { if (wasNull()) null else it }
    )
}
