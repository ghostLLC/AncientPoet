package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.Conversation
import com.ancientpoet.server.model.domain.PoetLifeEvent
import com.ancientpoet.server.plugin.invalid
import com.ancientpoet.server.plugin.notFound
import com.ancientpoet.server.repository.*

class StorylineService(
    private val poetRepository: PoetRepository,
    private val conversationRepository: ConversationRepository,
    private val poetLocationService: PoetLocationService
) {
    suspend fun jumpToYear(conversationId: Long, targetYear: Int, userId: Long): StorylineState {
        val conversation = conversationRepository.requireOwned(conversationId, userId)
        val poet = poetRepository.findById(conversation.poetId) ?: notFound()
        if (targetYear !in poet.birthYear..poet.deathYear) invalid("请选择诗人生平内的年代")
        if (poetLocationService.getPoetLocation(poet.id, targetYear) == null) invalid("这个年代的行迹暂缺")
        conversationRepository.updateStorylineYear(conversationId, targetYear, userId)
        return state(conversation.copy(storylineCurrentYear = targetYear))
    }

    suspend fun getState(conversationId: Long, userId: Long): StorylineState = state(conversationRepository.requireOwned(conversationId, userId))

    private suspend fun state(conversation: Conversation): StorylineState {
        val poet = poetRepository.findById(conversation.poetId) ?: notFound()
        val year = conversation.storylineCurrentYear ?: poetLocationService.getDefaultYear(poet.id)
        val location = poetLocationService.getPoetLocation(poet.id, year)
        val event = eventAtYear(poet.id, year)
        return StorylineState(
            year, year - poet.birthYear, location?.locationName ?: "行迹暂缺", location?.lat ?: 0.0, location?.lng ?: 0.0,
            event?.title, event?.description, event?.eventType ?: location?.eventType ?: "normal",
            event?.delayMultiplier ?: if (location?.eventType in setOf("war", "exile")) 1.5 else 1.0
        )
    }

    internal suspend fun getCurrentEvent(conversationId: Long): PoetLifeEvent? {
        val conversation = conversationRepository.findById(conversationId) ?: return null
        return eventAtYear(conversation.poetId, conversation.storylineCurrentYear ?: poetLocationService.getDefaultYear(conversation.poetId))
    }

    private suspend fun eventAtYear(poetId: Long, year: Int) = poetRepository.findLifeEventsByPoetId(poetId).firstOrNull { it.year == year }
}

data class StorylineState(
    val currentYear: Int,
    val poetAge: Int,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val activeEvent: String?,
    val eventDescription: String?,
    val eventType: String,
    val delayMultiplier: Double
)
