package com.ancientpoet.shared.util

import kotlinx.datetime.*

object DateTimeUtil {
    fun now(): Instant = Clock.System.now()
    fun nowLocal(): LocalDateTime = now().toLocalDateTime(TimeZone.currentSystemDefault())
    fun formatDelay(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        return when {
            days > 0 -> "${days}天${hours}小时"
            hours > 0 -> "${hours}小时"
            else -> "不到1小时"
        }
    }
}
