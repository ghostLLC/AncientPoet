package com.ancientpoet.android.ui.screen.conversation

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ConversationViewModel(private val client: HttpClient) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state

    fun loadMessages(conversationId: Long) {
        scope.launch {
            try {
                val msgs: List<MsgResponse> = client.get("http://10.0.2.2:8080/api/v1/conversations/$conversationId/messages").body()
                _state.value = _state.value.copy(messages = msgs.map { MsgItem(it.id, it.senderType, it.contentText ?: "", it.translation) })
            } catch (e: Exception) { /* silent */ }
        }
    }

    fun sendMessage(conversationId: Long, text: String) {
        scope.launch {
            _state.value = _state.value.copy(isSending = true)
            try {
                client.post("http://10.0.2.2:8080/api/v1/conversations/$conversationId/messages") {
                    contentType(ContentType.Application.Json)
                    setBody(SendReq(text))
                }
                _state.value = _state.value.copy(isSending = false)
                loadMessages(conversationId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSending = false)
            }
        }
    }
}

data class ConversationState(val messages: List<MsgItem> = emptyList(), val isSending: Boolean = false, val poetName: String = "")
data class MsgItem(val id: Long, val senderType: String, val contentText: String, val translation: String?)

@Serializable data class MsgResponse(val id: Long, val senderType: String, val contentText: String?, val translation: String?)
@Serializable data class SendReq(val contentText: String)
