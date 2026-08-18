package com.ancientpoet.android.ui.screen.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ConversationViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<StorylineRes>("conversations/$conversationId/storyline/state")) {
                is ApiResult.Success -> {
                    val sl = result.value
                    _state.value = _state.value.copy(
                        storyline = StorylineInfo(sl.currentYear, sl.poetAge, sl.locationName, sl.activeEvent, sl.eventDescription, sl.eventType),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<MsgResponse>>("conversations/$conversationId/messages")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    messages = result.value.map { MsgItem(it.id, it.senderType, it.contentText ?: "", it.translation, it.contentImageUrl) },
                    isLoading = false,
                    errorMessage = null,
                )
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun sendMessage(conversationId: Long, text: String, imageUrl: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true, errorMessage = null, canRetry = false)
            when (val result = api.post<MessageResponse>("conversations/$conversationId/messages", SendReq(text.ifBlank { null }, imageUrl))) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isSending = false)
                    loadMessages(conversationId)
                    loadConversation(conversationId)
                }
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    isSending = false,
                    errorMessage = result.message,
                    canRetry = result.retryable,
                )
            }
        }
    }

    fun jumpToYear(conversationId: Long, year: Int) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.post<StorylineRes>("conversations/$conversationId/storyline/jump", JumpReq(year))) {
                is ApiResult.Success -> {
                    val sl = result.value
                    _state.value = _state.value.copy(
                        storyline = StorylineInfo(sl.currentYear, sl.poetAge, sl.locationName, sl.activeEvent, sl.eventDescription, sl.eventType),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun createConversation(poetId: Long, year: Int, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            setLoading()
            val backgroundSetting = year.takeIf { it != 0 }?.let { "storylineYear=$it" }
            when (val result = api.post<ConversationCreatedResponse>("conversations", CreateConversationRequest(poetId, backgroundSetting = backgroundSetting))) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = null)
                    onCreated(result.value.id)
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    private fun setLoading() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
    }

    private fun setFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable)
    }
}

data class ConversationState(
    val messages: List<MsgItem> = emptyList(),
    val isSending: Boolean = false,
    val poetName: String = "",
    val storyline: StorylineInfo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
)

data class MsgItem(val id: Long, val senderType: String, val contentText: String, val translation: String?, val imageUrl: String? = null)
data class StorylineInfo(val currentYear: Int, val poetAge: Int, val locationName: String, val activeEvent: String?, val eventDescription: String?, val eventType: String)

@Serializable data class MsgResponse(val id: Long, val senderType: String, val contentText: String?, val translation: String?, val contentImageUrl: String? = null)
@Serializable data class SendReq(val contentText: String?, val contentImageUrl: String? = null)
@Serializable data class JumpReq(val year: Int)
@Serializable data class StorylineRes(val currentYear: Int, val poetAge: Int, val locationName: String, val activeEvent: String?, val eventDescription: String?, val eventType: String)
@Serializable data class CreateConversationRequest(val poetId: Long, val mode: String = "open", val backgroundSetting: String? = null, val dynastyId: String? = null)
@Serializable data class ConversationCreatedResponse(val id: Long, val poet: ConversationPoet = ConversationPoet(), val mode: String = "open", val dynastyId: String = "")
@Serializable data class ConversationPoet(val id: Long = 0, val name: String = "", val dynasty: String = "")
@Serializable data class MessageResponse(val messageId: Long = 0, val status: String = "")
