package com.ancientpoet.android.ui.screen.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.shared.contract.*
import com.ancientpoet.shared.data.api.*
import com.ancientpoet.shared.data.local.LetterDraft
import io.ktor.client.request.setBody
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ConversationViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state
    private val refreshLock = Mutex()
    private var polling: Job? = null
    private var currentId: Long? = null
    private var lastRead = 0L
    private var pendingRetry: PendingRetry? = null
    private val draftChanges = Channel<Pair<Long, String>>(Channel.CONFLATED)
    init {
        viewModelScope.launch {
            for ((id, text) in draftChanges) {
                if (currentId != id || _state.value.draftText != text || _state.value.isSending) continue
                try {
                    repository.saveDraft(id, text)
                    if (currentId == id && _state.value.draftText == text) _state.update { it.copy(draftSaved = true) }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    _state.update { it.copy(draftSaved = false, actionError = OperationError("信稿尚未保存，请保留当前页面", true)) }
                }
            }
        }
    }
    fun loadInitial(conversationId: Long, initialYear: Int? = null) {
        if (currentId == conversationId) return
        currentId = conversationId
        _state.value = ConversationState(isLoading = true)
        viewModelScope.launch {
            val detail = repository.cached<ConversationResponse>(path(conversationId))
            val messages = repository.cached<List<MessageItem>>(path(conversationId) + "/messages").orEmpty()
            val draft = try {
                repository.draft(conversationId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(actionError = OperationError("无法读取本机信稿，请检查存储空间", true)) }
                null
            }
            _state.update {
                it.copy(
                    detail = detail,
                    poetName = detail?.poet?.name.orEmpty(),
                    messages = messages.map(MessageItem::display),
                    draftText = draft?.text.orEmpty(),
                    draftSaved = true,
                    showingCache = detail != null || messages.isNotEmpty()
                )
            }
            refresh(conversationId)
        }
    }
    fun startPolling(conversationId: Long) {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            if (_state.value.detail != null) refresh(conversationId)
            while (isActive) {
                delay(8_000)
                refresh(conversationId)
            }
        }
    }
    fun stopPolling() {
        polling?.cancel()
        polling = null
    }
    fun updateDraft(conversationId: Long, text: String) {
        if (_state.value.isSending || text.length > 12_000) return
        pendingRetry = null
        _state.update { it.copy(draftText = text, draftSaved = false, quote = null, actionError = null) }
        draftChanges.trySend(conversationId to text)
    }
    fun loadConversation(conversationId: Long) {
        viewModelScope.launch { refresh(conversationId) }
    }
    fun loadMessages(conversationId: Long) = loadConversation(conversationId)

    private suspend fun refresh(id: Long) {
        if (!refreshLock.tryLock()) return
        val capturedOwner = repository.owner()
        try {
            coroutineScope {
                val detail = async { repository.fetch<ConversationResponse>(path(id)) }
                val letters = async { repository.fetch<List<MessageItem>>(path(id) + "/messages") }
                val pending = async { repository.fetch<List<PendingMessageItem>>(path(id) + "/pending") }
                val detailResult = detail.await()
                val lettersResult = letters.await()
                val pendingResult = pending.await()
                if (capturedOwner != repository.owner()) return@coroutineScope
                when (val result = detailResult) {
                    is ApiResult.Success -> {
                        val d = result.value
                        _state.update {
                            it.copy(
                                detail = d,
                                poetName = d.poet.name,
                                conversationError = null,
                                storyline = d.currentYear?.let { year -> StorylineInfo(year, 0, d.poetLocation?.name.orEmpty(), null, d.poetLocation?.event, "normal") }
                            )
                        }
                        if (_state.value.yearOptions.isEmpty() && d.poet.id > 0) {
                            val years = repository.fetch<List<YearOption>>("poets/" + d.poet.id + "/life-events", public = true)
                            if (years is ApiResult.Success) _state.update { it.copy(yearOptions = years.value.distinctBy { y -> y.year }) }
                        }
                    }

                    is ApiResult.Failure -> _state.update { it.copy(conversationError = result.operation()) }
                }
                when (val result = lettersResult) {
                    is ApiResult.Success -> {
                        val merged = (_state.value.messages.map(MsgItem::contract) + result.value).associateBy { it.id }.values.sortedBy { it.id }
                        _state.update {
                            it.copy(
                                messages = merged.map(MessageItem::display),
                                messagesError = null,
                                showingCache = false,
                                hasOlder = if (it.messages.isEmpty()) result.value.size == 50 else it.hasOlder
                            )
                        }
                        repository.optionalCache { repository.cache.write(capturedOwner, path(id) + "/messages", repository.json.encodeToString(merged.takeLast(500)), System.currentTimeMillis()) }
                        val unread = result.value.filter { it.senderType == "poet" && it.readAt == null }.maxOfOrNull { it.id } ?: 0
                        if (unread > lastRead && repository.markRead(id, unread)) lastRead = unread
                    }

                    is ApiResult.Failure -> _state.update { it.copy(messagesError = result.operation(), showingCache = it.messages.isNotEmpty()) }
                }
                when (val result = pendingResult) {
                    is ApiResult.Success -> _state.update { it.copy(pending = result.value, pendingError = null) }
                    is ApiResult.Failure -> _state.update { it.copy(pendingError = result.operation()) }
                }
            }
        } finally {
            _state.update { it.copy(isLoading = false) }
            refreshLock.unlock()
        }
    }
    fun loadOlder(conversationId: Long) {
        if (_state.value.loadingOlder) return
        val first = _state.value.messages.firstOrNull()?.id ?: return
        _state.update { it.copy(loadingOlder = true) }
        viewModelScope.launch {
            when (val result = repository.api.get<List<MessageItem>>(path(conversationId) + "/messages?beforeId=" + first, repository.owner())) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        messages = (result.value.map(MessageItem::display) + it.messages)
                            .distinctBy { m -> m.id }.sortedBy { m -> m.id },
                        hasOlder = result.value.size == 50,
                        loadingOlder = false,
                        messagesError = null
                    )
                }

                is ApiResult.Failure -> _state.update { it.copy(loadingOlder = false, messagesError = result.operation()) }
            }
        }
    }
    fun prepareSend(conversationId: Long) {
        if (_state.value.draftText.isBlank() || _state.value.isSending || _state.value.previewLoading) return
        _state.update { it.copy(previewLoading = true, actionError = null) }
        viewModelScope.launch {
            when (val result = repository.fetch<EstimatedDeliveryDto>(path(conversationId) + "/delivery-preview")) {
                is ApiResult.Success -> _state.update { it.copy(quote = result.value, previewLoading = false) }

                is ApiResult.Failure -> _state.update {
                    it.copy(
                        previewLoading = false,
                        actionError = result.operation(),
                        needsLocation = result.errorCode == "location_required"
                    )
                }
            }
        }
    }
    fun dismissQuote() {
        _state.update { it.copy(quote = null) }
    }
    fun confirmSend(conversationId: Long) = sendMessage(conversationId, _state.value.draftText)

    fun sendMessage(conversationId: Long, text: String, imageUrl: String? = null) {
        if (_state.value.isSending || text.isBlank()) return
        _state.update { it.copy(isSending = true, quote = null, draftText = text, actionError = null) }
        viewModelScope.launch {
            try {
                val draft = repository.saveDraft(conversationId, text)
                send(conversationId, draft)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                _state.update { it.copy(isSending = false, draftSaved = false, actionError = OperationError("信稿未能保存，请保留文字后重试", true)) }
            }
        }
    }
    private suspend fun send(id: Long, draft: LetterDraft) {
        when (val result = repository.send(id, draft)) {
            is ApiResult.Success -> {
                pendingRetry = null
                _state.update {
                    it.copy(
                        isSending = false,
                        draftText = if (it.draftText == draft.text) "" else it.draftText,
                        draftSaved = true,
                        actionError = null,
                        sentSequence = it.sentSequence + 1,
                        receipt = result.value.estimatedDelivery
                    )
                }
                refresh(id)
            }

            is ApiResult.Failure -> {
                pendingRetry = PendingRetry.Send(draft)
                _state.update { it.copy(isSending = false, draftSaved = true, actionError = result.operation()) }
            }
        }
    }
    fun retry(conversationId: Long) {
        if (_state.value.isSending) return
        viewModelScope.launch {
            when (val action = pendingRetry) {
                is PendingRetry.Send -> {
                    _state.update { it.copy(isSending = true, actionError = null) }
                    send(conversationId, action.draft)
                }

                is PendingRetry.Jump -> jump(conversationId, action.year)

                null -> refresh(conversationId)
            }
        }
    }
    fun retryReply(conversationId: Long, messageId: Long) {
        viewModelScope.launch {
            when (val result = repository.retryReply(conversationId, messageId)) {
                is ApiResult.Success -> refresh(conversationId)
                is ApiResult.Failure -> _state.update { it.copy(actionError = result.operation()) }
            }
        }
    }
    fun jumpToYear(conversationId: Long, year: Int) {
        viewModelScope.launch { jump(conversationId, year) }
    }
    private suspend fun jump(id: Long, year: Int) {
        pendingRetry = null
        when (val result = repository.api.post<StorylineRes>(path(id) + "/storyline/jump", repository.owner()) { setBody(JumpReq(year)) }) {
            is ApiResult.Success -> {
                val sl = result.value
                _state.update {
                    it.copy(
                        storyline = StorylineInfo(sl.currentYear, sl.poetAge, sl.locationName, sl.activeEvent, sl.eventDescription, sl.eventType),
                        actionError = null,
                        quote = null
                    )
                }
                refresh(id)
            }

            is ApiResult.Failure -> {
                pendingRetry = PendingRetry.Jump(year)
                _state.update { it.copy(actionError = result.operation()) }
            }
        }
    }
    fun createConversation(poetId: Long, year: Int, onCreated: (Long) -> Unit) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, actionError = null) }
        viewModelScope.launch {
            when (val result = repository.create(poetId, year.takeIf { it != 0 })) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    onCreated(result.value.id)
                }

                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, actionError = result.operation()) }
            }
        }
    }
    fun archive(id: Long, archived: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.archive(id, archived)) {
                is ApiResult.Success -> onDone()
                is ApiResult.Failure -> _state.update { it.copy(actionError = result.operation()) }
            }
        }
    }
    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.deleteConversation(id)) {
                is ApiResult.Success -> onDone()
                is ApiResult.Failure -> _state.update { it.copy(actionError = result.operation()) }
            }
        }
    }
    private fun path(id: Long) = "conversations/" + id
}
private fun ApiResult.Failure.operation() = OperationError(message, retryable)
private sealed interface PendingRetry {
    data class Send(val draft: LetterDraft) : PendingRetry
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
    val pendingError: OperationError? = null,
    val pending: List<PendingMessageItem> = emptyList(),
    val detail: ConversationResponse? = null,
    val draftText: String = "",
    val draftSaved: Boolean = true,
    val showingCache: Boolean = false,
    val hasOlder: Boolean = true,
    val loadingOlder: Boolean = false,
    val quote: EstimatedDeliveryDto? = null,
    val receipt: EstimatedDeliveryDto? = null,
    val previewLoading: Boolean = false,
    val sentSequence: Int = 0,
    val needsLocation: Boolean = false,
    val yearOptions: List<YearOption> = emptyList()
) {
    val errorMessage: String? get() = listOfNotNull(conversationError, messagesError, pendingError, actionError)
        .map { it.message }.distinct().joinToString("；").ifBlank { null }
    val canRetry: Boolean get() = listOfNotNull(conversationError, messagesError, pendingError, actionError).any { it.retryable }
}
data class OperationError(val message: String, val retryable: Boolean)
data class MsgItem(
    val id: Long,
    val senderType: String,
    val contentText: String,
    val translation: String?,
    val imageUrl: String? = null,
    val conversationId: Long = 0,
    val createdAt: String? = null,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val clientMessageId: String? = null
)
private fun MessageItem.display() = MsgItem(id, senderType, contentText.orEmpty(), translation, contentImageUrl, conversationId, createdAt, deliveredAt, readAt, clientMessageId)
private fun MsgItem.contract() = MessageItem(id, conversationId, senderType, contentText, imageUrl, translation, createdAt = createdAt, deliveredAt = deliveredAt, readAt = readAt, clientMessageId = clientMessageId)
data class StorylineInfo(val currentYear: Int, val poetAge: Int, val locationName: String, val activeEvent: String?, val eventDescription: String?, val eventType: String)

@Serializable data class JumpReq(val year: Int)

@Serializable data class StorylineRes(val currentYear: Int, val poetAge: Int, val locationName: String, val activeEvent: String?, val eventDescription: String?, val eventType: String)

@Serializable data class YearOption(val year: Int, val title: String)
