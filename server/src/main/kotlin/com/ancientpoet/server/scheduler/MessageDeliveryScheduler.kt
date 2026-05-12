package com.ancientpoet.server.scheduler

import com.ancientpoet.server.config.RedisConfig
import com.ancientpoet.server.push.PushNotificationService
import com.ancientpoet.server.repository.ConversationRepository
import com.ancientpoet.server.repository.MessageRepository
import kotlinx.coroutines.*
import java.time.Instant

class MessageDeliveryScheduler(
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository,
    private val pushService: PushNotificationService,
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(this::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var deliveryJob: Job? = null
    private val scheduleKey = "msg:delivery:schedule"

    fun start() {
        deliveryJob = scope.launch {
            while (isActive) {
                try {
                    processDeliveries()
                } catch (e: Exception) {
                    logger.error("Delivery polling error", e)
                }
                delay(60_000) // Poll every minute
            }
        }
    }

    fun schedule(messageId: Long, deliverAt: Instant) {
        val jedis = RedisConfig.pool.resource
        try {
            jedis.zadd(scheduleKey, deliverAt.epochSecond.toDouble(), messageId.toString())
        } finally {
            jedis.close()
        }
    }

    suspend fun processDeliveries() {
        val jedis = RedisConfig.pool.resource
        try {
            val now = Instant.now().epochSecond.toDouble()
            val dueIds = jedis.zrangeByScore(scheduleKey, 0.0, now)

            for (idStr in dueIds) {
                val messageId = idStr.toLong()
                try {
                    val message = messageRepo.markDelivered(messageId)
                    if (message != null) {
                        val conversation = conversationRepo.findById(message.conversationId)
                        if (conversation != null) {
                            pushService.sendLetterArrival(conversation.userId, "远方")
                        }
                    }
                    jedis.zrem(scheduleKey, idStr)
                } catch (e: Exception) {
                    logger.error("Failed to deliver message $messageId", e)
                }
            }
        } finally {
            jedis.close()
        }
    }
}
