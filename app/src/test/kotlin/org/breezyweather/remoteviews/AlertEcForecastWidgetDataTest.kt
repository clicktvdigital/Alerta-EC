package org.breezyweather.remoteviews

import breezyweather.domain.weather.model.Minutely
import org.breezyweather.remoteviews.presenters.updateAgeMinutes
import org.breezyweather.remoteviews.presenters.nextForecastRainWindow
import org.breezyweather.remoteviews.presenters.resolveWidgetTemperatureUnit
import org.breezyweather.unit.precipitation.Precipitation.Companion.millimeters
import org.breezyweather.unit.temperature.TemperatureUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Date

class AlertEcForecastWidgetDataTest {
    @Test
    fun `rain accumulation integrates rate over each forecast interval`() {
        val now = Date(1_000_000)
        val forecast = listOf(
            Minutely(Date(now.time - 60_000), 15, 8.0.millimeters),
            Minutely(now, 15, 4.0.millimeters),
            Minutely(Date(now.time + 15 * 60_000), 30, 2.0.millimeters),
            Minutely(Date(now.time + 45 * 60_000), 15, 0.0.millimeters)
        )

        val window = nextForecastRainWindow(forecast, now)

        assertEquals(now, window?.start)
        assertEquals(Date(now.time + 45 * 60_000), window?.end)
        assertEquals(2.0, window?.accumulationMm)
    }

    @Test
    fun `no future rain returns no forecast window`() {
        val now = Date(1_000_000)
        assertNull(nextForecastRainWindow(listOf(Minutely(now, 15, 0.0.millimeters)), now))
        assertNull(nextForecastRainWindow(emptyList(), now))
    }

    @Test
    fun `Ecuador automatic unit is Celsius while manual unit wins`() {
        assertEquals(
            TemperatureUnit.CELSIUS,
            resolveWidgetTemperatureUnit(null, "EC", "Ecuador", TemperatureUnit.FAHRENHEIT)
        )
        assertEquals(
            TemperatureUnit.FAHRENHEIT,
            resolveWidgetTemperatureUnit(TemperatureUnit.FAHRENHEIT, "EC", "Ecuador", TemperatureUnit.CELSIUS)
        )
    }

    @Test
    fun `update age rejects future timestamps and returns elapsed minutes`() {
        val now = Date(3_600_000)
        assertNull(updateAgeMinutes(now, Date(now.time + 1)))
        assertNull(updateAgeMinutes(now, Date(now.time + 59_999)))
        assertEquals(0, updateAgeMinutes(now, Date(now.time - 59_999)))
        assertEquals(5, updateAgeMinutes(now, Date(now.time - 5 * 60_000)))
        assertEquals(120, updateAgeMinutes(now, Date(now.time - 2 * 60 * 60_000)))
    }
}
