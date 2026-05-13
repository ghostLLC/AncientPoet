package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.Conversation
import com.ancientpoet.server.repository.ConversationRepository
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.server.repository.UserRepository

class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val poetRepository: PoetRepository,
    private val userRepository: UserRepository,
) {
    suspend fun createConversation(userId: Long, poetId: Long, dynastyId: String?, backgroundSetting: String?): Conversation {
        val poet = poetRepository.findById(poetId)
            ?: throw IllegalArgumentException("Poet not found: $poetId")
        val effectiveDynastyId = dynastyId ?: poet.dynastyId
        return conversationRepository.create(
            userId = userId,
            poetId = poetId,
            mode = "open",
            dynastyId = effectiveDynastyId,
            backgroundSetting = backgroundSetting,
        )
    }

    suspend fun listUserConversations(userId: Long): List<Conversation> {
        return conversationRepository.findByUserId(userId)
    }

    suspend fun getConversation(conversationId: Long): Conversation? {
        return conversationRepository.findById(conversationId)
    }

    suspend fun deleteConversation(conversationId: Long, userId: Long) {
        val conversation = conversationRepository.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        if (conversation.userId != userId) throw SecurityException("Not your conversation")
        conversationRepository.delete(conversationId)
    }
}
