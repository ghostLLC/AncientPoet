package com.ancientpoet.android.data

import com.ancientpoet.shared.auth.SessionStore
import com.ancientpoet.shared.contract.*
import com.ancientpoet.shared.data.api.*
import com.ancientpoet.shared.data.local.*
import io.ktor.client.request.setBody
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** All private caches are partitioned by account, and late responses cannot switch their owner. */
class AppRepository(
    @PublishedApi internal val api: AncientPoetApi,
    @PublishedApi internal val cache: ResourceCache,
    @PublishedApi internal val sessions: SessionStore
) {
    @PublishedApi internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val draftsMutex = Mutex()
    suspend fun owner(): Long = sessions.load()?.userId ?: 0
    suspend inline fun <reified T> cached(path: String, public: Boolean = false): T? {
        val id = if (public) 0 else owner()
        if (!public && id == 0L) return null
        return optionalCache { cache.read(id, path)?.let { json.decodeFromString<T>(it.payload) } }
    }
    suspend inline fun <reified T> fetch(path: String, public: Boolean = false): ApiResult<T> {
        val id = if (public) 0 else owner()
        if (!public && id == 0L) return ApiResult.Failure(ApiErrorKind.UNAUTHORIZED, "请先登录", false, 401)
        val result = api.get<T>(path, if (public) null else id)
        if (!public && id != owner()) return ApiResult.Failure(ApiErrorKind.UNAUTHORIZED, "账号已切换", false, 401)
        if (result is ApiResult.Success) optionalCache { cache.write(id, path, json.encodeToString(result.value), System.currentTimeMillis()) }
        return result
    }
    suspend fun draft(conversationId: Long): LetterDraft? {
        val id = owner()
        return if (id == 0L) null else cache.draft(id, conversationId)
    }
    suspend fun saveDraft(conversationId: Long, text: String): LetterDraft = draftsMutex.withLock {
        val id = owner()
        check(id > 0)
        val previous = cache.draft(id, conversationId)
        val draft = if (previous?.text == text) previous else LetterDraft(UUID.randomUUID().toString(), text)
        cache.saveDraft(id, conversationId, draft)
        draft
    }
    suspend fun send(conversationId: Long, draft: LetterDraft): ApiResult<MessageResponse> {
        val id = owner()
        if (id == 0L) return ApiResult.Failure(ApiErrorKind.UNAUTHORIZED, "请先登录", false, 401)
        val result = api.post<MessageResponse>("conversations/" + conversationId + "/messages", id) {
            setBody(SendMessageRequest(draft.text, clientMessageId = draft.clientId))
        }
        if (result is ApiResult.Success) {
            optionalCache { draftsMutex.withLock { cache.acknowledge(id, conversationId, draft.clientId) } }
            changes.tryEmit(Unit)
        }
        return if (owner() == id) result else ApiResult.Failure(ApiErrorKind.UNAUTHORIZED, "账号已切换", false, 401)
    }
    suspend fun create(poetId: Long, year: Int?, background: String? = null): ApiResult<ConversationResponse> {
        val id = owner()
        val result = api.post<ConversationResponse>("conversations", id) {
            setBody(CreateConversationRequest(poetId, startYear = year, backgroundSetting = background, mode = if (year == null) "open" else "storyline"))
        }
        if (result is ApiResult.Success) changes.tryEmit(Unit)
        return bound(id, result)
    }
    suspend fun archive(conversationId: Long, archived: Boolean): ApiResult<Unit> {
        val id = owner()
        val result = api.post<Unit>("conversations/" + conversationId + "/archive", id) { setBody(mapOf("archived" to archived)) }
        if (result is ApiResult.Success) changes.tryEmit(Unit)
        return bound(id, result)
    }
    suspend fun deleteConversation(conversationId: Long): ApiResult<Unit> {
        val id = owner()
        val result = api.delete<Unit>("conversations/" + conversationId, id)
        if (result is ApiResult.Success) {
            optionalCache {
                cache.remove(id, "conversations/" + conversationId)
                cache.remove(id, "conversations/" + conversationId + "/messages")
                draftsMutex.withLock { cache.draft(id, conversationId)?.let { cache.acknowledge(id, conversationId, it.clientId) } }
            }
            changes.tryEmit(Unit)
        }
        return bound(id, result)
    }
    suspend fun markRead(conversationId: Long, throughId: Long): Boolean {
        if (throughId <= 0) return true
        val success = api.post<Unit>("conversations/" + conversationId + "/read", owner()) { setBody(mapOf("throughId" to throughId)) } is ApiResult.Success
        if (success) changes.tryEmit(Unit)
        return success
    }
    suspend fun retryReply(conversationId: Long, messageId: Long) = api.post<Unit>("conversations/" + conversationId + "/messages/" + messageId + "/retry", owner())
    suspend fun logout(): ApiResult<Unit> {
        val id = owner()
        val result = api.post<Unit>("auth/logout", id)
        if (owner() == id) sessions.clear()
        changes.tryEmit(Unit)
        return result
    }
    suspend fun deleteAccount(): ApiResult<Unit> {
        val id = owner()
        val result = api.delete<Unit>("user/account", id)
        if (result is ApiResult.Success) {
            optionalCache { cache.clear(id) }
            if (owner() == id) sessions.clear()
            changes.tryEmit(Unit)
        }
        return result
    }
    suspend fun exportAccount(): ApiResult<kotlinx.serialization.json.JsonObject> {
        val id = owner()
        return bound(id, api.get("user/export", id))
    }
    private suspend fun <T> bound(id: Long, result: ApiResult<T>): ApiResult<T> = if (id > 0 && owner() == id) result else ApiResult.Failure(ApiErrorKind.UNAUTHORIZED, "账号已切换", false, 401)

    @PublishedApi internal suspend inline fun <T> optionalCache(block: () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
