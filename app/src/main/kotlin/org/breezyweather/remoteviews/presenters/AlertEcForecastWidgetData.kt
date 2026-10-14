/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 */

package org.breezyweather.remoteviews.presenters

import breezyweather.domain.weather.model.Minutely
import org.breezyweather.unit.temperature.TemperatureUnit
import java.util.Date

/** A future minutely forecast summarized as an accumulation over its real intervals. */
internal data class ForecastRainWindow(
    val start: Date,
    val end: Date,
    val accumulationMm: Double,
)

internal fun nextForecastRainWindow(
    forecast: List<Minutely>,
    now: Date,
): ForecastRainWindow? {
    val future = forecast
        .asSequence()
        .filter { it.date >= now && it.minuteInterval > 0 }
        .sortedBy { it.date }
        .toList()
    val firstRain = future.indexOfFirst { (it.precipitationIntensity?.inMillimeters ?: 0.0) > 0.0 }
    if (firstRain < 0) return null

    val rain = mutableListOf<Minutely>()
    for (interval in future.drop(firstRain)) {
        val previous = rain.lastOrNull()
        if ((interval.precipitationIntensity?.inMillimeters ?: 0.0) <= 0.0 ||
            (previous != null && previous.endingDate.time != interval.date.time)
        ) {
            break
        }
        rain.add(interval)
    }
    if (rain.isEmpty()) return null

    val totalMm = rain.sumOf {
        ((it.precipitationIntensity?.inMillimeters ?: 0.0) * it.minuteInterval) / 60.0
    }
    return ForecastRainWindow(rain.first().date, rain.last().endingDate, totalMm)
}

internal fun updateAgeMinutes(now: Date, updateTime: Date?): Int? {
    updateTime ?: return null
    val elapsedMilliseconds = now.time - updateTime.time
    if (elapsedMilliseconds < 0) return null
    return (elapsedMilliseconds / 60_000).toInt()
}

internal fun resolveWidgetTemperatureUnit(
    manualUnit: TemperatureUnit?,
    countryCode: String?,
    countryName: String,
    automaticUnit: TemperatureUnit,
): TemperatureUnit = manualUnit ?: if (
    countryCode.equals("EC", ignoreCase = true) || countryName.equals("Ecuador", ignoreCase = true)
) {
    TemperatureUnit.CELSIUS
} else {
    automaticUnit
}
