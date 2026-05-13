package com.ancientpoet.server.model.db

import kotlinx.serialization.Serializable

@Serializable
data class PersonalityProfileDb(
    val traits: List<String>,
    val mbti: String? = null,
    val speakingStyle: String? = null,
)
