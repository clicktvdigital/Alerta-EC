package org.breezyweather.ui.map

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InamhiLayerStatusTest {
    private val now = Instant.parse("2026-10-18T12:00:00Z")

    @Test
    fun availableTimeDoesNotClaimVisibleTilesOrObservation() {
        val text = InamhiLayerStatus.describe("Lluvia", true,
            InamhiLayerStatus.State(InamhiLayerStatus.Phase.REQUESTED, now), now)
        assertTrue(text.contains("18/10/2026 07:00"))
        assertTrue(text.contains("UTC−5"))
        assertTrue(text.contains("visualización no confirmada"))
        assertFalse(text.contains("mm"))
    }

    @Test
    fun expiredTimeIsNotPresentedAsUpcoming() {
        val text = InamhiLayerStatus.describe("Lluvia", true,
            InamhiLayerStatus.State(InamhiLayerStatus.Phase.REQUESTED, now.minusSeconds(1)), now)
        assertTrue(text.contains("vencida"))
        assertFalse(text.contains("pronóstico para"))
    }

    @Test
    fun disabledPreferenceOverridesOldDatesAndErrors() {
        InamhiLayerStatus.Phase.entries.forEach { phase ->
            assertEquals("Lluvia: desactivada por el usuario", InamhiLayerStatus.describe(
                "Lluvia", false, InamhiLayerStatus.State(phase, now.minusSeconds(1)), now
            ))
        }
    }

    @Test
    fun missingDateNeverInventsTimestamp() {
        val text = InamhiLayerStatus.describe("Lluvia", true,
            InamhiLayerStatus.State(InamhiLayerStatus.Phase.REQUESTED), now)
        assertFalse(text.contains("18/10/2026"))
        assertFalse(text.contains("pronóstico para"))
    }
    @Test
    fun refreshAndErrorRetainOnlyUnexpiredForecasts() {
        val initial = InamhiLayerStatus.State(InamhiLayerStatus.Phase.REQUESTED, now.plusSeconds(30))
        val loading = InamhiLayerStatus.retaining(initial, InamhiLayerStatus.Phase.LOADING, now)
        val failed = InamhiLayerStatus.retaining(loading, InamhiLayerStatus.Phase.ERROR, now)
        assertEquals(initial.forecastTime, failed.forecastTime)
        assertTrue(InamhiLayerStatus.describe("Lluvia", true, loading, now).contains("se conserva"))
        assertTrue(InamhiLayerStatus.describe("Lluvia", true, failed, now).contains("error al actualizar"))
        assertNull(InamhiLayerStatus.retaining(failed, InamhiLayerStatus.Phase.ERROR,
            now.plusSeconds(31)).forecastTime)
    }
}
