package com.ancientpoet.android.ui.screen.poet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.shared.contract.*
import com.ancientpoet.shared.data.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PoetViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(PoetState())
    val state: StateFlow<PoetState> = _state
    private var listJob: Job? = null
    private var detailJob: Job? = null
    fun loadPoets() {
        if (listJob?.isActive == true) return
        listJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, listError = null) }
            repository.cached<PoetListResponse>("poets", true)?.let { p -> _state.update { it.copy(poets = p.poets) } }
            when (val result = repository.fetch<PoetListResponse>("poets", true)) {
                is ApiResult.Success -> _state.update { it.copy(poets = result.value.poets, isLoading = false, listError = null) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, listError = result.operation()) }
            }
        }
    }
    fun loadPoetDetailInitial(id: Long) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val path = "poets/" + id
            _state.update {
                it.copy(
                    isLoading = true,
                    selectedPoet = repository.cached(path, true),
                    lifeEvents = repository.cached<List<LifeEventResponse>>(path + "/life-events", true).orEmpty(),
                    detailError = null,
                    lifeEventsError = null
                )
            }
            coroutineScope {
                val detail = async { repository.fetch<PoetDetailResponse>(path, true) }
                val events = async { repository.fetch<List<LifeEventResponse>>(path + "/life-events", true) }
                val location = async { repository.fetch<PoetLocationResponse>(path + "/location", true) }
                when (val result = detail.await()) {
                    is ApiResult.Success -> _state.update { it.copy(selectedPoet = result.value, detailError = null) }
                    is ApiResult.Failure -> _state.update { it.copy(detailError = result.operation()) }
                }
                when (val result = events.await()) {
                    is ApiResult.Success -> _state.update { it.copy(lifeEvents = result.value, lifeEventsError = null) }
                    is ApiResult.Failure -> _state.update { it.copy(lifeEventsError = result.operation()) }
                }
                when (val result = location.await()) {
                    is ApiResult.Success -> _state.update { it.copy(location = result.value) }
                    is ApiResult.Failure -> Unit
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
    fun retryPoetDetail(id: Long) = loadPoetDetailInitial(id)
}
private fun ApiResult.Failure.operation() = PoetOperationError(message, retryable)
data class PoetState(
    val poets: List<PoetItem> = emptyList(),
    val selectedPoet: PoetDetailResponse? = null,
    val lifeEvents: List<LifeEventResponse> = emptyList(),
    val location: PoetLocationResponse? = null,
    val isLoading: Boolean = false,
    val listError: PoetOperationError? = null,
    val detailError: PoetOperationError? = null,
    val lifeEventsError: PoetOperationError? = null
) {
    val errorMessage: String? get() = listOfNotNull(listError, detailError, lifeEventsError).map { it.message }.distinct().joinToString("；").ifBlank { null }
    val canRetry: Boolean get() = listOfNotNull(listError, detailError, lifeEventsError).any { it.retryable }
}
data class PoetOperationError(val message: String, val retryable: Boolean)
