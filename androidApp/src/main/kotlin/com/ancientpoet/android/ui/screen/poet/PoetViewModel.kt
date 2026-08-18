package com.ancientpoet.android.ui.screen.poet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class PoetViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(PoetState())
    val state: StateFlow<PoetState> = _state

    fun loadPoets() {
        viewModelScope.launch {
            beginListLoad()
            when (val result = api.get<PoetListRes>("poets")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    poets = result.value.poets.map { PoetItem(it.id, it.name, it.dynastyName ?: "", it.birthYear, it.deathYear) },
                    isLoading = false,
                    listError = null,
                )
                is ApiResult.Failure -> setListFailure(result)
            }
        }
    }

    fun loadPoetDetail(poetId: Long) {
        viewModelScope.launch { loadPoetDetailInternal(poetId) }
    }

    fun loadLifeEvents(poetId: Long) {
        viewModelScope.launch { loadLifeEventsInternal(poetId) }
    }

    fun loadPoetDetailInitial(poetId: Long) {
        viewModelScope.launch {
            loadPoetDetailInternal(poetId)
            loadLifeEventsInternal(poetId)
        }
    }

    fun retryPoetDetail(poetId: Long) {
        viewModelScope.launch {
            val current = _state.value
            if (current.detailError?.retryable == true) {
                loadPoetDetailInternal(poetId)
            }
            if (current.lifeEventsError?.retryable == true) {
                loadLifeEventsInternal(poetId)
            }
        }
    }

    private suspend fun loadPoetDetailInternal(poetId: Long) {
        beginDetailLoad()
        when (val result = api.get<PoetDetailRes>("poets/$poetId")) {
            is ApiResult.Success -> {
                val p = result.value
                _state.value = _state.value.copy(
                    selectedPoet = PoetDetail(p.id, p.name, p.courtesyName, p.artName, p.dynastyName ?: "", p.birthYear, p.deathYear, p.biographySummary, p.writingStyle),
                    isLoading = false,
                    detailError = null,
                )
            }
            is ApiResult.Failure -> setDetailFailure(result)
        }
    }

    private suspend fun loadLifeEventsInternal(poetId: Long) {
        beginLifeEventsLoad()
        when (val result = api.get<List<LifeEventRes>>("poets/$poetId/life-events")) {
            is ApiResult.Success -> _state.value = _state.value.copy(
                lifeEvents = result.value.map { LifeEventItem(it.year, it.title, it.description, it.eventType) },
                isLoading = false,
                lifeEventsError = null,
            )
            is ApiResult.Failure -> setLifeEventsFailure(result)
        }
    }

    private fun beginListLoad() {
        _state.value = _state.value.copy(isLoading = true, listError = null)
    }

    private fun beginDetailLoad() {
        _state.value = _state.value.copy(isLoading = true, detailError = null)
    }

    private fun beginLifeEventsLoad() {
        _state.value = _state.value.copy(isLoading = true, lifeEventsError = null)
    }

    private fun setListFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, listError = PoetOperationError(result.message, result.retryable))
    }

    private fun setDetailFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, detailError = PoetOperationError(result.message, result.retryable))
    }

    private fun setLifeEventsFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, lifeEventsError = PoetOperationError(result.message, result.retryable))
    }
}

data class PoetState(
    val poets: List<PoetItem> = emptyList(),
    val selectedPoet: PoetDetail? = null,
    val lifeEvents: List<LifeEventItem> = emptyList(),
    val isLoading: Boolean = false,
    val listError: PoetOperationError? = null,
    val detailError: PoetOperationError? = null,
    val lifeEventsError: PoetOperationError? = null,
) {
    val errorMessage: String?
        get() = listOfNotNull(listError, detailError, lifeEventsError)
            .joinToString("；") { it.message }
            .takeIf { it.isNotEmpty() }

    val canRetry: Boolean
        get() = listOfNotNull(listError, detailError, lifeEventsError).any { it.retryable }
}

data class PoetOperationError(val message: String, val retryable: Boolean)

data class PoetItem(val id: Long, val name: String, val dynastyName: String, val birthYear: Int, val deathYear: Int)
data class PoetDetail(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
data class LifeEventItem(val year: Int, val title: String, val description: String, val eventType: String)

@Serializable data class PoetListRes(val poets: List<PoetBriefRes>)
@Serializable data class PoetBriefRes(val id: Long, val name: String, val dynastyName: String?, val birthYear: Int, val deathYear: Int)
@Serializable data class PoetDetailRes(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String?, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
@Serializable data class LifeEventRes(val id: Long, val year: Int, val age: Int, val title: String, val description: String, val locationName: String?, val eventType: String, val delayMultiplier: Double)
