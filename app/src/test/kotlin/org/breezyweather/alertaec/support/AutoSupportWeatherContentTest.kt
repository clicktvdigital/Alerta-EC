package org.breezyweather.alertaec.support

import breezyweather.domain.weather.model.Current
import breezyweather.domain.weather.model.Daily
import breezyweather.domain.weather.model.HalfDay
import breezyweather.domain.weather.model.Hourly
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.concurrent.TimeUnit

internal class AutoSupportWeatherContentTest {

    private val now = 1_800_000_000_000L

    @Test
    fun emptyCurrentIsNotUseful() {
        assertFalse(AutoSupportWeatherContent.hasCurrent(null))
        assertFalse(AutoSupportWeatherContent.hasCurrent(Current()))
        assertFalse(AutoSupportWeatherContent.hasCurrent(Current(weatherText = "  ")))
    }

    @Test
    fun currentWithOnlyDescriptionIsUseful() {
        assertTrue(AutoSupportWeatherContent.hasCurrent(Current(weatherText = "Soleado")))
    }

    @Test
    fun dailyWithEmptyHalfDayIsNotUseful() {
        assertFalse(
            AutoSupportWeatherContent.hasDaily(
                listOf(Daily(date = Date(now), day = HalfDay(), night = HalfDay())),
                now,
            )
        )
    }

    @Test
    fun dailyWithPartialForecastIsUseful() {
        assertTrue(
            AutoSupportWeatherContent.hasDaily(
                listOf(Daily(date = Date(now), day = HalfDay(weatherText = "Lluvia"))),
                now,
            )
        )
    }

    @Test
    fun hourlyWithoutMeasurementsIsNotUseful() {
        assertFalse(
            AutoSupportWeatherContent.hasHourly(
                listOf(Hourly(date = Date(now), acquiredAt = Date(now))),
                now,
            )
        )
    }

    @Test
    fun hourlyMoreThan24HoursAheadIsNotUseful() {
        val farFuture = now + TimeUnit.HOURS.toMillis(25)
        assertFalse(
            AutoSupportWeatherContent.hasHourly(
                listOf(Hourly(date = Date(farFuture), weatherText = "Lluvia")),
                now,
            )
        )
    }

    @Test
    fun emptyHourlyRecordWithRecentAcquisitionIsNotUseful() {
        val record = Hourly(date = Date(now), acquiredAt = Date(now))
        assertFalse(AutoSupportWeatherContent.hasUsefulHourlyRecord(record, now))
    }

    @Test
    fun usefulHourlyRecordWithRecentAcquisitionIsRecognized() {
        val record = Hourly(
            date = Date(now),
            weatherText = "Nublado",
            acquiredAt = Date(now),
        )
        assertTrue(AutoSupportWeatherContent.hasUsefulHourlyRecord(record, now))
    }

    @Test
    fun oldHourlyRecordWithRecentAcquisitionIsNotUseful() {
        val old = now - TimeUnit.HOURS.toMillis(3)
        val record = Hourly(
            date = Date(old),
            weatherText = "Soleado",
            acquiredAt = Date(now),
        )
        assertFalse(AutoSupportWeatherContent.hasUsefulHourlyRecord(record, now))
    }

    @Test
    fun hourlyWithDescriptionMustBeInRelevantPeriod() {
        val old = now - TimeUnit.HOURS.toMillis(3)
        assertFalse(
            AutoSupportWeatherContent.hasHourly(
                listOf(Hourly(date = Date(old), weatherText = "Nublado")),
                now,
            )
        )
        assertTrue(
            AutoSupportWeatherContent.hasHourly(
                listOf(Hourly(date = Date(now), weatherText = "Nublado")),
                now,
            )
        )
    }
}
