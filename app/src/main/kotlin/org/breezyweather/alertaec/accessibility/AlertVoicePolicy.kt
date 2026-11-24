package org.breezyweather.alertaec.accessibility

import org.breezyweather.alertaec.AlertaEcEvent
import org.breezyweather.alertaec.AlertaEcPhenomenon
import org.breezyweather.alertaec.AlertaEcSeverity
import java.time.Instant

/** No backlog: an alert suppressed by Android or initial setup is never replayed later. */
internal object AlertVoicePolicy {
    data class AutomaticReadState(
        val isNewEvent: Boolean,
        val notificationAccepted: Boolean,
        val hasBacklog: Boolean,
        val activityResumed: Boolean,
        val activityHasFocus: Boolean,
        val pushEnabled: Boolean,
        val soundEnabled: Boolean,
        val voiceEnabled: Boolean,
        val dndAllowsAlerts: Boolean,
        val notificationVolume: Int,
        val deviceUnlocked: Boolean,
        val audioModeNormal: Boolean,
        val musicInactive: Boolean,
        val audioFocusAvailable: Boolean,
        val readingInProgress: Boolean,
    )

    fun mayReadAutomatically(event: AlertaEcEvent, nowMillis: Long, state: AutomaticReadState): Boolean {
        val start = event.startTimeMillis ?: return false
        val end = event.endTimeMillis ?: return false
        if (nowMillis <= 0L || start <= 0L || end <= start || start > nowMillis || end <= nowMillis) return false
        return event.severity >= AlertaEcSeverity.WATCH &&
            state.isNewEvent && state.notificationAccepted && !state.hasBacklog &&
            state.activityResumed && state.activityHasFocus && state.pushEnabled &&
            state.soundEnabled && state.voiceEnabled && state.dndAllowsAlerts &&
            state.notificationVolume > 0 && state.deviceUnlocked && state.audioModeNormal &&
            state.musicInactive && state.audioFocusAvailable && !state.readingInProgress
    }
    /**
     * Pure eligibility gate for an already accepted event; it does not inspect or confirm
     * Android notification state. The end is never inferred when the producer omitted it.
     */
    fun isEligibleForAutomaticVoice(event: AcceptedAlertEvent, now: Instant): Boolean {
        if (event.severity < AlertaEcSeverity.WATCH) return false
        if (!event.start.isAfter(Instant.EPOCH)) return false
        if (!event.end.isAfter(event.start)) return false
        return !event.start.isAfter(now) && event.end.isAfter(now)
    }

    fun mayReadAutomatic(hasPreviousWeather: Boolean, active: Boolean, end: Long?, now: Long): Boolean =
        hasPreviousWeather && active && end != null && end > now

    fun speechRate(value: Float): Float = if (value.isFinite()) value.coerceIn(0.5f, 1.5f) else 1f

    private val drivingRisk = setOf(
        AlertaEcPhenomenon.RAIN, AlertaEcPhenomenon.HEAVY_RAIN, AlertaEcPhenomenon.THUNDERSTORM,
        AlertaEcPhenomenon.HAIL, AlertaEcPhenomenon.STRONG_WIND, AlertaEcPhenomenon.FOG,
        AlertaEcPhenomenon.FLOOD, AlertaEcPhenomenon.LANDSLIDE, AlertaEcPhenomenon.SNOW,
        AlertaEcPhenomenon.VOLCANIC_ACTIVITY, AlertaEcPhenomenon.TROPICAL_CYCLONE,
    )

    /**
     * Concise spoken message: only the whole-number temperature and a short recommendation
     * (what to wear, whether to drive carefully, one brief tip). No locations, sources or figures.
     * [degrees] is already converted to the user's unit; [celsius] drives the clothing advice.
     */
    fun conciseSpeech(phenomenon: AlertaEcPhenomenon, degrees: Int?, celsius: Double?): String {
        val parts = mutableListOf<String>()
        degrees?.let { parts += if (it == 1 || it == -1) "$it grado." else "$it grados." }
        val temp = celsius?.takeIf { it.isFinite() }
        when {
            temp == null -> Unit
            temp <= 12.0 -> parts += "Abrígate bien, con chompa y ropa por capas."
            temp <= 17.0 -> parts += "Lleva chompa."
            temp >= 28.0 -> parts += "Usa ropa ligera y toma agua."
        }
        when (phenomenon) {
            AlertaEcPhenomenon.RAIN, AlertaEcPhenomenon.HEAVY_RAIN -> "Lleva paraguas o impermeable."
            AlertaEcPhenomenon.THUNDERSTORM -> "Usa impermeable y evita espacios abiertos."
            AlertaEcPhenomenon.HAIL -> "Protégete bajo techo."
            AlertaEcPhenomenon.STRONG_WIND -> "Evita el paraguas y aléjate de árboles."
            AlertaEcPhenomenon.FOG -> "Usa luces bajas."
            AlertaEcPhenomenon.FLOOD -> "No cruces calles ni quebradas inundadas."
            AlertaEcPhenomenon.LANDSLIDE -> "Aléjate de taludes y laderas."
            AlertaEcPhenomenon.VOLCANIC_ACTIVITY -> "Usa mascarilla si cae ceniza."
            AlertaEcPhenomenon.EXTREME_HEAT -> "Busca sombra."
            AlertaEcPhenomenon.EARTHQUAKE -> "Agáchate, cúbrete y sujétate."
            AlertaEcPhenomenon.TSUNAMI -> "Sube a zonas altas."
            else -> null
        }?.let(parts::add)
        if (phenomenon in drivingRisk) parts += "Si conduces, hazlo con precaución."
        return parts.joinToString(" ")
    }

    fun summary(phenomenon: String): String =
        "Alerta meteorológica: $phenomenon. Consulta los detalles y las indicaciones de la fuente en la aplicación."
}
