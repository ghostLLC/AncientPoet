package com.ancientpoet.android.ui.screen.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ConversationViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state
    private var pendingRetry: PendingRetry? = null

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch { loadConversationInternal(conversationId) }
    }

    fun loadMessages(conversationId: Long) {
        viewModelScope.launch { loadMessagesInternal(conversationId) }
    }

    fun loadInitial(conversationId: Long, initialYear: Int? = null) {
        viewModelScope.launch {
            loadConversationInternal(conversationId)
            loadMessagesInternal(conversationId)
            initialYear?.let { jumpToYearInternal(conversationId, it) }
        }
    }

    fun retry(conversationId: Long) {
        viewModelScope.launch {
            when (val action = pendingRetry) {
                is PendingRetry.Send -> sendMessageInternal(conversationId, action.text, action.imageUrl)
                is PendingRetry.Jump -> jumpToYearInternal(conversationId, action.year)
                null -> retryInitialLoadInternal(conversationId)
            }
        }
    }

    fun sendMessage(conversationId: Long, text: String, imageUrl: String? = null) {
        viewModelScope.launch { sendMessageInternal(conversationId, text, imageUrl) }
    }

    fun jumpToYear(conversationId: Long, year: Int) {
        viewModelScope.launch { jumpToYearInternal(conversationId, year) }
    }

    fun createConversation(poetId: Long, year: Int, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            pendingRetry = null
            beginAction()
            val backgroundSetting = year.takeIf { it != 0 }?.let { "storylineYear=$it" }
            when (val result = api.post<ConversationCreatedResponse>("conversations") {
                setBody(CreateConversationRequest(poetId, backgroundSetting = backgroundSetting))
            }) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, actionError = null)
                    onCreated(result.value.id)
                }
                is ApiResult.Failure -> setActionFailure(result)
            }
        }
    }

    private suspend fun retryInitialLoadInternal(conversationId: Long) {
        val current = _state.value
        if (current.conversationError?.retryable == true) {
            loadConversationInternal(conversationId)
        }
        if (current.messagesError?.retryable == true) {
            loadMessagesInternal(conversationId)
        }
    }

    private suspend fun sendMessageInternal(conversationId: Long, text: String, imageUrl: String?) {
        pendingRetry = null
        _state.value = _state.value.copy(isSending = true, actionError = null)
        when (val result = api.post<MessageResponse>("conversations/$conversationId/messages") {
            setBody(SendReq(text.ifBlank { null }, imageUrl))
        }) {
            is ApiResult.Success -> {
                _state.value = _state.value.copy(isSending = false)
                loadMessagesInternal(conversationId)
                loadConversationInternal(conversationId)
            }
            is ApiResult.Failure -> {
                if (result.retryable) pendingRetry = PendingRetry.Send(text, imageUrl)
                _state.value = _state.value.copy(
                    isSending = false,
                    actionError = OperationError(result.message, result.retryable),
                )
            }
        }
    }

    private suspend fun loadConversationInternal(conversationId: Long) {
        beginConversationLoad()
        when (val result = api.get<StorylineRes>("conversations/$conversationId/storyline/state")) {
            is ApiResult.Success -> {
                val sl = result.value
                _state.value = _state.value.copy(
                    storyline = StorylineInfo(sl.currentYear, sl.poetAge, sl.locationName, sl.activeEvent, sl.eventDescription, sl.eventType),
                    isLoading = false,
                    conversationError = null,
                )
            }
            is ApiResult.Failure -> setConversationFailure(result)
        }
    }

    private suspend fun loadMessagesInternal(conversationId: Long) {
        beginMessagesLoad()
        when (val result = api.get<List<MsgResponse>>("conversations/$conversationId/messages")) {
            is ApiResult.Success -> _state.value = _state.value.copy(
                messages = result.value.map { MsgItem(it.id, it.senderType, it.contentText ?: "", it.translation, it.contentImageUrl) },
                isLoading = false,
                messagesError = null,
            )
            is ApiResult.Failure -> setMessagesFailure(result)
        }
    }

    private suspend fun jumpToYearInternal(conversationId: Long, year: Int) {
        pendingRetry = null
        beginAction()
        when (val result = api.post<StorylineRes>("conversations/$conversationId/storyline/jump") {
            setBody(JumpReq(year))
        }) {
            is ApiResult.Success -> {
                val sl = result.value
                _state.value = _state.value.copy(
                    storyline = StorylineInfo(sl.currentYear, sl.poetAge, sl.locationName, sl.activeEvent, sl.eventDescription, sl.eventType),
                    isLoading = false,
                    actionError = null,
                )
            }
            is ApiResult.Failure -> setActionFailure(result)
        }
        if (_state.value.actionError?.retryable == true) {
            pendingRetry = PendingRetry.Jump(year)
        }
    }

    private fun beginConversationLoad() {
        _state.value = _state.value.copy(isLoading = true, conversationError = null)
    }

    private fun beginMessagesLoad() {
        _state.value = _state.value.copy(isLoading = true, messagesError = null)
    }

    private fun beginAction() {
        _state.value = _state.value.copy(isLoading = true, actionError = null)
    }

    private fun setConversationFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, conversationError = OperationError(result.message, result.retryable))
    }

    private fun setMessagesFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, messagesError = OperationError(result.message, result.retryable))
    }

    private fun setActionFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, actionError = OperationError(result.message, result.retryable))
    }
}

private sealed interface PendingRetry {
    data class Send(val text: String, val imageUrl: String?) : PendingRetry
    data class Jump(val year: Int) : PendingRetry
}

data class ConversationState(
    val messages: List<MsgItem> = emptyList(),
    val isSending: Boolean = false,
    val poetName: String = "",
    val storyline: StorylineInfo? = null,
    val isLoading: Boolean = false,
    val conversationError: OperationError? = null,
    val messagesError: OperationError? = null,
    val actionError: OperationError? = null,
) {
    val errorMessage: String?
        get() = listOfNotNull(conversationError, messagesError, actionError)
            .joinToString("；") { it.message }
            .takeIf { it.isNotEmpty() }

    val canRetry: Boolean
        get() = listOfNotNull(conversationError, messagesError, actionError).any { it.retryable }
}

data class OperationError(val message: String, val retryable: Boolean)

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
