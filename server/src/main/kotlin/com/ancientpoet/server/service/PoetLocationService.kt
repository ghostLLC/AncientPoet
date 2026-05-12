package com.ancientpoet.server.service

import com.ancientpoet.server.model.domain.PoetMovement
import com.ancientpoet.server.repository.PoetRepository

class PoetLocationService(private val poetRepository: PoetRepository) {

    suspend fun getPoetLocation(poetId: Long, year: Int): PoetMovement? {
        return poetRepository.findLocation(poetId, year)
    }

    suspend fun getDefaultYear(poetId: Long): Int {
        val poet = poetRepository.findById(poetId)
            ?: throw IllegalArgumentException("Poet not found: $poetId")
        // Default to the poet's most productive period (~age 40-45)
        return poet.birthYear + 42
    }
}
