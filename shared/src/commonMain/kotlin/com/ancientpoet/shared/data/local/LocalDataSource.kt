package com.ancientpoet.shared.data.local

import com.ancientpoet.shared.domain.model.Message
import com.ancientpoet.shared.domain.model.Poet

interface LocalDataSource {
    suspend fun cachePoets(poets: List<Poet>)
    suspend fun getCachedPoets(): List<Poet>
    suspend fun cacheMessages(conversationId: Long, messages: List<Message>)
    suspend fun getCachedMessages(conversationId: Long): List<Message>
    suspend fun clearMessages(conversationId: Long)
}

// In-memory fallback implementation (SQLDelight requires platform drivers)
class InMemoryLocalDataSource : LocalDataSource {
    private val poetCache = mutableListOf<Poet>()
    private val messageCache = mutableMapOf<Long, MutableList<Message>>()

    override suspend fun cachePoets(poets: List<Poet>) {
        poetCache.clear()
        poetCache.addAll(poets)
    }

    override suspend fun getCachedPoets(): List<Poet> = poetCache.toList()

    override suspend fun cacheMessages(conversationId: Long, messages: List<Message>) {
        val existing = messageCache[conversationId] ?: mutableListOf()
        existing.addAll(messages)
        messageCache[conversationId] = existing
    }

    override suspend fun getCachedMessages(conversationId: Long): List<Message> {
        return messageCache[conversationId]?.toList() ?: emptyList()
    }

    override suspend fun clearMessages(conversationId: Long) {
        messageCache.remove(conversationId)
    }
}
