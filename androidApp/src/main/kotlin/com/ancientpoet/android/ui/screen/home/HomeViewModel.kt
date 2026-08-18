package com.ancientpoet.android.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class HomeViewModel(private val client: HttpClient) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init { loadConversations() }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val response: List<ConvResponse> = client.get("http://10.0.2.2:8080/api/v1/conversations").body()
                _state.value = _state.value.copy(conversations = response.map { ConvItem(it.id, it.poet.name, it.poet.dynasty, "") })
            } catch (e: Exception) {
                // Silently fail; user sees empty state
            }
        }
    }
}

data class HomeState(val conversations: List<ConvItem> = emptyList())
data class ConvItem(val id: Long, val poetName: String, val dynasty: String, val lastMessage: String)

@Serializable data class ConvResponse(val id: Long, val poet: PoetBrief, val mode: String, val dynastyId: String)
@Serializable data class PoetBrief(val id: Long, val name: String, val dynasty: String)
