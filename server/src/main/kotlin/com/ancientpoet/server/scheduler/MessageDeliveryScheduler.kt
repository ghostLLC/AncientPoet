package com.ancientpoet.server.scheduler

import com.ancientpoet.server.ai.*
import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.push.PushNotificationService
import com.ancientpoet.server.repository.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

/** PostgreSQL owns the queue. Every claimed job is fenced by its lease token. */
class MessageDeliveryScheduler(
    private val config: AppConfig,
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository,
    private val deepSeek: DeepSeekClient,
    private val translation: TranslationService,
    private val context: ContextManager,
    private val push: PushNotificationService
) : AutoCloseable {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (isActive) {
                try {
                    messageRepo.deliverDue()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (failure: Exception) {
                    logger.warn("Delivery scan failed: {}", failure.javaClass.simpleName)
                }
                delay(config.workerPollMillis)
            }
        }
        repeat(2) {
            scope.launch {
                while (isActive) {
                    var job: MessageJob? = null
                    try {
                        job = messageRepo.claim(config.jobLeaseSeconds)
                        if (job == null) delay(config.workerPollMillis) else execute(job)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (failure: Exception) {
                        logger.warn("Background task {} failed: {}", job?.id, failure.javaClass.simpleName)
                        job?.let { failed ->
                            runCatching { messageRepo.fail(failed, failure.javaClass.simpleName) }
                        }
                        delay(config.workerPollMillis)
                    }
                }
            }
        }
    }

    private suspend fun execute(job: MessageJob) {
        val message = messageRepo.findInternal(job.messageId) ?: return
        when (job.kind) {
            "generate" -> {
                val snapshot = json.decodeFromString<GenerationSnapshot>(job.payload)
                val summary = messageRepo.summarySnapshot(message.conversationId, message.id)
                val covers = summary?.covers
                // Excludes this submitted letter and all later input; ContextManager appends it exactly once.
                val history = messageRepo.getRecentDelivered(message.conversationId, 40, covers, message.id)
                val messages = context.buildMessages(
                    message.conversationId,
                    snapshot.systemPrompt,
                    message.contentText.orEmpty(),
                    summary?.text,
                    history
                )
                val reply = deepSeek.chatCompletion(messages)
                if (reply.isBlank()) error("EmptyReply")
                messageRepo.completeGeneration(job, reply)
            }

            "translate" -> messageRepo.completeTranslation(job, translation.translateToVernacular(message.contentText.orEmpty()))

            "notify" -> {
                val conversation = conversationRepo.findById(message.conversationId)
                if (conversation != null && !conversation.archived) push.sendLetterArrival(conversation.userId, conversation.poetName.orEmpty())
                messageRepo.complete(job)
            }

            "summarize" -> {
                val previous = messageRepo.summarySnapshot(message.conversationId)
                val covers = previous?.covers
                if (messageRepo.countAfter(message.conversationId, covers ?: 0) > 40) {
                    val history = messageRepo.getRecentDelivered(message.conversationId, 200, covers, oldestFirst = true)
                    val toSummarize = history.dropLast(16)
                    if (toSummarize.isNotEmpty()) {
                        val old = previous?.text.orEmpty()
                        val content = toSummarize.joinToString("\n") { it.senderType + ": " + it.contentText.orEmpty() }
                        val summary = deepSeek.summarize("此前摘要：\n" + old + "\n新增通信：\n" + content)
                        messageRepo.completeSummary(job, message.conversationId, summary, toSummarize.last().id)
                        return
                    }
                }
                messageRepo.complete(job)
            }
        }
    }

    override fun close() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
    }
}
