package com.ancientpoet.server.model.domain

data class Poet(
    val id: Long,
    val name: String,
    val courtesyName: String?,
    val artName: String?,
    val dynastyId: String,
    val dynastyName: String? = null,
    val birthYear: Int,
    val deathYear: Int,
    val personalityProfile: PersonalityProfile,
    val writingStyle: String,
    val systemPrompt: String,
    val biographySummary: String?,
    val portraitUrl: String?,
    val isFree: Boolean,
)

data class PersonalityProfile(
    val traits: List<String>,
    val mbti: String? = null,
    val speakingStyle: String? = null,
)

data class PoetMovement(
    val id: Long,
    val poetId: Long,
    val yearStart: Int,
    val yearEnd: Int,
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val eventDescription: String?,
    val eventType: String,
)

data class PoetLifeEvent(
    val id: Long,
    val poetId: Long,
    val year: Int,
    val age: Int,
    val title: String,
    val description: String,
    val locationName: String?,
    val eventType: String,
    val delayMultiplier: Double,
    val sortOrder: Int,
)

data class Poem(
    val id: Long,
    val poetId: Long,
    val title: String,
    val content: String,
    val yearWritten: Int?,
    val context: String?,
    val translation: String?,
    val appreciation: String?,
    val tags: List<String>,
)
