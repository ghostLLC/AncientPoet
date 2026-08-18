package com.ancientpoet.shared.util

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceUtilTest {
    @Test
    fun identicalCoordinatesReturnZero() {
        assertEquals(0.0, DistanceUtil.haversineDistance(39.9042, 116.4074, 39.9042, 116.4074))
    }

    @Test
    fun beijingToShanghaiIsApproximatelyOneThousandSixtyEightKilometers() {
        val distance = DistanceUtil.haversineDistance(39.9042, 116.4074, 31.2304, 121.4737)

        assertTrue(distance in 1_040.0..1_090.0, "distance was $distance km")
    }

    @Test
    fun distanceIsSymmetric() {
        val forward = DistanceUtil.haversineDistance(39.9042, 116.4074, 31.2304, 121.4737)
        val reverse = DistanceUtil.haversineDistance(31.2304, 121.4737, 39.9042, 116.4074)

        assertTrue(abs(forward - reverse) < 0.000_001)
    }
}
