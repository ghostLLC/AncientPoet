package com.ancientpoet.android

import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.shared.auth.*
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.local.*
import io.ktor.client.HttpClient

class TestSession(var tokens: AuthTokens? = AuthTokens("access", "refresh", 1)) : SessionStore {
    override suspend fun load() = tokens
    override suspend fun save(tokens: AuthTokens) {
        this.tokens = tokens
    }
    override suspend fun clear() {
        tokens = null
    }
}
class TestCache : ResourceCache {
    private val resources = mutableMapOf<Pair<Long, String>, CachedResource>()
    private val drafts = mutableMapOf<Pair<Long, Long>, LetterDraft>()
    override suspend fun read(owner: Long, key: String) = resources[owner to key]
    override suspend fun write(owner: Long, key: String, payload: String, savedAt: Long) {
        resources[owner to key] = CachedResource(payload, savedAt)
    }
    override suspend fun remove(owner: Long, key: String) {
        resources.remove(owner to key)
    }
    override suspend fun draft(owner: Long, conversationId: Long) = drafts[owner to conversationId]
    override suspend fun saveDraft(owner: Long, conversationId: Long, draft: LetterDraft) {
        drafts[owner to conversationId] = draft
    }
    override suspend fun acknowledge(owner: Long, conversationId: Long, clientId: String) {
        if (drafts[owner to conversationId]?.clientId == clientId) drafts.remove(owner to conversationId)
    }
    override suspend fun clear(owner: Long) {
        resources.keys.removeAll { it.first == owner }
        drafts.keys.removeAll { it.first == owner }
    }
}
fun testRepository(client: HttpClient) = AppRepository(AncientPoetApi(client), TestCache(), TestSession())
