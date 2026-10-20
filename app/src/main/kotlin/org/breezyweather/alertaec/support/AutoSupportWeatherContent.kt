package org.breezyweather.alertaec.support

import breezyweather.domain.weather.model.Current
import breezyweather.domain.weather.model.Daily
import breezyweather.domain.weather.model.Hourly
import breezyweather.domain.weather.model.Weather
import java.util.concurrent.TimeUnit

/**
 * Comprueba presencia de contenido meteorologico sin exigir que todos
 * los proveedores suministren las mismas variables.
 * No sustituye la comprobacion independiente de vigencia.
 */
internal object AutoSupportWeatherContent {

    fun hasCurrent(current: Current?): Boolean =
        current != null && (
            !current.weatherText.isNullOrBlank() ||
                current.weatherCode != null ||
                current.temperature?.temperature != null ||
                current.wind != null ||
                current.relativeHumidity != null ||
                current.pressure != null ||
                current.cloudCover != null
            )

    private fun hasHalfDayContent(
        halfDay: breezyweather.domain.weather.model.HalfDay?,
    ): Boolean =
        halfDay != null && (
            !halfDay.weatherText.isNullOrBlank() ||
                !halfDay.weatherSummary.isNullOrBlank() ||
                halfDay.weatherCode != null ||
                halfDay.temperature?.temperature != null ||
                halfDay.precipitation != null ||
                halfDay.precipitationProbability != null ||
                halfDay.precipitationDuration != null ||
                halfDay.wind != null
            )

    fun hasDaily(daily: List<Daily>, nowMillis: Long): Boolean =
        daily.any { day ->
            day.date.time >= nowMillis - TimeUnit.DAYS.toMillis(1) &&
                (
                    hasHalfDayContent(day.day) ||
                        hasHalfDayContent(day.night) ||
                        day.uV != null ||
                        day.relativeHumidity != null ||
                        day.pressure != null ||
                        day.sunshineDuration != null
                    )
        }

    fun hasUsefulHourlyRecord(hour: Hourly, nowMillis: Long): Boolean =
        hour.date.time >= nowMillis - TimeUnit.HOURS.toMillis(1) &&
            hour.date.time < nowMillis + TimeUnit.HOURS.toMillis(24) &&
            (
                !hour.weatherText.isNullOrBlank() ||
                    hour.weatherCode != null ||
                    hour.temperature?.temperature != null ||
                    hour.precipitation != null ||
                    hour.precipitationProbability != null ||
                    hour.wind != null ||
                    hour.relativeHumidity != null
                )

    fun hasHourly(hourly: List<Hourly>, nowMillis: Long): Boolean =
        hourly.any { hasUsefulHourlyRecord(it, nowMillis) }

    fun summary(weather: Weather, nowMillis: Long): Triple<Boolean, Boolean, Boolean> =
        Triple(
            hasCurrent(weather.current),
            hasDaily(weather.dailyForecast, nowMillis),
            hasHourly(weather.hourlyForecast, nowMillis),
        )
}
