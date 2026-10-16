package org.breezyweather.alertaec

import org.breezyweather.domain.weather.model.HourlyDataFreshness
import org.breezyweather.domain.weather.model.HourlyForecastFreshness

data class AlertaEcEvent(
    val id: String,
    val phenomenon: AlertaEcPhenomenon,
    val state: AlertaEcEventState,
    val severity: AlertaEcSeverity,
    val headline: String,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sourceName: String? = null,
    val description: String? = null,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    // For FORECAST, the acquisition time of the individual hourly record.
    val updatedAtMillis: Long? = null,
    val precipitationMmPerHour: Double? = null,
    val precipitationAccumulationMm: Double? = null,
    val precipitationProbabilityPercent: Int? = null,
    val temperatureCelsius: Double? = null,
    val feelsLikeCelsius: Double? = null,
    val windSpeedKmh: Double? = null,
    val windGustKmh: Double? = null,
    val windDirectionDegrees: Int? = null,
    val humidityPercent: Int? = null,
    val pressureHpa: Double? = null,
    val visibilityKm: Double? = null,
    val hailProbabilityPercent: Int? = null,
    val lightningProbabilityPercent: Int? = null,
    val lightningDistanceKm: Double? = null,
    val confidencePercent: Int? = null,
    val magnitude: Double? = null,
    val depthKm: Double? = null,
    val epicenter: String? = null,
    val distanceKm: Double? = null,
    val details: Map<String, String> = emptyMap(),
)

internal object AlertaEcNotificationPolicy {
    const val MAX_FUTURE_HORIZON_MILLIS = 6 * 60 * 60 * 1000L
    private const val MAX_START_AGE_WITHOUT_END_MILLIS = 60 * 60 * 1000L
    private const val MAX_INTERVAL_DURATION_MILLIS = 6 * 60 * 60 * 1000L

    fun shouldNotify(event: AlertaEcEvent, nowMillis: Long): Boolean {
        // Official alerts remain owned by remoteviews.Notifications, without this age limit.
        if (event.state != AlertaEcEventState.FORECAST ||
            HourlyForecastFreshness.classify(event.updatedAtMillis, nowMillis) != HourlyDataFreshness.RECENT
        ) return false
        val start = event.startTimeMillis ?: return false
        if (start <= 0 || (start > nowMillis && start - nowMillis > MAX_FUTURE_HORIZON_MILLIS)) return false
        val end = event.endTimeMillis
        return if (end != null) {
            end > start && end > nowMillis && end - start <= MAX_INTERVAL_DURATION_MILLIS
        } else {
            // Hourly supplies no duration. This is an eligibility limit, not an invented end time.
            start > nowMillis - MAX_START_AGE_WITHOUT_END_MILLIS
        }
    }

    fun filterNewEvents(events: List<AlertaEcEvent>, notifiedIds: Set<String>): List<AlertaEcEvent> =
        events.distinctBy { it.id }.filterNot { it.id in notifiedIds }

    fun stateLabel(state: AlertaEcEventState): String = when (state) {
        AlertaEcEventState.FORECAST -> "PRONÓSTICO"
        AlertaEcEventState.OBSERVED -> "OBSERVADO"
        AlertaEcEventState.OFFICIAL_EVENT -> "ALERTA OFICIAL"
    }
}
