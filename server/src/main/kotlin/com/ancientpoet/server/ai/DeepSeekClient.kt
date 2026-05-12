package com.ancientpoet.server.ai

import com.ancientpoet.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DeepSeekClient(private val config: AppConfig) {
    private val client = HttpClient {
        io.ktor.client.plugins.HttpTimeout {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        model: String = config.deepseekModelChat,
        temperature: Double = config.deepseekTemperature,
        maxTokens: Int = config.deepseekMaxTokens,
    ): String {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
        )

        val response = client.post("${config.deepseekBaseUrl}/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.deepseekApiKey}")
            setBody(request)
        }

        val body: ChatCompletionResponse = response.body()
        return body.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from DeepSeek")
    }

    suspend fun summarize(messagesContent: String): String {
        return chatCompletion(
            messages = listOf(
                ChatMessage(role = "system", content = "你是一位古文专家。请将以下对话摘要压缩为一段简洁的总结。只输出摘要，不要添加任何解释。"),
                ChatMessage(role = "user", content = messagesContent),
            ),
            temperature = 0.3,
        )
    }
}

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>,
)

@Serializable
data class Choice(
    val message: ChatMessage,
    @kotlinx.serialization.SerialName("finish_reason")
    val finishReason: String? = null,
)
