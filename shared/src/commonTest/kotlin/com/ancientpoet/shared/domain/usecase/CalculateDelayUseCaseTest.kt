package com.ancientpoet.shared.domain.usecase

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateDelayUseCaseTest {
    private val calculate = CalculateDelayUseCase()

    @Test
    fun distanceBelowThirtyKilometersMapsToTwoHours() {
        val result = calculate(0.0, 0.0, 0.0, 0.26)

        assertTrue(result.distanceKm < 30.0)
        assertEquals(2.0, result.estimatedHours)
    }

    @Test
    fun distanceFromThirtyToUnderOneHundredFiftyKilometersMapsToOneDay() {
        val lowerRange = calculate(0.0, 0.0, 0.0, 0.27)
        val upperRange = calculate(0.0, 0.0, 0.0, 1.34)

        assertTrue(lowerRange.distanceKm >= 30.0)
        assertTrue(upperRange.distanceKm < 150.0)
        assertEquals(24.0, lowerRange.estimatedHours)
        assertEquals(24.0, upperRange.estimatedHours)
    }

    @Test
    fun distanceAtOrAboveOneHundredFiftyKilometersUsesTravelSpeed() {
        val result = calculate(0.0, 0.0, 0.0, 1.35)
        val expectedHours = result.distanceKm / 80.0 * 24.0

        assertTrue(result.distanceKm >= 150.0)
        assertTrue(abs(expectedHours - result.estimatedHours) < 0.000_001)
    }
}
