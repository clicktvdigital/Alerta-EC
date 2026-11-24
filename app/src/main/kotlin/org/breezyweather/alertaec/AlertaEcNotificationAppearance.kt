package org.breezyweather.alertaec

import org.breezyweather.R
import breezyweather.domain.weather.model.Minutely
import breezyweather.domain.weather.reference.AlertSeverity

/**
 * Natural sound + vibration identity of each phenomenon. A notification channel freezes its sound and
 * vibration when it is first created, so every change of sound/pattern must bump [CHANNEL_VERSION].
 */
internal enum class AlertaEcSoundProfile(
    val key: String,
    val sound: Int,
    val vibration: LongArray,
    val highImportance: Boolean,
) {
    RAIN("rain", R.raw.alertaec_intro_rain, longArrayOf(0, 120, 90, 120, 90, 120), false),
    STORM("storm", R.raw.alertaec_intro_storm, longArrayOf(0, 600, 180, 220, 120, 220), false),
    HAIL("hail", R.raw.alertaec_tono_granizo, longArrayOf(0, 60, 40, 60, 40, 60, 40, 60, 40, 60), true),
    WIND("wind", R.raw.alertaec_intro_wind, longArrayOf(0, 800, 300, 800), false),
    SEISMIC("seismic", R.raw.alertaec_intro_seismic, longArrayOf(0, 100, 50, 100, 50, 100, 50, 700), true),
    FLOOD_LANDSLIDE("flood_landslide", R.raw.alertaec_tono_inundacion_deslizamiento_grave, longArrayOf(0, 500, 200, 500), true),
    VOLCANIC("volcanic", R.raw.alertaec_tono_actividad_volcanica, longArrayOf(0, 700, 250, 700), true),
    CRITICAL("critical", R.raw.alertaec_tono_emergencia_critica, longArrayOf(0, 500, 200, 500, 200, 500), true),
    ;

    val channelId: String get() = "alertaec_sound_${key}_$CHANNEL_VERSION"
    val silentVibrationChannelId: String get() = "alertaec_sound_${key}_no_vibration_$CHANNEL_VERSION"

    companion object {
        // v4: v3 rain channel could be created without its sound (system default tone), and phenomena
        // shared a single 180 ms vibration.
        // v5: the channel stores the tone by resource NAME (android.resource://pkg/raw/name). v4 stored the
        // numeric id, which shifts whenever raw resources change, leaving frozen channels on a wrong/missing tone.
        const val CHANNEL_VERSION = "v5"

        fun forPhenomenon(phenomenon: AlertaEcPhenomenon): AlertaEcSoundProfile? = when (phenomenon) {
            AlertaEcPhenomenon.RAIN, AlertaEcPhenomenon.HEAVY_RAIN -> RAIN
            AlertaEcPhenomenon.THUNDERSTORM, AlertaEcPhenomenon.TROPICAL_CYCLONE -> STORM
            AlertaEcPhenomenon.HAIL, AlertaEcPhenomenon.SNOW -> HAIL
            AlertaEcPhenomenon.STRONG_WIND -> WIND
            AlertaEcPhenomenon.EARTHQUAKE, AlertaEcPhenomenon.TSUNAMI -> SEISMIC
            AlertaEcPhenomenon.FLOOD, AlertaEcPhenomenon.LANDSLIDE -> FLOOD_LANDSLIDE
            AlertaEcPhenomenon.VOLCANIC_ACTIVITY, AlertaEcPhenomenon.WILDFIRE -> VOLCANIC
            else -> null
        }

        fun forChannel(channelId: String): AlertaEcSoundProfile? =
            entries.firstOrNull { it.channelId == channelId || it.silentVibrationChannelId == channelId }
    }
}

internal object AlertaEcNotificationAppearance {
    const val PRECIPITATION = "weather_precipitation"
    const val PRECIPITATION_IMMINENT = "weather_precipitation_v3"
    const val SEVERE = "weather_severe"
    const val EXTREME = "weather_extreme"

