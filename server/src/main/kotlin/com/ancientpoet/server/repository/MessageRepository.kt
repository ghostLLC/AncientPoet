package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.ConversationSummariesTable
import com.ancientpoet.server.model.db.MessagesTable
import com.ancientpoet.server.model.domain.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class MessageRepository {
    suspend fun create(
        conversationId: Long, senderType: String, contentText: String?,
        contentImageUrl: String?, translation: String?,
        scheduledDeliveryAt: Instant?, delaySeconds: Int?,
        delayFactors: Map<String, String>?,
    ): Message = withContext(Dispatchers.IO) {
        transaction {
            val id = MessagesTable.insertAndGetId {
                it[MessagesTable.conversationId] = conversationId
                it[MessagesTable.senderType] = senderType
                it[MessagesTable.contentText] = contentText
                it[MessagesTable.contentImageUrl] = contentImageUrl
                it[MessagesTable.translation] = translation
                if (scheduledDeliveryAt != null) {
                    it[MessagesTable.scheduledDeliveryAt] = scheduledDeliveryAt
                }
                it[MessagesTable.delaySeconds] = delaySeconds
                if (delayFactors != null) {
                    it[MessagesTable.delayFactors] = delayFactors
                }
            }
            Message(
                id = id.value, conversationId = conversationId,
                senderType = senderType, contentText = contentText,
                contentImageUrl = contentImageUrl, translation = translation,
                isDelivered = false, scheduledDeliveryAt = scheduledDeliveryAt?.toString(),
                deliveredAt = null, delaySeconds = delaySeconds,
                delayFactors = delayFactors, createdAt = null,
            )
        }
    }

    suspend fun findByConversationId(conversationId: Long, limit: Int = 50): List<Message> =
        withContext(Dispatchers.IO) {
            transaction {
                MessagesTable.select {
                    (MessagesTable.conversationId eq conversationId) and
                    (MessagesTable.isDelivered eq true)
                }.orderBy(MessagesTable.createdAt)
                    .limit(limit)
                    .map { it.toMessage() }
            }
        }

    suspend fun findPendingByConversationId(conversationId: Long): List<Message> =
        withContext(Dispatchers.IO) {
            transaction {
                MessagesTable.select {
                    (MessagesTable.conversationId eq conversationId) and
                    (MessagesTable.isDelivered eq false)
                }.orderBy(MessagesTable.scheduledDeliveryAt)
                    .map { it.toMessage() }
            }
        }

    suspend fun markDelivered(messageId: Long): Message? = withContext(Dispatchers.IO) {
        transaction {
            val now = Instant.now()
            MessagesTable.update({ MessagesTable.id eq messageId }) {
                it[isDelivered] = true
                it[deliveredAt] = now
            }
            MessagesTable.select { MessagesTable.id eq messageId }
                .singleOrNull()?.toMessage()
        }
    }

    suspend fun countDelivered(conversationId: Long): Long = withContext(Dispatchers.IO) {
        transaction {
            MessagesTable.select {
                (MessagesTable.conversationId eq conversationId) and
                (MessagesTable.isDelivered eq true)
            }.count()
        }
    }

    suspend fun countAfter(conversationId: Long, afterMessageId: Long): Long =
        withContext(Dispatchers.IO) {
            transaction {
                MessagesTable.select {
                    (MessagesTable.conversationId eq conversationId) and
                    (MessagesTable.id greater afterMessageId)
                }.count()
            }
        }

    suspend fun getRecentDelivered(conversationId: Long, limit: Int, afterMessageId: Long?): List<Message> =
        withContext(Dispatchers.IO) {
            transaction {
                var query = MessagesTable.select { MessagesTable.conversationId eq conversationId }
                if (afterMessageId != null) {
                    query = query.andWhere { MessagesTable.id greater afterMessageId }
                }
                query.orderBy(MessagesTable.createdAt)
                    .limit(limit)
                    .map { it.toMessage() }
            }
        }

    suspend fun saveSummary(conversationId: Long, summaryText: String, coversUpTo: Long) {
        withContext(Dispatchers.IO) {
            transaction {
                ConversationSummariesTable.insertAndGetId {
                    it[ConversationSummariesTable.conversationId] = conversationId
                    it[ConversationSummariesTable.summaryText] = summaryText
                    it[ConversationSummariesTable.coversUpTo] = coversUpTo
                }
            }
        }
    }

    suspend fun getLatestSummary(conversationId: Long): String? = withContext(Dispatchers.IO) {
        transaction {
            ConversationSummariesTable.select {
                ConversationSummariesTable.conversationId eq conversationId
            }.orderBy(ConversationSummariesTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .firstOrNull()
                ?.get(ConversationSummariesTable.summaryText)
        }
    }

    suspend fun getLatestSummaryCoversUpTo(conversationId: Long): Long? = withContext(Dispatchers.IO) {
        transaction {
            ConversationSummariesTable.select {
                ConversationSummariesTable.conversationId eq conversationId
            }.orderBy(ConversationSummariesTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .firstOrNull()
                ?.get(ConversationSummariesTable.coversUpTo)?.value
        }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toMessage() = Message(
        id = this[MessagesTable.id].value,
        conversationId = this[MessagesTable.conversationId].value,
        senderType = this[MessagesTable.senderType],
        contentText = this[MessagesTable.contentText],
        contentImageUrl = this[MessagesTable.contentImageUrl],
        translation = this[MessagesTable.translation],
        isDelivered = this[MessagesTable.isDelivered],
        scheduledDeliveryAt = this[MessagesTable.scheduledDeliveryAt]?.toString(),
        deliveredAt = this[MessagesTable.deliveredAt]?.toString(),
        delaySeconds = this[MessagesTable.delaySeconds],
        delayFactors = null,
        createdAt = this[MessagesTable.createdAt]?.toString(),
    )
}
