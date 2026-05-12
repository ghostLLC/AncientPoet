package com.ancientpoet.android.ui.screen.poet

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class PoetViewModel(private val client: HttpClient) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(PoetState())
    val state: StateFlow<PoetState> = _state

    fun loadPoets() {
        scope.launch {
            try {
                val response: PoetListRes = client.get("http://10.0.2.2:8080/api/v1/poets").body()
                _state.value = _state.value.copy(poets = response.poets.map { PoetItem(it.id, it.name, it.dynastyName ?: "", it.birthYear, it.deathYear) })
            } catch (e: Exception) { }
        }
    }

    fun loadPoetDetail(poetId: Long) {
        scope.launch {
            try {
                val p: PoetDetailRes = client.get("http://10.0.2.2:8080/api/v1/poets/$poetId").body()
                _state.value = _state.value.copy(selectedPoet = PoetDetail(p.id, p.name, p.courtesyName, p.artName, p.dynastyName ?: "", p.birthYear, p.deathYear, p.biographySummary, p.writingStyle))
            } catch (e: Exception) { }
        }
    }
}

data class PoetState(val poets: List<PoetItem> = emptyList(), val selectedPoet: PoetDetail? = null)
data class PoetItem(val id: Long, val name: String, val dynastyName: String, val birthYear: Int, val deathYear: Int)
data class PoetDetail(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)

@Serializable data class PoetListRes(val poets: List<PoetBriefRes>)
@Serializable data class PoetBriefRes(val id: Long, val name: String, val dynastyName: String?, val birthYear: Int, val deathYear: Int)
@Serializable data class PoetDetailRes(val id: Long, val name: String, val courtesyName: String?, val artName: String?, val dynastyName: String?, val birthYear: Int, val deathYear: Int, val biographySummary: String?, val writingStyle: String)
