package com.ancientpoet.android.ui.screen.poetry

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class PoetryViewModel(private val client: HttpClient) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(PoetryState())
    val state: StateFlow<PoetryState> = _state

    fun loadAllPoems() {
        scope.launch {
            try {
                val poems: List<PoemItem> = client.get("http://10.0.2.2:8080/api/v1/poems").body()
                _state.value = _state.value.copy(poems = poems.map { it.toDisplay() })
            } catch (_: Exception) {}
        }
    }

    fun loadPoetPoems(poetId: Long) {
        scope.launch {
            try {
                val poems: List<PoemItem> = client.get("http://10.0.2.2:8080/api/v1/poets/$poetId/poems").body()
                _state.value = _state.value.copy(poems = poems.map { it.toDisplay() })
            } catch (_: Exception) {}
        }
    }

    fun searchPoems(query: String) {
        if (query.isBlank()) { loadAllPoems(); return }
        scope.launch {
            try {
                val poems: List<PoemSearchResult> = client.get("http://10.0.2.2:8080/api/v1/poems/search?q=$query").body()
                _state.value = _state.value.copy(poems = poems.map { PoemDisplay(it.id, it.title, it.poetName, it.tags.firstOrNull() ?: "", it.content) })
            } catch (_: Exception) {}
        }
    }

    fun loadPoemDetail(poemId: Long) {
        // Search across cached poems; in production, add GET /poems/{id} endpoint
        val cached = _state.value.poems.find { it.id == poemId }
        if (cached != null) {
            _state.value = _state.value.copy(selectedPoem = PoemDetail(
                id = cached.id, title = cached.title, poetName = cached.poetName,
                dynasty = cached.dynasty, content = cached.preview,
                yearWritten = null, context = null, translation = null, appreciation = null,
            ))
        }
    }
}

data class PoetryState(
    val poems: List<PoemDisplay> = emptyList(),
    val selectedPoem: PoemDetail? = null,
)
data class PoemDisplay(val id: Long, val title: String, val poetName: String, val dynasty: String, val preview: String)
data class PoemDetail(
    val id: Long, val title: String, val poetName: String, val dynasty: String,
    val content: String, val yearWritten: Int?, val context: String?,
    val translation: String?, val appreciation: String?,
)

@Serializable data class PoemItem(val id: Long, val title: String, val content: String, val yearWritten: Int? = null, val context: String? = null, val translation: String? = null, val appreciation: String? = null, val tags: List<String> = emptyList())
@Serializable data class PoemSearchResult(val id: Long, val title: String, val poetName: String, val content: String, val tags: List<String> = emptyList())

private fun PoemItem.toDisplay() = PoemDisplay(id, title, "", "", content.take(40))
private fun PoemSearchResult.toDisplay() = PoemDisplay(id, title, poetName, tags.firstOrNull() ?: "", content.take(40))
