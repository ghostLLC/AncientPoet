package com.ancientpoet.android.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LoginViewModel(private val client: HttpClient) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    fun sendSms(phone: String) {
        viewModelScope.launch {
            try {
                client.post("http://10.0.2.2:8080/api/v1/auth/sms/send") {
                    contentType(ContentType.Application.Json)
                    setBody(SmsRequest(phone))
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "发送失败: ${e.message}")
            }
        }
    }

    fun verifySms(phone: String, code: String) {
        viewModelScope.launch {
            try {
                val response: VerifyResponse = client.post("http://10.0.2.2:8080/api/v1/auth/sms/verify") {
                    contentType(ContentType.Application.Json)
                    setBody(VerifyRequest(phone, code))
                }.body()
                // Store token for future API calls
                _state.value = _state.value.copy(verified = true, token = response.accessToken)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "验证失败: ${e.message}")
            }
        }
    }
}

data class LoginState(val verified: Boolean = false, val token: String? = null, val error: String? = null)
@Serializable data class SmsRequest(val phone: String)
@Serializable data class VerifyRequest(val phone: String, val code: String)
@Serializable data class VerifyResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val userId: Long)
