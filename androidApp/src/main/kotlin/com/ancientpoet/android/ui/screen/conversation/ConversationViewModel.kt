package com.ancientpoet.android.ui.screen.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ConversationViewModel(private val client: HttpClient) : ViewModel() {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                val sl: StorylineRes? = try {
                    client.get("http://10.0.2.2:8080/api/v1/conversations/$conversationId/storyline/state").body()
                } catch (_: Exception) { null }
                _state.value = _state.value.copy(storyline = sl?.let { StorylineInfo(it.currentYear, it.poetAge, it.locationName, it.activeEvent, it.eventDescription, it.eventType) })
            } catch (_: Exception) {}
        }
    }

    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            try {
                val msgs: List<MsgResponse> = client.get("http://10.0.2.2:8080/api/v1/conversations/$conversationId/messages").body()
                _state.value = _state.value.copy(messages = msgs.map { MsgItem(it.id, it.senderType, it.contentText ?: "", it.translation, it.contentImageUrl) })
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(conversationId: Long, text: String, imageUrl: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)
            try {
                client.post("http://10.0.2.2:8080/api/v1/conversations/$conversationId/messages") {
                    contentType(ContentType.Application.Json)
                    setBody(SendReq(text.ifBlank { null }, imageUrl))
                }
                _state.value = _state.value.copy(isSending = false)
                loadMessages(conversationId)
                loadConversation(conversationId)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isSending = false)
            }
        }
    }

    fun jumpToYear(conversationId: Long, year: Int) {
        viewModelScope.launch {
            try {
                client.post("http://10.0.2.2:8080/api/v1/conversations/$conversationId/storyline/jump") {
                    contentType(ContentType.Application.Json)
                    setBody(JumpReq(year))
                }
                loadConversation(conversationId)
            } catch (_: Exception) {}
        }
    }
}

data class ConversationState(
    val messages: List<MsgItem> = emptyList(),
    val isSending: Boolean = false,
    val poetName: String = "",
    val storyline: StorylineInfo? = null,
)
data class MsgItem(val id: Long, val senderType: String, val contentText: String, val translation: String?, val imageUrl: String? = null)
data class StorylineInfo(val currentYear: Int, val poetAge: Int, val locationName: String, val activeEvent: String?, val eventDescription: String?, val eventType: String)

@Serializable data class MsgResponse(val id: Long, val senderType: String, val contentText: String?, val translation: String?, val contentImageUrl: String? = null)
@Serializable data class SendReq(val contentText: String?, val contentImageUrl: String? = null)
@Serializable data class JumpReq(val year: Int)
@Serializable data class StorylineRes(val currentYear: Int, val poetAge: Int, val locationName: String, val activeEvent: String?, val eventDescription: String?, val eventType: String)
