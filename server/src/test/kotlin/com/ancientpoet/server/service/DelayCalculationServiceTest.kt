package com.ancientpoet.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DelayCalculationServiceTest {
    @Test
    fun samePointClampsToTwoHours() {
        val result = DelayCalculationService.calculate(39.9, 116.4, 39.9, 116.4)

        assertEquals(2 * 60 * 60L, result.finalDelaySeconds)
    }

    @Test
    fun settledCoefficientReducesUnclampedDelayByTwentyPercent() {
        val unsettled = DelayCalculationService.calculate(0.0, 0.0, 0.0, 2.0, settled = false)
        val settled = DelayCalculationService.calculate(0.0, 0.0, 0.0, 2.0, settled = true)

        assertEquals((unsettled.finalDelaySeconds * 0.8).toLong(), settled.finalDelaySeconds)
    }

    @Test
    fun warMultiplierIncreasesDelayByFiftyPercent() {
        val normal = DelayCalculationService.calculate(0.0, 0.0, 0.0, 2.0, settled = false)
        val war = DelayCalculationService.calculate(
            0.0,
            0.0,
            0.0,
            2.0,
            settled = false,
            eventDelayMultiplier = 1.5,
        )

        assertEquals((normal.finalDelaySeconds * 1.5).toLong(), war.finalDelaySeconds)
    }

    @Test
    fun veryLongDistanceClampsToSevenDays() {
        val result = DelayCalculationService.calculate(0.0, 0.0, 0.0, 180.0, settled = false)

        assertEquals(7 * 24 * 60 * 60L, result.finalDelaySeconds)
    }

    @Test
    fun haversineDistanceIsSymmetric() {
        val forward = DelayCalculationService.haversineDistance(39.9042, 116.4074, 31.2304, 121.4737)
        val reverse = DelayCalculationService.haversineDistance(31.2304, 121.4737, 39.9042, 116.4074)

        assertTrue(kotlin.math.abs(forward - reverse) < 0.000_001)
    }
}
