package com.ancientpoet.android.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ReadingOptions(val theme: String = "system", val textScale: Float = 1f, val notifications: Boolean = false)
class ReadingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("reading_preferences", Context.MODE_PRIVATE)
    private val _options = MutableStateFlow(
        ReadingOptions(
            prefs.getString("theme", "system") ?: "system",
            prefs.getFloat("text_scale", 1f).coerceIn(0.9f, 1.3f),
            prefs.getBoolean("notifications", false)
        )
    )
    val options: StateFlow<ReadingOptions> = _options
    fun theme(value: String) {
        require(value in listOf("system", "light", "dark"))
        prefs.edit().putString("theme", value).apply()
        _options.value = _options.value.copy(theme = value)
    }
    fun textScale(value: Float) {
        val scale = value.coerceIn(0.9f, 1.3f)
        prefs.edit().putFloat("text_scale", scale).apply()
        _options.value = _options.value.copy(textScale = scale)
    }
    fun notifications(value: Boolean) {
        prefs.edit().putBoolean("notifications", value).apply()
        _options.value = _options.value.copy(notifications = value)
    }
}
