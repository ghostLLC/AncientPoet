package com.ancientpoet.server.ai

import com.ancientpoet.server.model.domain.Message
import com.ancientpoet.server.repository.MessageRepository

class ContextManager(private val messageRepo: MessageRepository) {

    suspend fun buildMessages(
        conversationId: Long,
        systemPrompt: String,
        newUserMessage: String,
        summary: String? = null,
        recentMessages: List<Message>,
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        messages.add(ChatMessage(role = "system", content = systemPrompt))

        if (summary != null) {
            messages.add(ChatMessage(role = "system", content = "【此前通信摘要】\n$summary"))
        }

        recentMessages.forEach { msg ->
            when (msg.senderType) {
                "user" -> messages.add(ChatMessage(role = "user", content = msg.contentText ?: ""))
                "poet" -> messages.add(ChatMessage(role = "assistant", content = msg.contentText ?: ""))
            }
        }

        messages.add(ChatMessage(role = "user", content = newUserMessage))

        return messages
    }

    suspend fun shouldSummarize(conversationId: Long): Boolean {
        val total = messageRepo.countDelivered(conversationId)
        val lastSummaryCoversUpTo = messageRepo.getLatestSummaryCoversUpTo(conversationId)
        val unsummarized = if (lastSummaryCoversUpTo != null) {
            messageRepo.countAfter(conversationId, lastSummaryCoversUpTo)
        } else total
        return unsummarized >= 40
    }
}
