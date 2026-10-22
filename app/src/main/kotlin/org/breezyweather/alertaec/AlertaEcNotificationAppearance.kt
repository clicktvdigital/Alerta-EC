package org.breezyweather.alertaec

import org.breezyweather.R
import breezyweather.domain.weather.model.Minutely
import breezyweather.domain.weather.reference.AlertSeverity

internal object AlertaEcNotificationAppearance {
    const val PRECIPITATION = "weather_precipitation"
    const val PRECIPITATION_IMMINENT = "weather_precipitation_v2"
    const val SEVERE = "weather_severe"
    const val EXTREME = "weather_extreme"

    // Versioned because Android channel sound settings are immutable after creation.
    const val SOUND_RAIN = "alertaec_sound_rain_v1"
    const val SOUND_STORM = "alertaec_sound_storm_v1"
    const val SOUND_FLOOD_LANDSLIDE = "alertaec_sound_flood_landslide_v1"
    const val SOUND_VOLCANIC = "alertaec_sound_volcanic_v1"
    const val SOUND_CRITICAL = "alertaec_sound_critical_v1"
    const val SOUND_RAIN_NO_VIBRATION = "alertaec_sound_rain_no_vibration_v1"
    const val SOUND_STORM_NO_VIBRATION = "alertaec_sound_storm_no_vibration_v1"
    const val SOUND_FLOOD_LANDSLIDE_NO_VIBRATION = "alertaec_sound_flood_landslide_no_vibration_v1"
    const val SOUND_VOLCANIC_NO_VIBRATION = "alertaec_sound_volcanic_no_vibration_v1"
    const val SOUND_CRITICAL_NO_VIBRATION = "alertaec_sound_critical_no_vibration_v1"
    const val VIBRATION_FORECAST = "alertaec_vibration_forecast_v1"
    const val VIBRATION_SEVERE = "alertaec_vibration_severe_v1"
    const val VIBRATION_EXTREME = "alertaec_vibration_extreme_v1"
    // Stable visual-only channels for the both-preferences-disabled case.
    const val SILENT_SEVERE = "alertaec_silent_severe_v1"
    const val SILENT_EXTREME = "alertaec_silent_extreme_v1"

    // Only official provider severity authorizes an intrusive channel.
    fun officialChannel(severity: AlertSeverity): String = when (severity) {
        AlertSeverity.EXTREME -> EXTREME
        AlertSeverity.SEVERE -> SEVERE
        else -> "forecast"
    }

    /** Selects a sound only from an already-supported phenomenon/severity pair. */
    fun officialSoundChannel(phenomenon: AlertaEcPhenomenon, severity: AlertSeverity): String? = when {
        severity == AlertSeverity.EXTREME -> SOUND_CRITICAL
        severity == AlertSeverity.SEVERE && phenomenon == AlertaEcPhenomenon.FLOOD -> SOUND_FLOOD_LANDSLIDE
        severity == AlertSeverity.SEVERE && phenomenon == AlertaEcPhenomenon.LANDSLIDE -> SOUND_FLOOD_LANDSLIDE
        severity == AlertSeverity.SEVERE && phenomenon == AlertaEcPhenomenon.VOLCANIC_ACTIVITY -> SOUND_VOLCANIC
        else -> null
    }

    fun eventSoundChannel(phenomenon: AlertaEcPhenomenon, severity: AlertaEcSeverity): String? = when {
        severity == AlertaEcSeverity.EMERGENCY -> SOUND_CRITICAL
        severity == AlertaEcSeverity.WARNING && phenomenon in setOf(AlertaEcPhenomenon.FLOOD, AlertaEcPhenomenon.LANDSLIDE) -> SOUND_FLOOD_LANDSLIDE
        severity == AlertaEcSeverity.WARNING && phenomenon == AlertaEcPhenomenon.VOLCANIC_ACTIVITY -> SOUND_VOLCANIC
        severity <= AlertaEcSeverity.ADVISORY && phenomenon == AlertaEcPhenomenon.THUNDERSTORM -> SOUND_STORM
        severity <= AlertaEcSeverity.ADVISORY && phenomenon in setOf(AlertaEcPhenomenon.RAIN, AlertaEcPhenomenon.HEAVY_RAIN) -> SOUND_RAIN
        else -> null
    }

