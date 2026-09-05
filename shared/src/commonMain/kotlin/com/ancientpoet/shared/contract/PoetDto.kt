package com.ancientpoet.shared.contract

import kotlinx.serialization.Serializable

@Serializable
data class PoetListResponse(
    val poets: List<PoetItem>
)

@Serializable
data class PoetItem(
    val id: Long,
    val name: String,
    val courtesyName: String? = null,
    val artName: String? = null,
    val dynastyId: String,
    val dynastyName: String? = null,
    val birthYear: Int,
    val deathYear: Int,
    val isFree: Boolean,
    val portraitUrl: String? = null
)

@Serializable
data class PoetDetailResponse(
    val id: Long,
    val name: String,
    val courtesyName: String? = null,
    val artName: String? = null,
    val dynastyId: String,
    val dynastyName: String? = null,
    val birthYear: Int,
    val deathYear: Int,
    val biographySummary: String? = null,
    val personalityProfile: PersonalityProfileDto? = null,
    val writingStyle: String,
    val portraitUrl: String? = null,
    val isFree: Boolean
)

@Serializable
data class PersonalityProfileDto(
    val traits: List<String>,
    val mbti: String? = null,
    val speakingStyle: String? = null
)

@Serializable
data class PoetLocationResponse(
    val poetId: Long,
    val poetName: String,
    val year: Int,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val eventDescription: String? = null,
    val eventType: String? = null
)

@Serializable
data class PoemResponse(
    val id: Long,
    val title: String,
    val content: String,
    val yearWritten: Int? = null,
    val context: String? = null,
    val translation: String? = null,
    val appreciation: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class LifeEventResponse(
    val id: Long,
    val year: Int,
    val age: Int,
    val title: String,
    val description: String,
    val locationName: String? = null,
    val eventType: String,
    val delayMultiplier: Double
)
