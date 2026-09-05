package com.ancientpoet.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.session.SessionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SessionViewModel(private val sessionController: SessionController) : ViewModel() {
    private val _status = MutableStateFlow<SessionStatus>(SessionStatus.Checking)
    val status: StateFlow<SessionStatus> = _status
    init {
        viewModelScope.launch {
            sessionController.identityRevision.collect {
                _status.value = sessionController.load()?.let { SessionStatus.Authenticated(it.userId) } ?: SessionStatus.Anonymous
            }
        }
    }
    fun logout() {
        viewModelScope.launch { sessionController.logout() }
    }
}
sealed interface SessionStatus {
    data object Checking : SessionStatus
    data class Authenticated(val userId: Long) : SessionStatus
    data object Anonymous : SessionStatus
}
