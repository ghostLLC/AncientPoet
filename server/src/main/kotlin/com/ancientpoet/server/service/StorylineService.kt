package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.Conversation
import com.ancientpoet.server.model.domain.PoetLifeEvent
import com.ancientpoet.server.repository.ConversationRepository
import com.ancientpoet.server.repository.PoetRepository

class StorylineService(
    private val poetRepository: PoetRepository,
    private val conversationRepository: ConversationRepository,
    private val poetLocationService: PoetLocationService,
) {
    suspend fun startStoryline(conversation: Conversation, startYear: Int): StorylineState {
        val poet = poetRepository.findById(conversation.poetId)
            ?: throw IllegalArgumentException("Poet not found")
        if (startYear < poet.birthYear || startYear > poet.deathYear) {
            throw IllegalArgumentException("Year $startYear out of poet's lifetime ${poet.birthYear}-${poet.deathYear}")
        }
        val location = poetLocationService.getPoetLocation(conversation.poetId, startYear)
        val event = getEventAtYear(conversation.poetId, startYear)
        return StorylineState(
            currentYear = startYear,
            poetAge = startYear - poet.birthYear,
            locationName = location?.locationName ?: "未知",
            lat = location?.lat ?: 0.0,
            lng = location?.lng ?: 0.0,
            activeEvent = event?.title,
            eventDescription = event?.description,
            eventType = event?.eventType ?: "normal",
            delayMultiplier = event?.delayMultiplier ?: 1.0,
        )
    }

    suspend fun advanceYear(conversationId: Long): StorylineState {
        val conversation = conversationRepository.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        val poet = poetRepository.findById(conversation.poetId)
            ?: throw IllegalArgumentException("Poet not found")

        val currentYear = conversation.storylineCurrentYear ?: poet.birthYear + 42
        val nextYear = minOf(currentYear + 1, poet.deathYear)

        conversationRepository.updateStorylineYear(conversationId, nextYear)

        return getState(conversationId)
    }

    suspend fun jumpToYear(conversationId: Long, targetYear: Int): StorylineState {
        val conversation = conversationRepository.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        val poet = poetRepository.findById(conversation.poetId)
            ?: throw IllegalArgumentException("Poet not found")
        if (targetYear < poet.birthYear || targetYear > poet.deathYear) {
            throw IllegalArgumentException("Year out of poet's lifetime")
        }
        conversationRepository.updateStorylineYear(conversationId, targetYear)
        return getState(conversationId)
    }

    suspend fun getState(conversationId: Long): StorylineState {
        val conversation = conversationRepository.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        val poet = poetRepository.findById(conversation.poetId)
            ?: throw IllegalArgumentException("Poet not found")

        val year = conversation.storylineCurrentYear ?: poet.birthYear + 42
        val location = poetLocationService.getPoetLocation(conversation.poetId, year)
        val event = getEventAtYear(conversation.poetId, year)

        return StorylineState(
            currentYear = year,
            poetAge = year - poet.birthYear,
            locationName = location?.locationName ?: "未知",
            lat = location?.lat ?: 0.0,
            lng = location?.lng ?: 0.0,
            activeEvent = event?.title,
            eventDescription = event?.description,
            eventType = event?.eventType ?: "normal",
            delayMultiplier = event?.delayMultiplier ?: 1.0,
        )
    }

    suspend fun getCurrentEvent(conversationId: Long): PoetLifeEvent? {
        val conversation = conversationRepository.findById(conversationId) ?: return null
        val year = conversation.storylineCurrentYear ?: return null
        return getEventAtYear(conversation.poetId, year)
    }

    private suspend fun getEventAtYear(poetId: Long, year: Int): PoetLifeEvent? {
        val events = poetRepository.findLifeEventsByPoetId(poetId)
        return events.firstOrNull { it.year == year }
    }
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
    val delayMultiplier: Double,
)
