package org.breezyweather.ui.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class InamhiWmsTimesTest {
    private val now = Instant.parse("2026-10-18T12:00:00Z")

    @Test
    fun selectsNearestUpcomingForecast() {
        val times = listOf(now.plusSeconds(7200), now.plusSeconds(1800), now.plusSeconds(3600))
        assertEquals(now.plusSeconds(1800), InamhiWmsTimes.selectUpcoming(times, now))
    }

    @Test
    fun rejectsExpiredForecasts() {
        assertNull(InamhiWmsTimes.selectUpcoming(listOf(now.minusSeconds(60)), now))
    }

    @Test
    fun rejectsForecastsBeyondSixHours() {
        assertNull(InamhiWmsTimes.selectUpcoming(listOf(now.plusSeconds(21601)), now))
    }

    @Test
    fun selectsForecastAtCurrentTime() {
        val times = listOf(now.minusSeconds(3600), now, now.plusSeconds(3600))
        assertEquals(now, InamhiWmsTimes.selectUpcoming(times, now))
    }

    @Test
    fun acceptsForecastAtSixHourBoundary() {
        assertEquals(
            now.plusSeconds(21600),
            InamhiWmsTimes.selectUpcoming(listOf(now.plusSeconds(21600)), now)
        )
    }

    @Test
    fun supportsDailyForecastWindow() {
        val tomorrow = now.plusSeconds(23 * 3600L)
        assertEquals(
            tomorrow,
            InamhiWmsTimes.selectUpcoming(listOf(tomorrow), now, horizonHours = 24)
        )
    }

    @Test
    fun rejectsForecastBeyondConfiguredWindow() {
        val tooLate = now.plusSeconds(25 * 3600L)
        assertNull(
            InamhiWmsTimes.selectUpcoming(listOf(tooLate), now, horizonHours = 24)
        )
    }

    @Test
    fun rejectsInvalidHorizon() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            InamhiWmsTimes.selectUpcoming(emptyList(), now, horizonHours = 0)
        }
    }

    @Test
    fun usesCorrectHorizonForEachLayer() {
        assertEquals(
            6L,
            InamhiWmsTimes.horizonHoursFor(InamhiLayers.PRECIPITATION_3H)
        )
        assertEquals(
            24L,
            InamhiWmsTimes.horizonHoursFor(InamhiLayers.PRECIPITATION_24H)
        )
        assertEquals(
            6L,
            InamhiWmsTimes.horizonHoursFor(InamhiLayers.TEMPERATURE_3H)
        )
    }

    @Test
    fun handlesEmptyAvailability() {
        assertNull(InamhiWmsTimes.selectUpcoming(emptyList(), now))
    }
    @Test
    fun expandsOnlyAdvertisedFixedIntervalSteps() {
        assertEquals(listOf(now, now.plusSeconds(10800), now.plusSeconds(21600)),
            InamhiWmsTimes.parseDimension("$now/${now.plusSeconds(25000)}/PT3H"))
    }

    @Test
    fun sortsAndDeduplicatesExplicitDatesWithOffsets() {
        assertEquals(listOf(now, now.plusSeconds(3600)), InamhiWmsTimes.parseDimension(
            "2026-10-18T08:00:00-05:00, $now, 2026-10-18T07:00:00-05:00"
        ))
    }

    @Test
    fun rejectsInvalidOrUnboundedIntervalsInsteadOfInventingTimes() {
        listOf(
            "$now/${now.plusSeconds(3600)}/PT0S",
            "$now/${now.plusSeconds(3600)}/PT-1S",
            "$now/${now.minusSeconds(1)}/PT1H",
            "$now/${now.plusSeconds(3600)}/P1M",
            "$now/${now.plusSeconds(10000)}/PT1S",
            "current", "not-a-date", "$now/invalid"
        ).forEach { value ->
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException::class.java) {
                InamhiWmsTimes.parseDimension(value)
            }
        }
    }

    @Test
    fun revalidatesDateAfterSlowNetworkResponse() {
        assertNull(InamhiWmsTimes.selectUpcoming(listOf(now), now.plusSeconds(1)))
    }

    @Test
    fun emptyDimensionHasNoInventedCurrentTime() {
        assertEquals(emptyList<Instant>(), InamhiWmsTimes.parseDimension("  "))
    }
}
