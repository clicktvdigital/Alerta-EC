package org.breezyweather.sources

import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.wrappers.WeatherWrapper
import java.util.Date

/** Call only for a received FORECAST response, before merging any cached records. */
internal fun WeatherWrapper.withHourlyAcquisition(receivedAtMillis: Long): WeatherWrapper {
    if (failedFeatures?.containsKey(SourceFeature.FORECAST) == true) return this
    return copy(
        hourlyForecast = hourlyForecast?.map { hour ->
            hour.copy(acquiredAt = receivedAtMillis.takeIf { it > 0 }?.let { Date(it) })
        }
    )
}
