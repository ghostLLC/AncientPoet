package com.ancientpoet.android.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class HomeViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
            when (val result = api.get<List<ConvResponse>>("conversations")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    conversations = result.value.map { ConvItem(it.id, it.poet.name, it.poet.dynasty, "") },
                    isLoading = false,
                )
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                    canRetry = result.retryable,
                )
            }
        }
    }
}

data class HomeState(
    val conversations: List<ConvItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
)

data class ConvItem(val id: Long, val poetName: String, val dynasty: String, val lastMessage: String)

@Serializable data class ConvResponse(val id: Long, val poet: PoetBrief, val mode: String, val dynastyId: String)
@Serializable data class PoetBrief(val id: Long, val name: String, val dynasty: String)
