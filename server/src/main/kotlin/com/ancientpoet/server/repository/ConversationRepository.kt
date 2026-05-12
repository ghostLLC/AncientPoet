package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.ConversationsTable
import com.ancientpoet.server.model.db.DynastiesTable
import com.ancientpoet.server.model.db.PoetsTable
import com.ancientpoet.server.model.domain.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ConversationRepository {
    suspend fun create(
        userId: Long, poetId: Long, mode: String, dynastyId: String,
        backgroundSetting: String?, storylineStartYear: Int? = null,
    ): Conversation = withContext(Dispatchers.IO) {
        transaction {
            val id = ConversationsTable.insertAndGetId {
                it[ConversationsTable.userId] = userId
                it[ConversationsTable.poetId] = poetId
                it[ConversationsTable.mode] = mode
                it[ConversationsTable.dynastyId] = dynastyId
                it[ConversationsTable.backgroundSetting] = backgroundSetting
                if (storylineStartYear != null) {
                    it[ConversationsTable.storylineCurrentYear] = storylineStartYear
                }
            }
            findByIdInternal(id.value)!!
        }
    }

    suspend fun updateStorylineYear(conversationId: Long, year: Int) {
        withContext(Dispatchers.IO) {
            transaction {
                ConversationsTable.update({ ConversationsTable.id eq conversationId }) {
                    it[ConversationsTable.storylineCurrentYear] = year
                    it[ConversationsTable.updatedAt] = java.time.Instant.now()
                }
            }
        }
    }

    suspend fun findByUserId(userId: Long): List<Conversation> = withContext(Dispatchers.IO) {
        transaction {
            ConversationsTable
                .innerJoin(PoetsTable)
                .select { ConversationsTable.userId eq userId }
                .orderBy(ConversationsTable.updatedAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .map { it.toConversation() }
        }
    }

    suspend fun findById(conversationId: Long): Conversation? = withContext(Dispatchers.IO) {
        transaction { findByIdInternal(conversationId) }
    }

    suspend fun delete(conversationId: Long) {
        withContext(Dispatchers.IO) {
            transaction {
                ConversationsTable.deleteWhere { ConversationsTable.id eq conversationId }
            }
        }
    }

    private fun findByIdInternal(id: Long): Conversation? {
        return ConversationsTable
            .innerJoin(PoetsTable)
            .select { ConversationsTable.id eq id }
            .singleOrNull()
            ?.toConversation()
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toConversation() = Conversation(
        id = this[ConversationsTable.id].value,
        userId = this[ConversationsTable.userId].value,
        poetId = this[ConversationsTable.poetId].value,
        poetName = this[PoetsTable.name],
        mode = this[ConversationsTable.mode],
        dynastyId = this[ConversationsTable.dynastyId],
        storylineCurrentYear = this[ConversationsTable.storylineCurrentYear],
        storylineCompleted = this[ConversationsTable.storylineCompleted],
        backgroundSetting = this[ConversationsTable.backgroundSetting],
    )
}
