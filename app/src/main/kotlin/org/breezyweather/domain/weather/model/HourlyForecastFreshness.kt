package org.breezyweather.domain.weather.model

internal enum class HourlyDataFreshness {
    RECENT,
    STALE,
    UNKNOWN,
}

internal object HourlyForecastFreshness {
    // Conservative product policy for ordinary hourly notifications, not official alerts.
    const val MAX_ACQUISITION_AGE_MILLIS = 60 * 60 * 1000L

    fun classify(acquiredAtMillis: Long?, nowMillis: Long): HourlyDataFreshness {
        if (nowMillis <= 0 || acquiredAtMillis == null || acquiredAtMillis <= 0 || acquiredAtMillis > nowMillis) {
            return HourlyDataFreshness.UNKNOWN
        }
        return if (nowMillis - acquiredAtMillis <= MAX_ACQUISITION_AGE_MILLIS) {
            HourlyDataFreshness.RECENT
        } else {
            HourlyDataFreshness.STALE
        }
    }
}
