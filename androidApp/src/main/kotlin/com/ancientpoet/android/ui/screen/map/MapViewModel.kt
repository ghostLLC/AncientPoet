package com.ancientpoet.android.ui.screen.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MapViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state

    fun loadMapData(dynastyId: String = "tang") {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<CityRes>>("map/$dynastyId/cities")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    selectedDynasty = dynastyId,
                    cities = result.value.map { CityItem(it.name, it.lat, it.lng, it.mapX, it.mapY, it.isCapital) },
                    isLoading = false,
                    errorMessage = null,
                )
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadUserStatus(dynastyId: String, userId: Long = 1) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<MovementRes>("user/location/$dynastyId/status")) {
                is ApiResult.Success -> {
                    val status = result.value
                    _state.value = _state.value.copy(
                        userLocation = LocationInfo(status.currentName, status.currentLat, status.currentLng),
                        userStatus = status.status,
                        movingTo = status.movingToName?.let { LocationInfo(it, status.movingToLat ?: 0.0, status.movingToLng ?: 0.0) },
                        remainingSeconds = status.remainingSeconds,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun moveTo(dynastyId: String, city: CityItem) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.post<MessageResponse>("user/location/$dynastyId/move", MoveReq(city.name, city.lat, city.lng))) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = null)
                    loadUserStatus(dynastyId)
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun selectCity(city: CityItem) {
        _state.value = _state.value.copy(selectedCity = city)
        val poet = _state.value.poetLocation
        if (poet != null) {
            val dist = haversine(city.lat, city.lng, poet.lat, poet.lng)
            val hours = when {
                dist < 30 -> 2.0
                dist < 150 -> 24.0
                else -> (dist / 80.0) * 24.0
            }
            _state.value = _state.value.copy(
                distanceKm = Math.round(dist * 10) / 10.0,
                estimatedDelay = formatDelay(hours),
            )
        }
    }

    private fun setLoading() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
    }

    private fun setFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable)
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2).let { it * it }
        return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun formatDelay(hours: Double): String {
        val days = (hours / 24).toInt()
        val h = (hours % 24).toInt()
        return when {
            days > 1 -> "${days}天${h}小时"
            days == 1 -> "1天${h}小时"
            h > 1 -> "${h}小时"
            else -> "约2小时"
        }
    }
}

data class MapState(
    val selectedDynasty: String = "tang",
    val cities: List<CityItem> = emptyList(),
    val selectedCity: CityItem? = null,
    val userLocation: LocationInfo? = null,
    val poetLocation: LocationInfo? = LocationInfo("长安", 34.26, 108.94),
    val userStatus: String = "settled",
    val movingTo: LocationInfo? = null,
    val remainingSeconds: Long = 0,
    val distanceKm: Double = 0.0,
    val estimatedDelay: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
)

data class CityItem(val name: String, val lat: Double, val lng: Double, val mapX: Int, val mapY: Int, val isCapital: Boolean)
data class LocationInfo(val name: String, val lat: Double, val lng: Double)

@Serializable data class CityRes(val name: String, val lat: Double, val lng: Double, val mapX: Int, val mapY: Int, val isCapital: Boolean)
@Serializable data class MoveReq(val toName: String, val toLat: Double, val toLng: Double)
@Serializable data class MovementRes(val status: String, val currentName: String, val currentLat: Double, val currentLng: Double, val movingToName: String?, val movingToLat: Double?, val movingToLng: Double?, val remainingSeconds: Long)
@Serializable data class MessageResponse(val message: String? = null)
