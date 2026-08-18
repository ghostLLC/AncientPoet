package com.ancientpoet.android.ui.screen.poetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class PoetryViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(PoetryState())
    val state: StateFlow<PoetryState> = _state

    fun loadAllPoems() {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<PoemItem>>("poems")) {
                is ApiResult.Success -> setPoems(result.value.map { it.toDisplay() })
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadPoetPoems(poetId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<PoemItem>>("poets/$poetId/poems")) {
                is ApiResult.Success -> setPoems(result.value.map { it.toDisplay() })
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun searchPoems(query: String) {
        if (query.isBlank()) {
            loadAllPoems()
            return
        }
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<PoemSearchResult>>("poems/search?q=${query.encodeForQuery()}")) {
                is ApiResult.Success -> setPoems(result.value.map { it.toDisplay() })
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadPoemDetail(poemId: Long) {
        val cached = _state.value.poems.find { it.id == poemId }
        if (cached != null) {
            _state.value = _state.value.copy(selectedPoem = PoemDetail(
                id = cached.id,
                title = cached.title,
                poetName = cached.poetName,
                dynasty = cached.dynasty,
                content = cached.preview,
                yearWritten = null,
                context = null,
                translation = null,
                appreciation = null,
            ))
        } else {
            _state.value = _state.value.copy(errorMessage = "诗词内容暂不可用", canRetry = false)
        }
    }

    private fun setLoading() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
    }

    private fun setPoems(poems: List<PoemDisplay>) {
        _state.value = _state.value.copy(poems = poems, isLoading = false, errorMessage = null, canRetry = false)
    }

    private fun setFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable)
    }

    private fun String.encodeForQuery(): String = replace(" ", "%20").replace("&", "%26").replace("?", "%3F")
}

data class PoetryState(
    val poems: List<PoemDisplay> = emptyList(),
    val selectedPoem: PoemDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
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