    fun eventChannel(severity: AlertaEcSeverity): String = when (severity) {
        AlertaEcSeverity.EMERGENCY -> EXTREME
        AlertaEcSeverity.WATCH, AlertaEcSeverity.WARNING -> SEVERE
        else -> "forecast"
    }

    fun soundResource(channel: String): Int? = when (channel) {
        SOUND_RAIN -> R.raw.alertaec_tono_lluvia_informativa
        SOUND_STORM -> R.raw.alertaec_tono_tormenta_preventiva
        SOUND_FLOOD_LANDSLIDE -> R.raw.alertaec_tono_inundacion_deslizamiento_grave
        SOUND_VOLCANIC -> R.raw.alertaec_tono_actividad_volcanica
        SOUND_CRITICAL -> R.raw.alertaec_tono_emergencia_critica
        SOUND_RAIN_NO_VIBRATION -> R.raw.alertaec_tono_lluvia_informativa
        SOUND_STORM_NO_VIBRATION -> R.raw.alertaec_tono_tormenta_preventiva
        SOUND_FLOOD_LANDSLIDE_NO_VIBRATION -> R.raw.alertaec_tono_inundacion_deslizamiento_grave
        SOUND_VOLCANIC_NO_VIBRATION -> R.raw.alertaec_tono_actividad_volcanica
        SOUND_CRITICAL_NO_VIBRATION -> R.raw.alertaec_tono_emergencia_critica
        else -> null
    }

    fun icon(phenomenon: AlertaEcPhenomenon): Int = when (phenomenon) {
        AlertaEcPhenomenon.RAIN, AlertaEcPhenomenon.HEAVY_RAIN -> R.drawable.ic_notification_rain
        AlertaEcPhenomenon.THUNDERSTORM -> R.drawable.ic_notification_thunderstorm
        AlertaEcPhenomenon.STRONG_WIND -> R.drawable.ic_notification_wind
        else -> R.drawable.ic_notification_weather
    }
}


internal enum class AlertaEcRainEpisodeState {
    NO_DATA, NO_RAIN, PROBABLE, IMMINENT, ACTIVE, PAUSE, RESUMED, FINISHED
}

internal data class AlertaEcRainDecision(
    val state: AlertaEcRainEpisodeState,
    val validUntilMillis: Long? = null,
    val windowMinutes: Int? = null,
    val transitionKey: String? = null,
) {
    val canNotifyPreventively: Boolean
        get() = state == AlertaEcRainEpisodeState.IMMINENT ||
            state == AlertaEcRainEpisodeState.RESUMED ||
            state == AlertaEcRainEpisodeState.ACTIVE
}

/** Pure interpretation of a minutely forecast; it never claims an observed ground event. */
internal object AlertaEcRainEpisodeEvaluator {
    private const val MAX_FIRST_SAMPLE_AGE_MILLIS = 30 * 60 * 1000L

