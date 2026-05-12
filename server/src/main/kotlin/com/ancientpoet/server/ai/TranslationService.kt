package com.ancientpoet.server.ai

import com.ancientpoet.server.config.AppConfig

class TranslationService(
    private val deepSeekClient: DeepSeekClient,
    private val config: AppConfig,
) {
    suspend fun translateToVernacular(classicalText: String): String {
        return deepSeekClient.chatCompletion(
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "你是一位精通古文的翻译专家。请将以下文言文书信翻译为通俗易懂的现代白话文。保留原文的情感和意境，但语言要现代化。只输出翻译结果，不要添加任何解释。",
                ),
                ChatMessage(role = "user", content = classicalText),
            ),
            temperature = 0.3,
        )
    }
}