    val SOUND_RAIN = AlertaEcSoundProfile.RAIN.channelId
    val SOUND_STORM = AlertaEcSoundProfile.STORM.channelId
    val SOUND_HAIL = AlertaEcSoundProfile.HAIL.channelId
    val SOUND_WIND = AlertaEcSoundProfile.WIND.channelId
    val SOUND_SEISMIC = AlertaEcSoundProfile.SEISMIC.channelId
    val SOUND_FLOOD_LANDSLIDE = AlertaEcSoundProfile.FLOOD_LANDSLIDE.channelId
    val SOUND_VOLCANIC = AlertaEcSoundProfile.VOLCANIC.channelId
    val SOUND_CRITICAL = AlertaEcSoundProfile.CRITICAL.channelId
    val SOUND_RAIN_NO_VIBRATION = AlertaEcSoundProfile.RAIN.silentVibrationChannelId
    val SOUND_CRITICAL_NO_VIBRATION = AlertaEcSoundProfile.CRITICAL.silentVibrationChannelId
    const val VIBRATION_FORECAST = "alertaec_vibration_forecast_v1"
    const val VIBRATION_SEVERE = "alertaec_vibration_severe_v1"
    const val VIBRATION_EXTREME = "alertaec_vibration_extreme_v1"
    // Stable visual-only channels for the both-preferences-disabled case.
    const val SILENT_SEVERE = "alertaec_silent_severe_v1"
    const val SILENT_EXTREME = "alertaec_silent_extreme_v1"

    /** Channels of previous versions; deleted so Android settings only list the current ones. */
    val OBSOLETE_SOUND_CHANNELS = listOf("rain", "storm", "flood_landslide", "volcanic", "critical").flatMap {
        listOf("alertaec_sound_${it}_v3", "alertaec_sound_${it}_no_vibration_v3")
    } + AlertaEcSoundProfile.entries.flatMap {
        listOf("alertaec_sound_${it.key}_v4", "alertaec_sound_${it.key}_no_vibration_v4")
    }

    // Only official provider severity authorizes an intrusive channel.
    fun officialChannel(severity: AlertSeverity): String = when (severity) {
        AlertSeverity.EXTREME -> EXTREME
        AlertSeverity.SEVERE -> SEVERE
        else -> "forecast"
    }

    /** Official alerts play the sound of their phenomenon; extreme ones always use the critical tone. */
    fun officialSoundChannel(phenomenon: AlertaEcPhenomenon, severity: AlertSeverity): String? = when (severity) {
        AlertSeverity.EXTREME -> SOUND_CRITICAL
        AlertSeverity.SEVERE, AlertSeverity.MODERATE -> AlertaEcSoundProfile.forPhenomenon(phenomenon)?.channelId
        else -> null
    }

    fun eventSoundChannel(phenomenon: AlertaEcPhenomenon, severity: AlertaEcSeverity): String? =
        if (severity == AlertaEcSeverity.EMERGENCY) SOUND_CRITICAL
        else AlertaEcSoundProfile.forPhenomenon(phenomenon)?.channelId

    fun eventChannel(severity: AlertaEcSeverity): String = when (severity) {
        AlertaEcSeverity.EMERGENCY -> EXTREME
        AlertaEcSeverity.WATCH, AlertaEcSeverity.WARNING -> SEVERE
        else -> "forecast"
    }

    fun soundResource(channel: String): Int? = AlertaEcSoundProfile.forChannel(channel)?.sound

    /** Vibration of the phenomenon; also applied on the builder for Android < 8. */
    fun vibrationPattern(phenomenon: AlertaEcPhenomenon, severity: AlertaEcSeverity): LongArray =
        (if (severity == AlertaEcSeverity.EMERGENCY) AlertaEcSoundProfile.CRITICAL else AlertaEcSoundProfile.forPhenomenon(phenomenon))
            ?.vibration ?: longArrayOf(0, 180)

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
