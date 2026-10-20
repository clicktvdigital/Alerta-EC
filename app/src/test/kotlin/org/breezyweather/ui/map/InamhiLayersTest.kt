package org.breezyweather.ui.map

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InamhiLayersTest {
    @Test
    fun approximateContinentalDomainDoesNotImplyNationalCoverage() {
        assertTrue(InamhiLayers.covers(-0.18, -78.47))
        assertTrue(InamhiLayers.covers(-2.17, -79.90))
        assertTrue(InamhiLayers.covers(-0.46, -76.99))
        assertFalse(InamhiLayers.covers(-0.74, -90.31))
        assertFalse(InamhiLayers.covers(Double.NaN, -78.47))
        assertFalse(InamhiLayers.covers(-0.18, Double.POSITIVE_INFINITY))
    }

    @Test
    fun tilesRequestTransparencyAndExactForecastTime() {
        val url = InamhiWmsTiles.url(InamhiLayers.PRECIPITATION_3H,
            Instant.parse("2026-10-18T12:00:00Z"))
        assertTrue(url.startsWith(InamhiLayers.WMS_URL))
        assertTrue(url.contains("TRANSPARENT=TRUE"))
        assertTrue(url.contains("FORMAT=image/png"))
        assertTrue(url.contains("BBOX={bbox-epsg-3857}"))
        assertTrue(url.contains("TIME=2026-10-18T12%3A00%3A00Z"))
        assertThrows(IllegalArgumentException::class.java) {
            InamhiWmsTiles.url("unknown", Instant.EPOCH)
        }
    }
}
