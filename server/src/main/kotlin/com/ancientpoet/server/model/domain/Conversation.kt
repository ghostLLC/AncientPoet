package com.ancientpoet.server.model.domain

data class Conversation(
    val id: Long,
    val userId: Long,
    val poetId: Long,
    val poetName: String? = null,
    val mode: String,
    val dynastyId: String,
    val storylineCurrentYear: Int?,
    val storylineCompleted: Boolean,
    val backgroundSetting: String?,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
