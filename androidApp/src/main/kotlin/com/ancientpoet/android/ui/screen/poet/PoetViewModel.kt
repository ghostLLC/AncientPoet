package com.ancientpoet.android.ui.screen.poet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class PoetViewModel(private val client: HttpClient) : ViewModel() {
    private val _state = MutableStateFlow(PoetState())
    val state: StateFlow<PoetState> = _state

    fun loadPoets() {
        viewModelScope.launch {
            try {
                val response: PoetListRes = client.get("http://10.0.2.2:8080/api/v1/poets").body()
                _state.value = _state.value.copy(poets = response.poets.map { PoetItem(it.id, it.name, it.dynastyName ?: "", it.birthYear, it.deathYear) })
            } catch (_: Exception) { }
        }
    }

    fun loadPoetDetail(poetId: Long) {
        viewModelScope.launch {
            try {
                val p: PoetDetailRes = client.get("http://10.0.2.2:8080/api/v1/poets/$poetId").body()
                _state.value = _state.value.copy(selectedPoet = PoetDetail(p.id, p.name, p.courtesyName, p.artName, p.dynastyName ?: "", p.birthYear, p.deathYear, p.biographySummary, p.writingStyle))
            } catch (_: Exception) { }
        }
    }

    fun loadLifeEvents(poetId: Long) {
        viewModelScope.launch {
            try {
                val events: List<LifeEventRes> = client.get("http://10.0.2.2:8080/api/v1/poets/$poetId/life-events").body()
                _state.value = _state.value.copy(lifeEvents = events.map { LifeEventItem(it.year, it.title, it.description, it.eventType) })
            } catch (_: Exception) { }
        }
    }
}

data class PoetState(
    val poets: List<PoetItem> = emptyList(),
    val selectedPoet: PoetDetail? = null,
    val lifeEvents: List<LifeEventItem> = emptyList(),
)
data class PoetItem(val id: Long, val name: String, val dynastyName: String, val birthYear: Int, val deathYear: Int)
data class PoetDetail(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
data class LifeEventItem(val year: Int, val title: String, val description: String, val eventType: String)

@Serializable data class PoetListRes(val poets: List<PoetBriefRes>)
@Serializable data class PoetBriefRes(val id: Long, val name: String, val dynastyName: String?, val birthYear: Int, val deathYear: Int)
@Serializable data class PoetDetailRes(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String?, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
@Serializable data class LifeEventRes(val id: Long, val year: Int, val age: Int, val title: String, val description: String, val locationName: String?, val eventType: String, val delayMultiplier: Double)
