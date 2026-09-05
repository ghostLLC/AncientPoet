package com.ancientpoet.android.ui.screen.poetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.shared.contract.PoemDetail
import com.ancientpoet.shared.data.api.*
import java.net.URLEncoder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PoetryViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(PoetryState())
    val state: StateFlow<PoetryState> = _state
    private var searchJob: Job? = null
    private var requestKey = ""
    fun search(query: String, poetId: Long? = null, more: Boolean = false) {
        val key = query + "|" + poetId
        searchJob?.cancel()
        requestKey = key
        val offset = if (more) _state.value.poems.size else 0
        _state.update { it.copy(isLoading = true, errorMessage = null, poems = if (more) it.poems else emptyList()) }
        searchJob = viewModelScope.launch {
            if (query.isNotBlank() && !more) delay(350)
            val path = "poems?q=" + URLEncoder.encode(query, "UTF-8") + (poetId?.let { "&poetId=" + it } ?: "") + "&offset=" + offset
            if (!more) repository.cached<List<PoemDetail>>(path, true)?.let { cached -> _state.update { it.copy(poems = cached, showingCache = true) } }
            when (val result = repository.fetch<List<PoemDetail>>(path, true)) {
                is ApiResult.Success -> if (requestKey == key) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            poems = (if (more) it.poems + result.value else result.value).distinctBy { p -> p.id },
                            hasMore = result.value.size == 40,
                            showingCache = false
                        )
                    }
                }

                is ApiResult.Failure -> if (requestKey == key) _state.update { it.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable) }
            }
        }
    }
    fun loadPoemDetail(id: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val path = "poems/" + id
            _state.update { it.copy(isLoading = true, selectedPoem = repository.cached(path, true), errorMessage = null) }
            when (val result = repository.fetch<PoemDetail>(path, true)) {
                is ApiResult.Success -> _state.update { it.copy(selectedPoem = result.value, isLoading = false) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable) }
            }
        }
    }
}
data class PoetryState(
    val poems: List<PoemDetail> = emptyList(),
    val selectedPoem: PoemDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val hasMore: Boolean = false,
    val showingCache: Boolean = false
)
