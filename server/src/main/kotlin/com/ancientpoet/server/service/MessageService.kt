package com.ancientpoet.server.service

import com.ancientpoet.server.ai.PromptBuilder
import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.model.domain.LocationStatus
import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.repository.*
import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.util.UUID

class MessageService(
    private val config: AppConfig,
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository,
    private val poetRepo: PoetRepository,
    private val userRepo: UserRepository,
    private val poetLocationService: PoetLocationService,
    private val storylineService: StorylineService,
    private val movementService: MovementService
) {
    suspend fun sendMessage(
        conversationId: Long,
        userId: Long,
        contentText: String?,
        contentImageUrl: String?,
        clientMessageId: String? = null
    ): MessageResponse {
        val text = contentText?.trim().orEmpty()
        if (text.isEmpty() || text.length > 12_000) invalid("请写下 1–12000 字的信件")
        if (contentImageUrl != null) invalid("当前版本支持文字书信，图片功能尚未开放")
        val clientId = clientMessageId ?: UUID.randomUUID().toString()
        if (!clientId.matches(Regex("[A-Za-z0-9_-]{8,80}"))) invalid("信稿标识无效，请重新打开编辑页")
        conversationRepo.requireOwned(conversationId, userId)
        messageRepo.receipt(conversationId, userId, clientId, text)?.let { return it }
        if (config.aiMode == "remote" && config.deepseekApiKey.isBlank()) unavailable("回信服务尚未配置，信稿可以继续保存")
        val snapshot = prepare(conversationId, userId)
        return messageRepo.accept(conversationId, userId, clientId, text, snapshot, config.lettersPerDay)
    }

    suspend fun preview(conversationId: Long, userId: Long): EstimatedDeliveryDto = prepare(conversationId, userId).delivery

    private suspend fun prepare(conversationId: Long, userId: Long): GenerationSnapshot {
        val conversation = conversationRepo.requireOwned(conversationId, userId)
        val poet = poetRepo.findById(conversation.poetId) ?: notFound()
        movementService.getStatus(userId, conversation.dynastyId)
        val userLocation = userRepo.getLocation(userId, conversation.dynastyId)
            ?: throw ApiException(HttpStatusCode.Conflict, "location_required", "请先在驿路选择落脚地")
        val year = conversation.storylineCurrentYear ?: poetLocationService.getDefaultYear(poet.id)
        val poetLocation = poetLocationService.getPoetLocation(poet.id, year)
            ?: throw ApiException(HttpStatusCode.Conflict, "location_unknown", "这个年代的行迹暂缺，请选择其他年代")
        val event = storylineService.getCurrentEvent(conversationId)
        val multiplier = event?.delayMultiplier ?: if (poetLocation.eventType in setOf("war", "exile")) 1.5 else 1.0
        val result = DelayCalculationService.calculate(
            userLocation.lat,
            userLocation.lng,
            poetLocation.lat,
            poetLocation.lng,
            settled = userLocation.status == LocationStatus.SETTLED,
            eventDelayMultiplier = multiplier
        )
        val seconds = config.demoDeliverySeconds ?: result.finalDelaySeconds.toInt()
        val quote = EstimatedDeliveryDto(
            seconds,
            Instant.now().plusSeconds(seconds.toLong()).toString(),
            result.distanceKm,
            userLocation.locationName,
            poetLocation.locationName,
            mapOf(
                "eventMultiplier" to multiplier.toString(),
                "settled" to (userLocation.status == LocationStatus.SETTLED).toString(),
                "demo" to (config.demoDeliverySeconds != null).toString()
            )
        )
        return GenerationSnapshot(
            PromptBuilder.buildSystemPrompt(
                poet,
                year,
                poetLocation.locationName,
                event,
                backgroundSetting = conversation.backgroundSetting
            ),
            quote
        )
    }

    suspend fun getMessages(conversationId: Long, userId: Long, limit: Int = 50, beforeId: Long? = null, afterId: Long? = null) = conversationRepo.requireOwned(conversationId, userId).let {
        require(limit in 1..100 && (beforeId == null || beforeId > 0) && (afterId == null || afterId > 0))
        require(beforeId == null || afterId == null)
        messageRepo.findByConversationId(conversationId, limit, beforeId, afterId)
    }

    suspend fun getPending(conversationId: Long, userId: Long) = conversationRepo.requireOwned(conversationId, userId).let { messageRepo.pending(conversationId) }

    suspend fun markRead(conversationId: Long, userId: Long, throughId: Long) {
        conversationRepo.requireOwned(conversationId, userId)
        require(throughId > 0)
        messageRepo.markRead(conversationId, throughId)
    }

    suspend fun retry(messageId: Long, userId: Long) = messageRepo.retry(messageId, userId)
}
