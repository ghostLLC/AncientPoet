package com.ancientpoet.server.repository

import com.ancientpoet.server.config.*
import com.ancientpoet.server.model.domain.Message
import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.plugin.*
import io.ktor.http.HttpStatusCode
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GenerationSnapshot(val systemPrompt: String, val delivery: EstimatedDeliveryDto)
data class SummarySnapshot(val text: String, val covers: Long)

data class MessageJob(
    val id: Long,
    val messageId: Long,
    val kind: String,
    val payload: String,
    val attempts: Int,
    val leaseToken: String
)

class MessageRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun receipt(conversationId: Long, userId: Long, clientId: String, text: String): MessageResponse? = database { db ->
        db.rows(
            """
            SELECT m.id,m.content_text,j.payload FROM messages m
            JOIN conversations c ON c.id=m.conversation_id AND c.user_id=?
            JOIN message_jobs j ON j.message_id=m.id AND j.kind='generate'
            WHERE m.conversation_id=? AND m.client_message_id=?
            """.trimIndent(),
            userId,
            conversationId,
            clientId
        ) {
            if (it.getString("content_text") != text) throw ApiException(HttpStatusCode.Conflict, "idempotency_conflict", "这份信稿已提交，请刷新后再编辑")
            MessageResponse(it.getLong("id"), "sent", json.decodeFromString<GenerationSnapshot>(it.getString("payload")).delivery, clientId)
        }.singleOrNull()
    }

    suspend fun accept(
        conversationId: Long,
        userId: Long,
        clientId: String,
        text: String,
        snapshot: GenerationSnapshot,
        dailyLimit: Int
    ): MessageResponse = database { db ->
        // Lock owner then conversation: quota and idempotency work across concurrent requests/processes.
        if (db.rows("SELECT id FROM users WHERE id=? FOR UPDATE", userId) { it.getLong(1) }.isEmpty()) notFound()
        val archived = db.rows(
            "SELECT archived FROM conversations WHERE id=? AND user_id=? FOR UPDATE",
            conversationId,
            userId
        ) { it.getBoolean(1) }.singleOrNull() ?: notFound()
        if (archived) throw ApiException(HttpStatusCode.Conflict, "archived", "请先恢复这段通信")
        val prior = db.rows(
            """
            SELECT m.id,m.content_text,j.payload FROM messages m
            JOIN message_jobs j ON j.message_id=m.id AND j.kind='generate'
            WHERE m.conversation_id=? AND m.client_message_id=?
            """.trimIndent(),
            conversationId,
            clientId
        ) {
            Triple(it.getLong("id"), it.getString("content_text"), it.getString("payload"))
        }.singleOrNull()
        if (prior != null) {
            if (prior.second != text) throw ApiException(HttpStatusCode.Conflict, "idempotency_conflict", "这份信稿已提交，请刷新后再编辑")
            return@database MessageResponse(prior.first, "sent", json.decodeFromString<GenerationSnapshot>(prior.third).delivery, clientId)
        }
        val count = db.rows(
            """
            SELECT count(*) FROM messages m JOIN conversations c ON c.id=m.conversation_id
            WHERE c.user_id=? AND m.sender_type='user' AND m.created_at>now()-interval '1 day'
            """.trimIndent(),
            userId
        ) { it.getInt(1) }.single()
        if (count >= dailyLimit) throw ApiException(HttpStatusCode.TooManyRequests, "letter_quota", "今日寄信额度已用完，信稿可以继续保存")
        val id = db.rows(
            """
            INSERT INTO messages(conversation_id,sender_type,content_text,client_message_id,is_delivered,delivered_at)
            VALUES (?,'user',?,?,true,now()) RETURNING id
            """.trimIndent(),
            conversationId,
            text,
            clientId
        ) { it.getLong(1) }.single()
        enqueue(db, id, "generate", json.encodeToString(snapshot))
        db.update("UPDATE conversations SET updated_at=now() WHERE id=?", conversationId)
        MessageResponse(id, "sent", snapshot.delivery, clientId)
    }

    suspend fun findByConversationId(
        conversationId: Long,
        limit: Int = 50,
        beforeId: Long? = null,
        afterId: Long? = null
    ): List<Message> = database { db ->
        val filters = StringBuilder("conversation_id=? AND is_delivered=true")
        val args = mutableListOf<Any?>(conversationId)
        beforeId?.let {
            filters.append(" AND id<?")
            args.add(it)
        }
        afterId?.let {
            filters.append(" AND id>?")
            args.add(it)
        }
        args.add(limit.coerceIn(1, 100))
        val ascending = afterId != null
        val result = db.rows(
            "SELECT * FROM messages WHERE $filters ORDER BY id " +
                (if (ascending) "ASC" else "DESC") + " LIMIT ?",
            *args.toTypedArray()
        ) { it.message() }
        if (ascending) result else result.reversed()
    }

    suspend fun findInternal(id: Long): Message? = database { db ->
        db.rows("SELECT * FROM messages WHERE id=?", id) { it.message() }.singleOrNull()
    }

    suspend fun pending(conversationId: Long): List<PendingMessageItem> = database { db ->
        db.rows(
            """
            SELECT u.id,j.state,j.payload,j.attempts,p.id AS reply_id
            FROM message_jobs j JOIN messages u ON u.id=j.message_id
            LEFT JOIN messages p ON p.reply_to_message_id=u.id
            WHERE u.conversation_id=? AND j.kind='generate'
              AND (j.state<>'done' OR p.is_delivered=false)
            ORDER BY u.id
            """.trimIndent(),
            conversationId
        ) { row ->
            val quote = json.decodeFromString<GenerationSnapshot>(row.getString("payload")).delivery
            val state = if (row.optionalLong("reply_id") != null) {
                "scheduled"
            } else {
                when (row.getString("state")) {
                    "running" -> "generating"
                    else -> row.getString("state")
                }
            }
            PendingMessageItem(
                row.getLong("id"),
                status = state,
                scheduledDeliveryAt = quote.deliverAt,
                delaySeconds = quote.delaySeconds,
                estimatedSecondsRemaining = (Instant.parse(quote.deliverAt).epochSecond - Instant.now().epochSecond).coerceAtLeast(0),
                canRetry = state == "failed" && row.getInt("attempts") < 10
            )
        }
    }

    suspend fun retry(messageId: Long, userId: Long): Boolean = database { db ->
        val owned = db.rows(
            """
            SELECT j.id FROM message_jobs j JOIN messages m ON m.id=j.message_id
            JOIN conversations c ON c.id=m.conversation_id
            WHERE m.id=? AND c.user_id=? AND j.kind='generate'
            """.trimIndent(),
            messageId,
            userId
        ) { it.getLong(1) }.singleOrNull() ?: notFound()
        db.update(
            """
            UPDATE message_jobs SET state='queued',next_attempt_at=now(),last_error_code=NULL
            WHERE id=? AND state='failed' AND attempts<10
            """.trimIndent(),
            owned
        ) > 0
    }

    suspend fun markRead(conversationId: Long, throughId: Long) {
        database {
            it.update(
                """
            UPDATE messages SET read_at=COALESCE(read_at,now())
            WHERE conversation_id=? AND id<=? AND sender_type='poet' AND is_delivered=true
                """.trimIndent(),
                conversationId,
                throughId
            )
        }
    }

    suspend fun getRecentDelivered(
        conversationId: Long,
        limit: Int,
        afterMessageId: Long?,
        beforeMessageId: Long? = null,
        oldestFirst: Boolean = false
    ): List<Message> = database { db ->
        val args = mutableListOf<Any?>(conversationId)
        var condition = "conversation_id=? AND is_delivered=true"
        afterMessageId?.let {
            condition += " AND id>?"
            args.add(it)
        }
        beforeMessageId?.let {
            condition += " AND id<?"
            args.add(it)
        }
        args.add(limit)
        val result = db.rows("SELECT * FROM messages WHERE $condition ORDER BY id " + (if (oldestFirst) "ASC" else "DESC") + " LIMIT ?", *args.toTypedArray()) { it.message() }
        if (oldestFirst) result else result.reversed()
    }

    suspend fun countDelivered(conversationId: Long): Long = countAfter(conversationId, 0)
    suspend fun countAfter(conversationId: Long, afterMessageId: Long): Long = database { db ->
        db.rows(
            "SELECT count(*) FROM messages WHERE conversation_id=? AND id>? AND is_delivered=true",
            conversationId,
            afterMessageId
        ) { it.getLong(1) }.single()
    }

    suspend fun saveSummary(conversationId: Long, summaryText: String, coversUpTo: Long) {
        database { db ->
            db.update(
                "INSERT INTO conversation_summaries(conversation_id,summary_text,covers_up_to) VALUES (?,?,?)",
                conversationId,
                summaryText,
                coversUpTo
            )
        }
    }

    suspend fun getLatestSummary(conversationId: Long): String? = database { db ->
        db.rows(
            "SELECT summary_text FROM conversation_summaries WHERE conversation_id=? ORDER BY covers_up_to DESC LIMIT 1",
            conversationId
        ) { it.getString(1) }.singleOrNull()
    }

    suspend fun summarySnapshot(conversationId: Long, beforeId: Long = Long.MAX_VALUE): SummarySnapshot? = database { db ->
        db.rows(
            "SELECT summary_text,covers_up_to FROM conversation_summaries WHERE conversation_id=? AND covers_up_to<? ORDER BY covers_up_to DESC LIMIT 1",
            conversationId,
            beforeId
        ) { SummarySnapshot(it.getString(1), it.getLong(2)) }.singleOrNull()
    }

    suspend fun completeSummary(job: MessageJob, conversationId: Long, text: String, covers: Long) = database { db ->
        if (!ownsLease(db, job)) return@database
        db.update("INSERT INTO conversation_summaries(conversation_id,summary_text,covers_up_to) VALUES (?,?,?)", conversationId, text, covers)
        finish(db, job)
    }

    suspend fun getLatestSummaryCoversUpTo(conversationId: Long): Long? = database { db ->
        db.rows(
            "SELECT covers_up_to FROM conversation_summaries WHERE conversation_id=? ORDER BY covers_up_to DESC LIMIT 1",
            conversationId
        ) { it.getLong(1) }.singleOrNull()
    }

    suspend fun claim(leaseSeconds: Long): MessageJob? = database { db ->
        db.update("UPDATE message_jobs SET state='failed',last_error_code='RetryLimit',lease_token=NULL,lease_until=NULL WHERE state='running' AND lease_until<now() AND attempts>=10")
        val token = UUID.randomUUID().toString()
        db.rows(
            """
            WITH candidate AS (
                SELECT j.id FROM message_jobs j JOIN messages m ON m.id=j.message_id
                WHERE ((j.state IN ('queued','retrying') AND j.next_attempt_at<=now())
                    OR (j.state='running' AND j.lease_until<now()))
                  AND NOT EXISTS (
                    SELECT 1 FROM message_jobs older JOIN messages om ON om.id=older.message_id
                    WHERE j.kind IN ('generate','summarize') AND older.kind=j.kind
                      AND om.conversation_id=m.conversation_id AND older.id<j.id
                      AND older.state IN ('queued','retrying','running')
                  )
                ORDER BY j.next_attempt_at,j.id FOR UPDATE OF j SKIP LOCKED LIMIT 1
            )
            UPDATE message_jobs j SET state='running',attempts=j.attempts+1,lease_token=?,
                lease_until=now()+(? * interval '1 second'),updated_at=now()
            FROM candidate WHERE j.id=candidate.id RETURNING j.*
            """.trimIndent(),
            token,
            leaseSeconds
        ) {
            MessageJob(
                it.getLong("id"),
                it.getLong("message_id"),
                it.getString("kind"),
                it.getString("payload"),
                it.getInt("attempts"),
                token
            )
        }.singleOrNull()
    }

    suspend fun completeGeneration(job: MessageJob, text: String) = database { db ->
        if (!ownsLease(db, job)) return@database
        val user = db.rows("SELECT * FROM messages WHERE id=?", job.messageId) { it.message() }.singleOrNull() ?: return@database
        val quote = json.decodeFromString<GenerationSnapshot>(job.payload).delivery
        val id = db.rows(
            """
            INSERT INTO messages(conversation_id,sender_type,content_text,is_delivered,
                scheduled_delivery_at,delay_seconds,delay_factors,reply_to_message_id)
            VALUES (?,'poet',?,false,?,?,?::jsonb,?)
            ON CONFLICT(reply_to_message_id) WHERE reply_to_message_id IS NOT NULL DO NOTHING RETURNING id
            """.trimIndent(),
            user.conversationId,
            text,
            Instant.parse(quote.deliverAt),
            quote.delaySeconds,
            json.encodeToString(quote.factors),
            user.id
        ) { it.getLong(1) }.singleOrNull()
        id?.let { enqueue(db, it, "translate") }
        finish(db, job)
    }

    suspend fun completeTranslation(job: MessageJob, translation: String) = database { db ->
        if (!ownsLease(db, job)) return@database
        db.update("UPDATE messages SET translation=? WHERE id=?", translation, job.messageId)
        finish(db, job)
    }

    suspend fun complete(job: MessageJob) = database { db -> if (ownsLease(db, job)) finish(db, job) }

    suspend fun fail(job: MessageJob, code: String) {
        database { db ->
            val delay = (5L shl job.attempts.coerceAtMost(6)).coerceAtMost(300)
            db.update(
                """
                UPDATE message_jobs SET state=?,next_attempt_at=now()+(? * interval '1 second'),
                    last_error_code=?,lease_token=NULL,lease_until=NULL,updated_at=now()
                WHERE id=? AND lease_token=? AND state='running'
                """.trimIndent(),
                if (job.attempts >= 5) "failed" else "retrying",
                delay,
                code.take(50),
                job.id,
                job.leaseToken
            )
        }
    }

    suspend fun deliverDue(): Int = database { db ->
        val ids = db.rows(
            """
            UPDATE messages SET is_delivered=true,delivered_at=now()
            WHERE id IN (
                SELECT id FROM messages WHERE sender_type='poet' AND is_delivered=false
                    AND scheduled_delivery_at<=now() ORDER BY scheduled_delivery_at
                FOR UPDATE SKIP LOCKED LIMIT 50
            ) AND is_delivered=false RETURNING id,conversation_id
            """.trimIndent()
        ) { it.getLong("id") to it.getLong("conversation_id") }
        ids.forEach { (id, conversationId) ->
            enqueue(db, id, "notify")
            enqueue(db, id, "summarize")
            db.update("UPDATE conversations SET updated_at=now() WHERE id=?", conversationId)
        }
        ids.size
    }

    private fun enqueue(db: Connection, messageId: Long, kind: String, payload: String = "{}") {
        db.update(
            "INSERT INTO message_jobs(message_id,kind,payload) VALUES (?,?,?::jsonb) ON CONFLICT(message_id,kind) DO NOTHING",
            messageId,
            kind,
            payload
        )
    }

    private fun ownsLease(db: Connection, job: MessageJob): Boolean = db.rows(
        "SELECT 1 FROM message_jobs WHERE id=? AND lease_token=? AND state='running' FOR UPDATE",
        job.id,
        job.leaseToken
    ) { true }.isNotEmpty()

    private fun finish(db: Connection, job: MessageJob) {
        db.update(
            "UPDATE message_jobs SET state='done',lease_until=NULL,lease_token=NULL,last_error_code=NULL,updated_at=now() WHERE id=?",
            job.id
        )
    }

    private fun ResultSet.message() = Message(
        id = getLong("id"), conversationId = getLong("conversation_id"), senderType = getString("sender_type"),
        contentText = getString("content_text"), contentImageUrl = getString("content_image_url"), translation = getString("translation"),
        isDelivered = getBoolean("is_delivered"), scheduledDeliveryAt = instant("scheduled_delivery_at"),
        deliveredAt = instant("delivered_at"), delaySeconds = getInt("delay_seconds").let { if (wasNull()) null else it },
        delayFactors = getString("delay_factors")?.let { json.decodeFromString<Map<String, String>>(it) },
        createdAt = instant("created_at"), clientMessageId = getString("client_message_id"),
        replyToMessageId = optionalLong("reply_to_message_id"), readAt = instant("read_at")
    )
}
