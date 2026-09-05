package com.ancientpoet.android.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.android.data.session.SessionController
import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.contract.AppInfo
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LoginViewModel(private val api: AncientPoetApi, private val sessions: SessionController) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state
    init {
        viewModelScope.launch {
            val result = api.get<AppInfo>("info")
            if (result is ApiResult.Success) _state.update { it.copy(development = result.value.development) }
        }
    }
    fun sendSms(phone: String) {
        if (_state.value.isLoading) return
        if (!phone.matches(Regex("1[3-9][0-9]{9}"))) {
            fail("请输入有效的 11 位手机号")
            return
        }
        if (_state.value.sentPhone == phone && _state.value.cooldown > 0) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = api.post<SmsResponse>("auth/sms/send") { setBody(SmsRequest(phone)) }) {
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }

                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false, sentPhone = phone, cooldown = 60) }
                    repeat(60) {
                        delay(1000)
                        _state.update { state -> if (state.sentPhone == phone) state.copy(cooldown = (state.cooldown - 1).coerceAtLeast(0)) else state }
                    }
                }
            }
        }
    }
    fun verifySms(phone: String, code: String) {
        if (_state.value.isLoading) return
        if (!phone.matches(Regex("1[3-9][0-9]{9}")) || !code.matches(Regex("[0-9]{6}"))) {
            fail("请检查手机号与 6 位验证码")
            return
        }
        if (_state.value.sentPhone != phone) {
            fail("请先为这个手机号获取验证码")
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = api.post<VerifyResponse>("auth/sms/verify") { setBody(VerifyRequest(phone, code)) }) {
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }

                is ApiResult.Success -> try {
                    sessions.save(AuthTokens(result.value.accessToken, result.value.refreshToken, result.value.userId))
                    _state.update { it.copy(isLoading = false, verified = true) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    fail("无法安全保存登录信息，请检查设备存储后重试")
                }
            }
        }
    }
    private fun fail(message: String) {
        _state.update { it.copy(isLoading = false, errorMessage = message) }
    }
}
data class LoginState(
    val verified: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sentPhone: String? = null,
    val cooldown: Int = 0,
    val development: Boolean = false
)

@Serializable data class SmsRequest(val phone: String)

@Serializable data class VerifyRequest(val phone: String, val code: String)

@Serializable data class VerifyResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long, val userId: Long)

@Serializable data class SmsResponse(val message: String? = null)
