package org.breezyweather.alertaec

import org.breezyweather.R
import breezyweather.domain.weather.reference.AlertSeverity

internal object AlertaEcNotificationAppearance {
    const val PRECIPITATION = "weather_precipitation"
    const val SEVERE = "weather_severe"
    const val EXTREME = "weather_extreme"

    // Only official provider severity authorizes an intrusive channel.
    fun officialChannel(severity: AlertSeverity): String = when (severity) {
        AlertSeverity.EXTREME -> EXTREME
        AlertSeverity.SEVERE -> SEVERE
        else -> "forecast"
    }

    fun icon(phenomenon: AlertaEcPhenomenon): Int = when (phenomenon) {
        AlertaEcPhenomenon.RAIN, AlertaEcPhenomenon.HEAVY_RAIN -> R.drawable.ic_notification_rain
        AlertaEcPhenomenon.THUNDERSTORM -> R.drawable.ic_notification_thunderstorm
        AlertaEcPhenomenon.STRONG_WIND -> R.drawable.ic_notification_wind
        else -> R.drawable.ic_notification_weather
    }
}
