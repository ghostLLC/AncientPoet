package com.ancientpoet.android.ui.screen.map

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MapViewModel(private val client: HttpClient) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state

    fun loadMapData(dynastyId: String = "tang") {
        scope.launch {
            try {
                val cities: List<CityRes> = client.get("http://10.0.2.2:8080/api/v1/map/$dynastyId/cities").body()
                _state.value = _state.value.copy(cities = cities.map { CityItem(it.name, it.lat, it.lng, it.mapX, it.mapY, it.isCapital) })
            } catch (e: Exception) { }
        }
    }
}

data class MapState(val cities: List<CityItem> = emptyList(), val distanceKm: Double = 0.0, val estimatedDelay: String = "")
data class CityItem(val name: String, val lat: Double, val lng: Double, val mapX: Int, val mapY: Int, val isCapital: Boolean)

@Serializable data class CityRes(val name: String, val lat: Double, val lng: Double, val mapX: Int, val mapY: Int, val isCapital: Boolean)
