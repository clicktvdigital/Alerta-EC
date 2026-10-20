package org.breezyweather.ui.map

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object InamhiLayerStatus {
    enum class Phase { WAITING, LOADING, REQUESTED, NO_DATA, EXPIRED, ERROR }

    data class State(val phase: Phase = Phase.WAITING, val forecastTime: Instant? = null)

    fun retaining(state: State?, phase: Phase, now: Instant): State =
        State(phase, state?.forecastTime?.takeUnless { it.isBefore(now) })

    private val formatter = DateTimeFormatter.ofPattern(
        "dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-EC")
    ).withZone(ZoneId.of("America/Guayaquil"))

    fun describe(title: String, enabled: Boolean, state: State, now: Instant): String {
        if (!enabled) return "$title: desactivada por el usuario"
        val time = state.forecastTime
        val detail = when {
            state.phase == Phase.EXPIRED || time != null && time.isBefore(now) ->
                "fecha de pronóstico vencida; capa retirada"
            time != null && state.phase in setOf(Phase.LOADING, Phase.ERROR) ->
                (if (state.phase == Phase.LOADING) "consultando fechas WMS" else "error al actualizar") +
                    "; se conserva pronóstico para ${formatter.format(time)} (UTC−5); " +
                    "teselas solicitadas, visualización no confirmada"
            state.phase == Phase.REQUESTED && time != null ->
                "pronóstico para ${formatter.format(time)} (UTC−5, hora continental); " +
                    "teselas solicitadas, visualización no confirmada"
            state.phase == Phase.LOADING -> "consultando fechas WMS"
            state.phase == Phase.NO_DATA -> "sin fecha de pronóstico dentro del horizonte configurado"
            state.phase == Phase.ERROR -> "error de consulta o de aplicación de la capa; sin datos confirmados"
            else -> "pendiente de consulta"
        }
        return "$title: $detail"
    }
}
