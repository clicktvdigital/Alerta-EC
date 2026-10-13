package org.breezyweather.alertaec

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
    val updatedAtMillis: Long? = null,
    val precipitationMmPerHour: Double? = null,
    val precipitationAccumulationMm: Double? = null,
    val precipitationProbabilityPercent: Int? = null,
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
    fun shouldNotify(state: AlertaEcEventState): Boolean =
        state == AlertaEcEventState.FORECAST

    fun filterNewEvents(events: List<AlertaEcEvent>, notifiedIds: Set<String>): List<AlertaEcEvent> =
        events.distinctBy { it.id }.filterNot { it.id in notifiedIds }

    fun stateLabel(state: AlertaEcEventState): String = when (state) {
        AlertaEcEventState.FORECAST -> "PRONÓSTICO"
        AlertaEcEventState.OBSERVED -> "OBSERVADO"
        AlertaEcEventState.OFFICIAL_EVENT -> "ALERTA OFICIAL"
    }
}
