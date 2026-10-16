package org.breezyweather.ui.main.adapters.main

import breezyweather.domain.weather.model.Hourly
import breezyweather.domain.weather.reference.WeatherCode
import java.util.Calendar
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.TimeZone

class HourlyForecastHighlightsTest {

    @Test
    fun selectUsesLocalHourBucketsForRequestedOffsets() {
        val timeZone = TimeZone.getTimeZone("America/Guayaquil")
        val now = 1_760_000_000_000L
        val currentHour = (now / 3_600_000L) * 3_600_000L
        val forecasts = listOf(1, 2, 3, 6).map { offset ->
            Hourly(date = Date(currentHour + offset * 3_600_000L + 15 * 60_000L))
        }

        HourlyForecastHighlights.select(forecasts, now, timeZone).map { it.offsetHours } shouldBe listOf(1, 2, 3, 6)
    }

    @Test
    fun selectSkipsMissingIntervalsAndEmptyData() {
        val timeZone = TimeZone.getTimeZone("America/Guayaquil")
        val now = 1_760_000_000_000L
        val currentHour = (now / 3_600_000L) * 3_600_000L
        val forecasts = listOf(2, 6).map { offset ->
            Hourly(date = Date(currentHour + offset * 3_600_000L))
        }

        HourlyForecastHighlights.select(forecasts, now, timeZone).map { it.offsetHours } shouldBe listOf(2, 6)
        HourlyForecastHighlights.select(emptyList(), now, timeZone) shouldBe emptyList()
    }

    @Test
    fun cachedValuesRemainVisibleWithAStaleStateAndMissingIntervalsAreExplicit() {
        val now = 1_800_000_000_000L
        val hourStart = now / 3_600_000L * 3_600_000L
        val cached = Hourly(
            date = Date(hourStart + 3_600_000L),
            weatherCode = WeatherCode.RAIN,
            acquiredAt = Date(now - 3_600_001L)
        )
        val slots = HourlyForecastHighlights.describe(listOf(cached), now, TimeZone.getTimeZone("America/Guayaquil"))
        slots.map { it.offsetHours } shouldBe listOf(1, 2, 3, 6)
        slots.first().forecast shouldBe cached
        slots.map { it.state } shouldBe listOf(
            HourlyHighlightState.STALE,
            HourlyHighlightState.MISSING,
            HourlyHighlightState.MISSING,
            HourlyHighlightState.MISSING
        )
    }

    @Test
    fun acquisitionBoundariesAndUnreliableDatesHaveDistinctStates() {
        val now = 1_800_000_000_000L
        val forecast = Hourly(Date(now / 3_600_000L * 3_600_000L + 3_600_000L), weatherCode = WeatherCode.RAIN)
        val cases = listOf(
            now to HourlyHighlightState.RECENT,
            now - 3_600_000L to HourlyHighlightState.RECENT,
            now - 3_600_001L to HourlyHighlightState.STALE,
            null to HourlyHighlightState.UNKNOWN,
            0L to HourlyHighlightState.UNKNOWN,
            now + 1 to HourlyHighlightState.UNKNOWN
        )
        for ((acquired, expected) in cases) {
            val hour = forecast.copy(acquiredAt = acquired?.let { Date(it) })
            val slot = HourlyForecastHighlights.describe(listOf(hour), now, TimeZone.getTimeZone("UTC")).first()
            slot.state shouldBe expected
            slot.forecast shouldBe hour
        }
    }

    @Test
    fun emptySeriesAndEmptyRecordsProduceFourMissingIntervals() {
        val now = 1_800_000_000_000L
        val zone = TimeZone.getTimeZone("America/Guayaquil")
        val emptyHour = Hourly(Date(now / 3_600_000L * 3_600_000L + 3_600_000L), acquiredAt = Date(now))
        for (hours in listOf(emptyList(), listOf(emptyHour))) {
            HourlyForecastHighlights.describe(hours, now, zone).map { it.state } shouldBe
                List(4) { HourlyHighlightState.MISSING }
        }
    }

    @Test
    fun localHourBucketsAndOffsetsArePreservedAcrossMidnightAndFractionalZones() {
        for (zoneId in listOf("America/Guayaquil", "Pacific/Galapagos", "Asia/Kathmandu")) {
            val zone = TimeZone.getTimeZone(zoneId)
            val clock = Calendar.getInstance(zone).apply {
                clear()
                set(2026, Calendar.OCTOBER, 15, 23, 50, 0)
            }
            val now = clock.timeInMillis
            val expectedDates = listOf(1, 2, 3, 6).map { offset ->
                (clock.clone() as Calendar).apply {
                    set(Calendar.MINUTE, 0)
                    add(Calendar.HOUR_OF_DAY, offset)
                }.time
            }
            val hours = expectedDates.map {
                Hourly(it, weatherCode = WeatherCode.RAIN, acquiredAt = Date(now))
            }
            val slots = HourlyForecastHighlights.describe(hours.reversed(), now, zone)
            slots.map { it.forecast?.date } shouldBe expectedDates
            slots.map { it.state } shouldBe List(4) { HourlyHighlightState.RECENT }
            // P05 is deliberately unchanged: +1 h is the next clock hour, 10 minutes away here.
            slots.first().forecast!!.date.time - now shouldBe 10 * 60_000L
        }
    }
}
