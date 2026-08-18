package com.ancientpoet.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.session.SessionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SessionViewModel(private val sessionController: SessionController) : ViewModel() {
    private val _status = MutableStateFlow<SessionStatus>(SessionStatus.Checking)
    val status: StateFlow<SessionStatus> = _status

    init {
        viewModelScope.launch {
            sessionController.sessionEvents.collect { authenticated ->
                _status.value = when (authenticated) {
                    true -> SessionStatus.Authenticated
                    false -> SessionStatus.Anonymous
                    null -> if (sessionController.load() == null) {
                        SessionStatus.Anonymous
                    } else {
                        SessionStatus.Authenticated
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionController.logout()
        }
    }
}

sealed interface SessionStatus {
    data object Checking : SessionStatus
    data object Authenticated : SessionStatus
    data object Anonymous : SessionStatus
}
