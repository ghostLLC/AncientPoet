package com.ancientpoet.server.service

import com.ancientpoet.server.ai.ContextManager
import com.ancientpoet.server.ai.DeepSeekClient
import com.ancientpoet.server.ai.PromptBuilder
import com.ancientpoet.server.ai.TranslationService
import com.ancientpoet.server.model.domain.*
import com.ancientpoet.server.repository.ConversationRepository
import com.ancientpoet.server.repository.MessageRepository
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.server.repository.UserRepository
import com.ancientpoet.server.scheduler.MessageDeliveryScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MessageService(
    private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(MessageService::class.java),
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository,
    private val poetRepo: PoetRepository,
    private val userRepo: UserRepository,
    private val deepSeekClient: DeepSeekClient,
    private val translationService: TranslationService,
    private val contextManager: ContextManager,
    private val deliveryScheduler: MessageDeliveryScheduler,
    private val poetLocationService: PoetLocationService,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun sendMessage(
        conversationId: Long,
        userId: Long,
        contentText: String?,
        contentImageUrl: String?,
    ): EstimatedDelivery {
        val conversation = conversationRepo.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        if (conversation.userId != userId) throw SecurityException("Not your conversation")

        val poet = poetRepo.findById(conversation.poetId)
            ?: throw IllegalArgumentException("Poet not found")

        val userLoc = userRepo.getLocation(userId, conversation.dynastyId)
        val userLat = userLoc?.lat ?: 34.26
        val userLng = userLoc?.lng ?: 108.94
        val userLocationName = userLoc?.locationName ?: "长安"

        val year = conversation.storylineCurrentYear
            ?: poetLocationService.getDefaultYear(conversation.poetId)
        val poetMovement = poetLocationService.getPoetLocation(poet.id, year)
        val poetLat = poetMovement?.lat ?: 34.26
        val poetLng = poetMovement?.lng ?: 108.94
        val poetLocationName = poetMovement?.locationName ?: "长安"

        val delayResult = DelayCalculationService.calculate(
            userLat, userLng, poetLat, poetLng,
            settled = userLoc?.status == LocationStatus.SETTLED,
            eventDelayMultiplier = poetMovement?.let { mv ->
                if (mv.eventType == "war" || mv.eventType == "exile") 1.5 else 1.0
            } ?: 1.0,
        )

        val scheduledAt = Instant.now().plusSeconds(delayResult.finalDelaySeconds)
        val factors = mapOf(
            "distanceKm" to "${delayResult.distanceKm}",
            "baseDelay" to "${delayResult.baseDelayHours}小时",
            "fromLocation" to userLocationName,
            "toLocation" to poetLocationName,
        )

        // Store user message (immediately delivered)
        messageRepo.create(
            conversationId = conversationId,
            senderType = "user",
            contentText = contentText,
            contentImageUrl = contentImageUrl,
            translation = null,
            scheduledDeliveryAt = null,
            delaySeconds = null,
            delayFactors = null,
        )

        // Generate AI reply in background
        scope.launch {
            try {
                val systemPrompt = PromptBuilder.buildSystemPrompt(poet, year, poetLocationName)
                val summary = messageRepo.getLatestSummary(conversationId)
                val recentMessages = messageRepo.getRecentDelivered(conversationId, 16, null)

                val messages = contextManager.buildMessages(
                    conversationId = conversationId,
                    systemPrompt = systemPrompt,
                    newUserMessage = contentText ?: "",
                    summary = summary,
                    recentMessages = recentMessages,
                )

                val poetReply = deepSeekClient.chatCompletion(messages)
                val translation = translationService.translateToVernacular(poetReply)

                messageRepo.create(
                    conversationId = conversationId,
                    senderType = "poet",
                    contentText = poetReply,
                    contentImageUrl = null,
                    translation = translation,
                    scheduledDeliveryAt = scheduledAt,
                    delaySeconds = delayResult.finalDelaySeconds.toInt(),
                    delayFactors = factors,
                ).also { poetMsg ->
                    deliveryScheduler.schedule(poetMsg.id, scheduledAt)
                }

                // Check if summarization is needed
                if (contextManager.shouldSummarize(conversationId)) {
                    val unsummarized = messageRepo.getRecentDelivered(conversationId, 40, null)
                    val contentToSummarize = unsummarized.joinToString("\n") { msg ->
                        "${if (msg.senderType == "user") "友人" else "诗人"}: ${msg.contentText ?: ""}"
                    }
                    val summaryText = deepSeekClient.summarize(contentToSummarize)
                    val lastMsgId = unsummarized.lastOrNull()?.id ?: return@launch
                    messageRepo.saveSummary(conversationId, summaryText, lastMsgId)
                }
            } catch (e: Exception) {
                logger.error("AI reply generation failed for conversation $conversationId", e)
            }
        }

        return EstimatedDelivery(
            delaySeconds = delayResult.finalDelaySeconds.toInt(),
            deliverAt = DateTimeFormatter.ISO_INSTANT.format(scheduledAt),
            distanceKm = delayResult.distanceKm,
            fromLocation = userLocationName,
            toLocation = poetLocationName,
            factors = factors,
        )
    }

    suspend fun getMessages(conversationId: Long, userId: Long): List<Message> {
        val conversation = conversationRepo.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        if (conversation.userId != userId) throw SecurityException("Not your conversation")
        return messageRepo.findByConversationId(conversationId)
    }

    suspend fun getPending(conversationId: Long, userId: Long): List<Message> {
        val conversation = conversationRepo.findById(conversationId)
            ?: throw IllegalArgumentException("Conversation not found")
        if (conversation.userId != userId) throw SecurityException("Not your conversation")
        return messageRepo.findPendingByConversationId(conversationId)
    }
}
