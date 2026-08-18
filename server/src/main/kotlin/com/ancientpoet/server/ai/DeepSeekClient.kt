package com.ancientpoet.server.ai

import com.ancientpoet.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DeepSeekClient(private val config: AppConfig) {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
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
            model = model, messages = messages,
            temperature = temperature, maxTokens = maxTokens,
        )
        val response = client.post("${config.deepseekBaseUrl}/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.deepseekApiKey}")
            setBody(request)
        }
        return json.decodeFromString<ChatCompletionResponse>(response.body()).choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from DeepSeek")
    }

    suspend fun chatCompletionWithImage(
        textContent: String,
        imageUrl: String,
        messages: List<ChatMessage>,
    ): String {
        val visionMessage = VisionChatMessage(
            role = "user",
            content = listOf(
                ContentPart(type = "text", text = textContent, imageUrl = null),
                ContentPart(type = "image_url", text = null, imageUrl = ImageUrl(url = imageUrl)),
            ),
        )
        val historyMessages = messages.map { msg ->
            VisionChatMessage(role = msg.role, content = listOf(ContentPart(type = "text", text = msg.content, imageUrl = null)))
        }
        val request = VisionChatCompletionRequest(
            model = config.deepseekModelVision,
            messages = historyMessages + visionMessage,
            temperature = config.deepseekTemperature,
            maxTokens = config.deepseekMaxTokens,
        )
        val response = client.post("${config.deepseekBaseUrl}/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${config.deepseekApiKey}")
            setBody(request)
        }
        return json.decodeFromString<ChatCompletionResponse>(response.body()).choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from DeepSeek vision")
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

fun ChatMessage.toVisionMsg() = VisionChatMessageSimple(role = role, content = content)

@Serializable data class ChatMessage(val role: String, val content: String)
@Serializable data class ChatCompletionRequest(val model: String, val messages: List<ChatMessage>, val temperature: Double, @kotlinx.serialization.SerialName("max_tokens") val maxTokens: Int)
@Serializable data class ChatCompletionResponse(val choices: List<Choice>)
@Serializable data class Choice(val message: ChatMessage, @kotlinx.serialization.SerialName("finish_reason") val finishReason: String? = null)

@Serializable data class VisionChatMessage(val role: String, val content: List<ContentPart>)
@Serializable data class VisionChatMessageSimple(val role: String, val content: String)
@Serializable data class ContentPart(
    val type: String,
    val text: String? = null,
    @kotlinx.serialization.SerialName("image_url") val imageUrl: ImageUrl? = null,
)
@Serializable data class ImageUrl(val url: String)
@Serializable data class VisionChatCompletionRequest(val model: String, val messages: List<VisionChatMessage>, val temperature: Double, @kotlinx.serialization.SerialName("max_tokens") val maxTokens: Int)
