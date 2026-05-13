package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.ConversationSummariesTable
import com.ancientpoet.server.model.db.MessagesTable
import com.ancientpoet.server.model.domain.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class MessageRepository {
    suspend fun create(conversationId: Long, senderType: String, contentText: String?, contentImageUrl: String?, translation: String?, scheduledDeliveryAt: Instant?, delaySeconds: Int?, delayFactors: Map<String, String>?): Message = withContext(Dispatchers.IO) {
        transaction {
            val result = MessagesTable.insert {
                it[MessagesTable.conversationId] = conversationId
                it[MessagesTable.senderType] = senderType
                it[MessagesTable.contentText] = contentText
                it[MessagesTable.contentImageUrl] = contentImageUrl
                it[MessagesTable.translation] = translation
                if (scheduledDeliveryAt != null) it[MessagesTable.scheduledDeliveryAt] = scheduledDeliveryAt.toString()
                it[MessagesTable.delaySeconds] = delaySeconds
                if (delayFactors != null) it[MessagesTable.delayFactors] = delayFactors.toString()
            }
            val id = result[MessagesTable.id]
            Message(id = id, conversationId = conversationId, senderType = senderType, contentText = contentText, contentImageUrl = contentImageUrl, translation = translation, isDelivered = false, scheduledDeliveryAt = scheduledDeliveryAt?.toString(), deliveredAt = null, delaySeconds = delaySeconds, delayFactors = delayFactors, createdAt = null)
        }
    }

    suspend fun findByConversationId(conversationId: Long, limit: Int = 50): List<Message> = withContext(Dispatchers.IO) {
        transaction {
            MessagesTable.selectAll().where { (MessagesTable.conversationId eq conversationId) and (MessagesTable.isDelivered eq true) }
                .orderBy(MessagesTable.createdAt).limit(limit).map { it.toMessage() }
        }
    }

    suspend fun findPendingByConversationId(conversationId: Long): List<Message> = withContext(Dispatchers.IO) {
        transaction {
            MessagesTable.selectAll().where { (MessagesTable.conversationId eq conversationId) and (MessagesTable.isDelivered eq false) }
                .orderBy(MessagesTable.scheduledDeliveryAt).map { it.toMessage() }
        }
    }

    suspend fun markDelivered(messageId: Long): Message? = withContext(Dispatchers.IO) {
        transaction {
            val now = Instant.now()
            MessagesTable.update({ MessagesTable.id eq messageId }) { it[isDelivered] = true; it[deliveredAt] = now.toString() }
            MessagesTable.selectAll().where { MessagesTable.id eq messageId }.singleOrNull()?.toMessage()
        }
    }

    suspend fun countDelivered(conversationId: Long): Long = withContext(Dispatchers.IO) {
        transaction { MessagesTable.selectAll().where { (MessagesTable.conversationId eq conversationId) and (MessagesTable.isDelivered eq true) }.count() }
    }

    suspend fun countAfter(conversationId: Long, afterMessageId: Long): Long = withContext(Dispatchers.IO) {
        transaction { MessagesTable.selectAll().where { (MessagesTable.conversationId eq conversationId) and (MessagesTable.id greater afterMessageId) }.count() }
    }

    suspend fun getRecentDelivered(conversationId: Long, limit: Int, afterMessageId: Long?): List<Message> = withContext(Dispatchers.IO) {
        transaction {
            var query = MessagesTable.selectAll().where { MessagesTable.conversationId eq conversationId }
            if (afterMessageId != null) query = query.andWhere { MessagesTable.id greater afterMessageId }
            query.orderBy(MessagesTable.createdAt).limit(limit).map { it.toMessage() }
        }
    }

    suspend fun saveSummary(conversationId: Long, summaryText: String, coversUpTo: Long) {
        withContext(Dispatchers.IO) { transaction { ConversationSummariesTable.insert { it[ConversationSummariesTable.conversationId] = conversationId; it[ConversationSummariesTable.summaryText] = summaryText; it[ConversationSummariesTable.coversUpTo] = coversUpTo } } }
    }

    suspend fun getLatestSummary(conversationId: Long): String? = withContext(Dispatchers.IO) {
        transaction { ConversationSummariesTable.selectAll().where { ConversationSummariesTable.conversationId eq conversationId }.orderBy(ConversationSummariesTable.createdAt, SortOrder.DESC).firstOrNull()?.get(ConversationSummariesTable.summaryText) }
    }

    suspend fun getLatestSummaryCoversUpTo(conversationId: Long): Long? = withContext(Dispatchers.IO) {
        transaction { ConversationSummariesTable.selectAll().where { ConversationSummariesTable.conversationId eq conversationId }.orderBy(ConversationSummariesTable.createdAt, SortOrder.DESC).firstOrNull()?.get(ConversationSummariesTable.coversUpTo) }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toMessage() = Message(
        id = this[MessagesTable.id], conversationId = this[MessagesTable.conversationId],
        senderType = this[MessagesTable.senderType], contentText = this[MessagesTable.contentText],
        contentImageUrl = this[MessagesTable.contentImageUrl], translation = this[MessagesTable.translation],
        isDelivered = this[MessagesTable.isDelivered],
        scheduledDeliveryAt = this[MessagesTable.scheduledDeliveryAt],
        deliveredAt = this[MessagesTable.deliveredAt], delaySeconds = this[MessagesTable.delaySeconds],
        delayFactors = null, createdAt = this[MessagesTable.createdAt],
    )
}
