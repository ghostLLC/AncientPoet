package com.ancientpoet.shared.util

import kotlin.test.*

class MapProjectionTest {
    @Test fun allMarkersFitAndNorthIsUpForNarrowAndWideScreens() {
        val places = listOf(GeographicPoint(34.26, 108.94), GeographicPoint(30.25, 120.16), GeographicPoint(39.90, 116.40))
        val projection = MapProjection(places)
        for ((width, height) in listOf(280.0 to 260.0, 900.0 to 400.0)) {
            val points = places.map { projection.project(it.lat, it.lng, width, height) }
            points.forEach {
                assertTrue(it.x in 24.0..(width - 24))
                assertTrue(it.y in 24.0..(height - 24))
            }
            assertTrue(points[2].y < points[0].y)
            assertEquals(points[0], projection.project(places[0].lat, places[0].lng, width, height))
        }
    }

    @Test fun singleCityAndEmptyDatasetRemainFinite() {
        for (points in listOf(emptyList(), listOf(GeographicPoint(34.0, 108.0)))) {
            val projected = MapProjection(points).project(34.0, 108.0, 320.0, 260.0)
            assertTrue(projected.x.isFinite() && projected.y.isFinite())
        }
    }
}
