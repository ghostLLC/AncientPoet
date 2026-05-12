package com.ancientpoet.shared.domain.usecase

import com.ancientpoet.shared.domain.model.*
import com.ancientpoet.shared.domain.repository.*
import com.ancientpoet.shared.util.DistanceUtil

class SendMessageUseCase(private val messageRepo: MessageRepository) {
    suspend operator fun invoke(conversationId: Long, text: String?, imageUrl: String? = null): EstimatedDelivery {
        return messageRepo.sendMessage(conversationId, text, imageUrl)
    }
}

class GetConversationsUseCase(private val conversationRepo: ConversationRepository) {
    suspend operator fun invoke(): List<Conversation> = conversationRepo.getConversations()
}

class CalculateDelayUseCase {
    operator fun invoke(userLat: Double, userLng: Double, poetLat: Double, poetLng: Double): DelayEstimate {
        val distanceKm = DistanceUtil.haversineDistance(userLat, userLng, poetLat, poetLng)
        val hours = when {
            distanceKm < 30 -> 2.0
            distanceKm < 150 -> 24.0
            else -> (distanceKm / 80.0) * 24.0
        }
        return DelayEstimate(distanceKm = distanceKm, estimatedHours = hours)
    }
}

data class DelayEstimate(val distanceKm: Double, val estimatedHours: Double)

class BrowsePoetsUseCase(private val poetRepo: PoetRepository) {
    suspend operator fun invoke(): List<Poet> = poetRepo.getPoets()
}
