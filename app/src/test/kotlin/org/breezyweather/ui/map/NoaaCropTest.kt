package org.breezyweather.ui.map

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NoaaCropTest {
    @Test
    fun ecuadorCropIsALandscapeWindowOnTheWestOfTheSector() {
        val crop = NoaaSatelliteFrames.ECUADOR
        // Ecuador sits on the western edge of the GOES-19 "nsa" sector.
        assertTrue(crop.left < 0.1f && crop.right < 0.3f)
        // On a 3600x2160 frame the window keeps a landscape shape for TV (wider than tall).
        val width = (crop.right - crop.left) * 3600
        val height = (crop.bottom - crop.top) * 2160
        assertTrue(width > height)
        // Small enough to stay light on 1 GB devices after region decoding.
        assertTrue(width * height < 600_000f)
    }

    @Test
    fun invalidCropIsRejected() {
        assertThrows<IllegalArgumentException> { NoaaCrop(0.5f, 0.1f, 0.4f, 0.2f) }
        assertThrows<IllegalArgumentException> { NoaaCrop(0.1f, 0.1f, 1.2f, 0.2f) }
    }

    @Test
    fun croppedAndWholeFramesOfTheSameImageHaveDifferentKeys() {
        val at = Instant.parse("2026-11-19T23:00:00Z")
        val url = "https://cdn.star.nesdis.noaa.gov/GOES19/ABI/SECTOR/nsa/GEOCOLOR/x.jpg"
        val whole = NoaaSatelliteFrame(at, url)
        val ecuador = NoaaSatelliteFrame(at, url, NoaaSatelliteFrames.ECUADOR)
        assertEquals(url, whole.key)
        assertNotEquals(whole.key, ecuador.key)
    }
}
