package com.ancientpoet.android.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.session.SessionController
import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LoginViewModel(
    private val api: AncientPoetApi,
    private val sessionController: SessionController,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    fun sendSms(phone: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
            when (val result = api.post<MessageResponse>("auth/sms/send") { setBody(SmsRequest(phone)) }) {
                is ApiResult.Success -> _state.value = _state.value.copy(isLoading = false)
                is ApiResult.Failure -> _state.value = result.toState()
            }
        }
    }

    fun verifySms(phone: String, code: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
            when (val result = api.post<VerifyResponse>("auth/sms/verify") { setBody(VerifyRequest(phone, code)) }) {
                is ApiResult.Success -> {
                    sessionController.save(
                        AuthTokens(
                            accessToken = result.value.accessToken,
                            refreshToken = result.value.refreshToken,
                            userId = result.value.userId,
                        ),
                    )
                    _state.value = _state.value.copy(isLoading = false, verified = true, errorMessage = null)
                }
                is ApiResult.Failure -> _state.value = result.toState()
            }
        }
    }

    private fun ApiResult.Failure.toState() = LoginState(
        isLoading = false,
        errorMessage = message,
        canRetry = retryable,
    )
}

data class LoginState(
    val verified: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
)

@Serializable data class SmsRequest(val phone: String)
@Serializable data class VerifyRequest(val phone: String, val code: String)
@Serializable data class VerifyResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val userId: Long)
@Serializable data class MessageResponse(val message: String? = null)