    fun evaluate(samples: List<Minutely>, nowMillis: Long): AlertaEcRainDecision {
        if (samples.isEmpty()) return AlertaEcRainDecision(AlertaEcRainEpisodeState.NO_DATA)
        val cleaned = samples.map { sample ->
            val intensity = sample.precipitationIntensity?.inMillimeters
            if (sample.date.time <= 0L || sample.minuteInterval <= 0 ||
                (intensity != null && (!intensity.isFinite() || intensity < 0.0))
            ) return AlertaEcRainDecision(AlertaEcRainEpisodeState.NO_DATA)
            sample
        }
        if (cleaned.zipWithNext().any { (a, b) ->
                b.date.time <= a.date.time || b.date.time - a.date.time != a.minuteInterval * 60_000L
            }
        ) return AlertaEcRainDecision(AlertaEcRainEpisodeState.NO_DATA)

        val first = cleaned.first().date.time
        if (first > nowMillis + MAX_FIRST_SAMPLE_AGE_MILLIS ||
            nowMillis - first > MAX_FIRST_SAMPLE_AGE_MILLIS
        ) return AlertaEcRainDecision(AlertaEcRainEpisodeState.NO_DATA)

        val wet = cleaned.map { (it.precipitationIntensity?.inMillimeters ?: 0.0) > 0.0 }
        val currentIndex = cleaned.indexOfFirst { sample ->
            sample.date.time <= nowMillis && nowMillis < sample.endingDate.time
        }
        if (currentIndex < 0) {
            val ended = cleaned.last().endingDate.time <= nowMillis
            if (ended && wet.any { it }) {
                // The last sample boundary is the real episode end; keep this key
                // stable across repeated evaluations and leave terminal validity null.
                val finishedAt = cleaned.last().endingDate.time
                return AlertaEcRainDecision(
                    state = AlertaEcRainEpisodeState.FINISHED,
                    transitionKey = "${AlertaEcRainEpisodeState.FINISHED}:$finishedAt",
                )
            }
            return AlertaEcRainDecision(AlertaEcRainEpisodeState.NO_DATA)
        }

        val currentWet = wet[currentIndex]
        val previousWet = currentIndex > 0 && wet[currentIndex - 1]
        val previousDry = currentIndex > 0 && !wet[currentIndex - 1]
        val futureWetIndex = (currentIndex + 1 until cleaned.size).firstOrNull { wet[it] }
        val futureWet = futureWetIndex != null
        val priorWetBeforeDry = currentIndex >= 2 && wet.take(currentIndex - 1).any { it }
        val nextDry = currentIndex + 1 < cleaned.size && !wet[currentIndex + 1]
        val state = when {
            currentWet && previousDry && priorWetBeforeDry -> AlertaEcRainEpisodeState.RESUMED
            currentWet && nextDry -> AlertaEcRainEpisodeState.PAUSE
            currentWet -> AlertaEcRainEpisodeState.ACTIVE
            !currentWet && previousWet && futureWet -> AlertaEcRainEpisodeState.PAUSE
            !currentWet && previousWet && !futureWet -> AlertaEcRainEpisodeState.FINISHED
            futureWet -> {
                val start = cleaned[futureWetIndex!!].date.time
                if (start - nowMillis <= cleaned[futureWetIndex].minuteInterval * 60_000L) {
                    AlertaEcRainEpisodeState.IMMINENT
                } else {
                    AlertaEcRainEpisodeState.PROBABLE
                }
            }
            else -> AlertaEcRainEpisodeState.NO_RAIN
        }
        val transitionTime = when {
            state == AlertaEcRainEpisodeState.PAUSE || state == AlertaEcRainEpisodeState.FINISHED -> cleaned[currentIndex].date.time
            futureWetIndex != null -> cleaned[futureWetIndex].date.time
            else -> cleaned[currentIndex].date.time
        }
        return AlertaEcRainDecision(
            state = state,
            validUntilMillis = cleaned[currentIndex].endingDate.time,
            windowMinutes = if (futureWetIndex != null) {
                ((cleaned[futureWetIndex].date.time - nowMillis) / 60_000L).toInt().coerceAtLeast(0)
            } else 0,
            transitionKey = "$state:$transitionTime"
        )
    }

    fun transitionKeyAfter(previousState: AlertaEcRainEpisodeState?, decision: AlertaEcRainDecision): String? {
        if (decision.transitionKey == null) return null
        return if (previousState == AlertaEcRainEpisodeState.PAUSE && decision.canNotifyPreventively) {
            "${AlertaEcRainEpisodeState.RESUMED}:$decision.transitionKey"
        } else decision.transitionKey
    }

    fun stableEpisodeKey(
        previousState: AlertaEcRainEpisodeState?,
        previousEpisodeKey: String?,
        decision: AlertaEcRainDecision,
    ): String? {
        if (decision.transitionKey == null) return previousEpisodeKey
        return if (previousEpisodeKey != null && previousState != null &&
            previousState != AlertaEcRainEpisodeState.FINISHED &&
            previousState != AlertaEcRainEpisodeState.NO_DATA &&
            previousState != AlertaEcRainEpisodeState.NO_RAIN
        ) previousEpisodeKey else "EPISODE:${decision.transitionKey}"
    }

    fun stableNotificationTransitionKey(
        previousState: AlertaEcRainEpisodeState?,
        episodeKey: String?,
        previousTransitionKey: String?,
        decision: AlertaEcRainDecision,
    ): String? {
        if (!decision.canNotifyPreventively || episodeKey == null) return null
        if (previousState == AlertaEcRainEpisodeState.PAUSE) return "RESUMED:$episodeKey"
        return if (previousTransitionKey?.endsWith(episodeKey) == true) previousTransitionKey else episodeKey
    }
}
