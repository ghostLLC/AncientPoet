package com.ancientpoet.android.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.shared.contract.AppInfo
import com.ancientpoet.shared.contract.ConversationResponse
import com.ancientpoet.shared.data.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class HomeViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
    private var loading: Job? = null
    private var polling: Job? = null
    init {
        viewModelScope.launch { repository.changes.collect { loadConversations() } }
        viewModelScope.launch {
            val info = repository.fetch<AppInfo>("info", public = true)
            if (info is ApiResult.Success) _state.update { it.copy(info = info.value) }
        }
    }
    fun loadConversations() {
        if (loading?.isActive == true) return
        val archived = _state.value.showArchived
        loading = viewModelScope.launch {
            val guest = repository.owner() == 0L
            _state.update { it.copy(isLoading = !guest, errorMessage = null, guest = guest) }
            if (guest) return@launch
            val path = if (archived) "conversations?archived=true" else "conversations"
            repository.cached<List<ConversationResponse>>(path)?.let { cached ->
                _state.update { it.copy(conversations = cached, showingCache = true) }
            }
            when (val result = repository.fetch<List<ConversationResponse>>(path)) {
                is ApiResult.Success -> _state.update { it.copy(conversations = result.value, isLoading = false, showingCache = false) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable) }
            }
        }
    }
    fun showArchived(value: Boolean) {
        loading?.cancel()
        loading = null
        _state.update { it.copy(showArchived = value, conversations = emptyList(), showingCache = false) }
        loadConversations()
    }
    fun start() {
        loadConversations()
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                loadConversations()
            }
        }
    }
    fun stop() {
        polling?.cancel()
        polling = null
    }
}
data class HomeState(
    val conversations: List<ConversationResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val showArchived: Boolean = false,
    val showingCache: Boolean = false,
    val guest: Boolean = false,
    val info: AppInfo? = null
)
