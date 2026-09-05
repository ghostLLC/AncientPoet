package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.Conversation
import com.ancientpoet.server.plugin.invalid
import com.ancientpoet.server.plugin.notFound
import com.ancientpoet.server.repository.*

class ConversationService(
    private val conversations: ConversationRepository,
    private val poets: PoetRepository,
    private val userRepository: UserRepository,
    private val locations: PoetLocationService
) {
    suspend fun createConversation(
        userId: Long,
        poetId: Long,
        dynastyId: String?,
        backgroundSetting: String?,
        startYear: Int? = null,
        mode: String = "open"
    ): Conversation {
        val poet = poets.findById(poetId) ?: notFound()
        if (dynastyId != null && dynastyId != poet.dynastyId) invalid("朝代与诗人不一致")
        if (mode !in setOf("open", "storyline")) invalid("通信模式无效")
        if ((backgroundSetting?.length ?: 0) > 2000) invalid("背景介绍最多 2000 字")
        val year = startYear ?: locations.getDefaultYear(poetId)
        if (year !in poet.birthYear..poet.deathYear) invalid("请选择诗人生平内的年代")
        if (locations.getPoetLocation(poetId, year) == null) invalid("这个年代的行迹暂缺")
        return conversations.create(userId, poetId, mode, poet.dynastyId, backgroundSetting?.trim(), year)
    }
    suspend fun listUserConversations(userId: Long, archived: Boolean = false) = conversations.findByUserId(userId, archived)
    suspend fun getConversation(conversationId: Long, userId: Long) = conversations.requireOwned(conversationId, userId)
    suspend fun deleteConversation(conversationId: Long, userId: Long) = conversations.delete(conversationId, userId)
    suspend fun archive(conversationId: Long, userId: Long, archived: Boolean) = conversations.archive(conversationId, userId, archived)
}
