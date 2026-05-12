package com.ancientpoet.shared.domain.repository

import com.ancientpoet.shared.domain.model.*

interface AuthRepository {
    suspend fun sendSms(phone: String): Boolean
    suspend fun verifySms(phone: String, code: String): TokenData?
    suspend fun refreshToken(refreshToken: String): String?
}

interface PoetRepository {
    suspend fun getPoets(): List<Poet>
    suspend fun getPoetDetail(poetId: Long): Poet?
    suspend fun getPoetLocation(poetId: Long, year: Int): Location?
    suspend fun getPoems(poetId: Long): List<Poem>
    suspend fun getLifeEvents(poetId: Long): List<LifeEvent>
}

interface ConversationRepository {
    suspend fun createConversation(poetId: Long, dynastyId: String?, backgroundSetting: String?): Conversation
    suspend fun getConversations(): List<Conversation>
    suspend fun getConversation(conversationId: Long): Conversation?
    suspend fun deleteConversation(conversationId: Long)
}

interface MessageRepository {
    suspend fun sendMessage(conversationId: Long, text: String?, imageUrl: String?): EstimatedDelivery
    suspend fun getMessages(conversationId: Long): List<Message>
    suspend fun getPendingMessages(conversationId: Long): List<Message>
}

data class TokenData(val accessToken: String, val refreshToken: String, val expiresIn: Long, val userId: Long)
data class Poem(val id: Long, val title: String, val content: String, val yearWritten: Int?, val context: String?, val translation: String?)
data class LifeEvent(val id: Long, val year: Int, val age: Int, val title: String, val description: String, val eventType: String, val delayMultiplier: Double)
