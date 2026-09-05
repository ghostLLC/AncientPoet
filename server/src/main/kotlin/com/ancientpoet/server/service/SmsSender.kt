package com.ancientpoet.server.service

import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.plugin.unavailable
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import com.tencentcloudapi.sms.v20210111.SmsClient
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface SmsSender {
    suspend fun send(phone: String, code: String)
}

class TencentSmsSender(private val config: AppConfig) : SmsSender {
    override suspend fun send(phone: String, code: String): Unit = withContext(Dispatchers.IO) {
        if (config.smsProvider != "tencent" ||
            listOf(config.smsAccessKey, config.smsAccessSecret, config.smsSdkAppId, config.smsSignName, config.smsTemplateCode).any(String::isBlank)
        ) {
            unavailable("短信服务尚未配置，请联系维护者")
        }
        val profile = ClientProfile().apply {
            httpProfile = HttpProfile().apply {
                endpoint = "sms.tencentcloudapi.com"
                connTimeout = 10
                readTimeout = 10
                writeTimeout = 10
            }
        }
        val client = SmsClient(Credential(config.smsAccessKey, config.smsAccessSecret), config.smsRegion, profile)
        val request = SendSmsRequest().apply {
            smsSdkAppId = config.smsSdkAppId
            signName = config.smsSignName
            templateId = config.smsTemplateCode
            templateParamSet = arrayOf(code)
            phoneNumberSet = arrayOf("+86$phone")
        }
        val accepted = try {
            client.SendSms(request).sendStatusSet?.singleOrNull()?.code == "Ok"
        } catch (_: Exception) {
            false
        }
        if (!accepted) unavailable("验证码暂未能发送，请稍后重试")
    }
}
