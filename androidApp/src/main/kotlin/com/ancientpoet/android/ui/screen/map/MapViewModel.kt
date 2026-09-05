package com.ancientpoet.android.ui.screen.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.shared.contract.*
import com.ancientpoet.shared.data.api.*
import io.ktor.client.request.setBody
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

class MapViewModel(private val repository: AppRepository) : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state
    private var loading: Job? = null
    private var previewJob: Job? = null
    fun loadMapData(dynastyId: String = "tang", conversationId: Long? = null) {
        loading?.cancel()
        previewJob?.cancel()
        _state.value = MapState(selectedDynasty = dynastyId, conversationId = conversationId, isLoading = true)
        loading = viewModelScope.launch {
            val guest = repository.owner() == 0L
            _state.update { it.copy(guest = guest) }
            repository.cached<List<CityResponse>>("map/" + dynastyId + "/cities", true)?.let { cities -> _state.update { it.copy(cities = cities) } }
            when (val result = repository.fetch<List<CityResponse>>("map/" + dynastyId + "/cities", true)) {
                is ApiResult.Success -> _state.update { it.copy(cities = result.value) }
                is ApiResult.Failure -> failure(result)
            }
            if (!guest) {
                loadUserStatusInternal(dynastyId)
                when (val result = repository.fetch<List<ConversationResponse>>("conversations")) {
                    is ApiResult.Success -> _state.update { it.copy(conversations = result.value.filter { c -> c.dynastyId == dynastyId }) }
                    is ApiResult.Failure -> failure(result)
                }
                if (conversationId != null) {
                    when (val result = repository.fetch<ConversationResponse>("conversations/" + conversationId)) {
                        is ApiResult.Success -> if (result.value.dynastyId == dynastyId) _state.update { it.copy(conversation = result.value) }
                        is ApiResult.Failure -> failure(result)
                    }
                    loadDelivery(conversationId)
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
    fun refresh() = loadMapData(_state.value.selectedDynasty, _state.value.conversationId)
    private suspend fun loadUserStatusInternal(dynastyId: String) {
        when (val result = repository.fetch<MovementRes>("user/location/" + dynastyId + "/status")) {
            is ApiResult.Success -> _state.update { it.copy(movement = result.value) }
            is ApiResult.Failure -> failure(result)
        }
    }
    private suspend fun loadDelivery(id: Long) {
        when (val result = repository.fetch<EstimatedDeliveryDto>("conversations/" + id + "/delivery-preview")) {
            is ApiResult.Success -> _state.update { it.copy(delivery = result.value) }
            is ApiResult.Failure -> if (result.errorCode != "location_required") failure(result)
        }
    }
    fun selectCity(city: CityResponse) {
        previewJob?.cancel()
        _state.update { it.copy(selectedCity = city, preview = null, errorMessage = null) }
        if (_state.value.guest || _state.value.movement?.status == "moving") return
        val dynasty = _state.value.selectedDynasty
        previewJob = viewModelScope.launch {
            when (
                val result = repository.api.post<MovementPreview>("user/location/" + dynasty + "/move-preview", repository.owner()) {
                    setBody(MoveReq(city.name, city.lat, city.lng))
                }
            ) {
                is ApiResult.Success -> if (_state.value.selectedCity == city) _state.update { it.copy(preview = result.value) }
                is ApiResult.Failure -> failure(result)
            }
        }
    }
    fun clearCity() {
        previewJob?.cancel()
        _state.update { it.copy(selectedCity = null, preview = null, errorMessage = null) }
    }
    fun moveTo() {
        if (_state.value.isMoving) return
        val city = _state.value.selectedCity ?: return
        val dynasty = _state.value.selectedDynasty
        _state.update { it.copy(isMoving = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = repository.api.post<MovementRes>("user/location/" + dynasty + "/move", repository.owner()) {
                    setBody(MoveReq(city.name, city.lat, city.lng))
                }
            ) {
                is ApiResult.Success -> {
                    _state.update { it.copy(movement = result.value, isMoving = false, selectedCity = null, preview = null) }
                    _state.value.conversationId?.let { loadDelivery(it) }
                    repository.changes.tryEmit(Unit)
                }

                is ApiResult.Failure -> {
                    _state.update { it.copy(isMoving = false) }
                    failure(result)
                }
            }
        }
    }
    private fun failure(result: ApiResult.Failure) {
        _state.update { it.copy(errorMessage = result.message, canRetry = result.retryable) }
    }
}
data class MapState(
    val selectedDynasty: String = "tang",
    val cities: List<CityResponse> = emptyList(),
    val selectedCity: CityResponse? = null,
    val conversationId: Long? = null,
    val conversation: ConversationResponse? = null,
    val conversations: List<ConversationResponse> = emptyList(),
    val movement: MovementRes? = null,
    val delivery: EstimatedDeliveryDto? = null,
    val preview: MovementPreview? = null,
    val isLoading: Boolean = false,
    val isMoving: Boolean = false,
    val guest: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false
)

@Serializable data class MoveReq(val toName: String, val toLat: Double, val toLng: Double)

@Serializable data class MovementRes(
    val status: String,
    val currentName: String,
    val currentLat: Double,
    val currentLng: Double,
    val movingToName: String?,
    val movingToLat: Double?,
    val movingToLng: Double?,
    val remainingSeconds: Long
)
