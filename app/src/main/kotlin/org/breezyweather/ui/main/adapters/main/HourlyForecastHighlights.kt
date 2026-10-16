package org.breezyweather.ui.main.adapters.main

import breezyweather.domain.weather.model.Hourly
import org.breezyweather.domain.weather.model.HourlyDataFreshness
import org.breezyweather.domain.weather.model.HourlyForecastFreshness
import java.util.Calendar
import java.util.TimeZone

internal data class HourlyForecastHighlight(
    val offsetHours: Int,
    val forecast: Hourly,
)

internal enum class HourlyHighlightState {
    RECENT,
    STALE,
    UNKNOWN,
    MISSING,
}

internal data class HourlyForecastSlot(
    val offsetHours: Int,
    val forecast: Hourly?,
    val state: HourlyHighlightState,
)

internal object HourlyForecastHighlights {
    private val offsets = listOf(1, 2, 3, 6)

    /** Keeps cached values visible; missing intervals are explicit rather than omitted. */
    fun describe(
        forecasts: List<Hourly>,
        nowMillis: Long,
        timeZone: TimeZone,
    ): List<HourlyForecastSlot> {
        val selected = select(forecasts, nowMillis, timeZone).associateBy { it.offsetHours }
        return offsets.map { offset ->
            val hour = selected[offset]?.forecast
            val hasDetails = hour != null && (
                hour.temperature?.temperature != null ||
                    !hour.weatherText.isNullOrBlank() || hour.weatherCode != null ||
                    hour.precipitationProbability?.total != null || hour.wind?.speed != null
                )
            val state = if (!hasDetails) {
                HourlyHighlightState.MISSING
            } else {
                when (HourlyForecastFreshness.classify(hour?.acquiredAt?.time, nowMillis)) {
                    HourlyDataFreshness.RECENT -> HourlyHighlightState.RECENT
                    HourlyDataFreshness.STALE -> HourlyHighlightState.STALE
                    HourlyDataFreshness.UNKNOWN -> HourlyHighlightState.UNKNOWN
                }
            }
            HourlyForecastSlot(offset, hour, state)
        }
    }

    fun select(
        forecasts: List<Hourly>,
        nowMillis: Long,
        timeZone: TimeZone,
    ): List<HourlyForecastHighlight> {
        if (forecasts.isEmpty()) return emptyList()

        val currentHour = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val orderedForecasts = forecasts.sortedBy { it.date.time }

        return offsets.mapNotNull { offset ->
            val start = (currentHour.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, offset)
            }.timeInMillis
            val end = (currentHour.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, offset + 1)
            }.timeInMillis
            orderedForecasts.firstOrNull { it.date.time >= start && it.date.time < end }
                ?.let { HourlyForecastHighlight(offset, it) }
        }
    }
}
