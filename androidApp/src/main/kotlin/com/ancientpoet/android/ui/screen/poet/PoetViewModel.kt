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
            setLoading()
            when (val result = api.get<PoetListRes>("poets")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    poets = result.value.poets.map { PoetItem(it.id, it.name, it.dynastyName ?: "", it.birthYear, it.deathYear) },
                    isLoading = false,
                    errorMessage = null,
                )
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadPoetDetail(poetId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<PoetDetailRes>("poets/$poetId")) {
                is ApiResult.Success -> {
                    val p = result.value
                    _state.value = _state.value.copy(
                        selectedPoet = PoetDetail(p.id, p.name, p.courtesyName, p.artName, p.dynastyName ?: "", p.birthYear, p.deathYear, p.biographySummary, p.writingStyle),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadLifeEvents(poetId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<LifeEventRes>>("poets/$poetId/life-events")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    lifeEvents = result.value.map { LifeEventItem(it.year, it.title, it.description, it.eventType) },
                    isLoading = false,
                    errorMessage = null,
                )
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

data class PoetState(
    val poets: List<PoetItem> = emptyList(),
    val selectedPoet: PoetDetail? = null,
    val lifeEvents: List<LifeEventItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
)

data class PoetItem(val id: Long, val name: String, val dynastyName: String, val birthYear: Int, val deathYear: Int)
data class PoetDetail(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
data class LifeEventItem(val year: Int, val title: String, val description: String, val eventType: String)

@Serializable data class PoetListRes(val poets: List<PoetBriefRes>)
@Serializable data class PoetBriefRes(val id: Long, val name: String, val dynastyName: String?, val birthYear: Int, val deathYear: Int)
@Serializable data class PoetDetailRes(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String?, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
@Serializable data class LifeEventRes(val id: Long, val year: Int, val age: Int, val title: String, val description: String, val locationName: String?, val eventType: String, val delayMultiplier: Double)
