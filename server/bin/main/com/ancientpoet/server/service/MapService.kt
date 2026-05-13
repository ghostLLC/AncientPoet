package com.ancientpoet.server.service

import com.ancientpoet.server.model.dto.*

object MapService {
    fun getDelayPreview(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): DelayPreviewResponse {
        val result = DelayCalculationService.calculate(fromLat, fromLng, toLat, toLng)
        val hours = result.baseDelayHours
        val days = (hours / 24).toInt()
        val remainHours = (hours % 24).toInt()
        val estimated = when {
            days > 1 -> "${days}天${remainHours}小时"
            days == 1 -> "1天${remainHours}小时"
            remainHours > 1 -> "${remainHours}小时"
            else -> "约2小时"
        }
        return DelayPreviewResponse(
            distanceKm = result.distanceKm,
            baseDelayHours = result.baseDelayHours,
            estimatedDelay = estimated,
        )
    }
}
