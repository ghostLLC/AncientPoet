package com.ancientpoet.shared.data.local

import com.ancientpoet.shared.db.AncientPoetDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CachedResource(val payload: String, val savedAt: Long)
data class LetterDraft(val clientId: String, val text: String)

interface ResourceCache {
    suspend fun read(owner: Long, key: String): CachedResource?
    suspend fun write(owner: Long, key: String, payload: String, savedAt: Long)
    suspend fun remove(owner: Long, key: String)
    suspend fun draft(owner: Long, conversationId: Long): LetterDraft?
    suspend fun saveDraft(owner: Long, conversationId: Long, draft: LetterDraft)
    suspend fun acknowledge(owner: Long, conversationId: Long, clientId: String)
    suspend fun clear(owner: Long)
}
class SqliteResourceCache(private val database: AncientPoetDb) : ResourceCache {
    private val queries = database.cacheQueries
    override suspend fun read(owner: Long, key: String) = withContext(Dispatchers.Default) {
        queries.readResource(owner, key).executeAsOneOrNull()?.let { CachedResource(it.payload, it.saved_at) }
    }
    override suspend fun write(owner: Long, key: String, payload: String, savedAt: Long) = withContext(Dispatchers.Default) {
        queries.writeResource(owner, key, payload, savedAt)
    }
    override suspend fun remove(owner: Long, key: String) = withContext(Dispatchers.Default) { queries.deleteResource(owner, key) }
    override suspend fun draft(owner: Long, conversationId: Long) = withContext(Dispatchers.Default) {
        queries.readDraft(owner, conversationId).executeAsOneOrNull()?.let { LetterDraft(it.client_id, it.content) }
    }
    override suspend fun saveDraft(owner: Long, conversationId: Long, draft: LetterDraft) = withContext(Dispatchers.Default) {
        queries.writeDraft(owner, conversationId, draft.clientId, draft.text)
    }
    override suspend fun acknowledge(owner: Long, conversationId: Long, clientId: String) = withContext(Dispatchers.Default) {
        queries.acknowledgeDraft(owner, conversationId, clientId)
    }
    override suspend fun clear(owner: Long) = withContext(Dispatchers.Default) {
        database.transaction {
            queries.clearResources(owner)
            queries.clearDrafts(owner)
        }
    }
}
