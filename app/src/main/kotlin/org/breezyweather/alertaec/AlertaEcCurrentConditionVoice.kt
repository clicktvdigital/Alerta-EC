package org.breezyweather.alertaec

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import breezyweather.domain.location.model.Location
import org.breezyweather.alertaec.accessibility.AlertVoice
import org.breezyweather.alertaec.accessibility.AlertVoicePolicy
import org.breezyweather.domain.settings.SettingsManager

/**
 * After a weather update with news (the current condition changed to fog, rain, storm, hail, wind or
 * snow), ALERTA EC plays the phenomenon's own tone and then says it, e.g. "Estamos con niebla ahorita en
 * Quito. 12 grados. Usa luces bajas." Once per change: the same condition is not repeated for 2 hours,
 * and AlertVoice also de-duplicates across processes. Silent when sound or voice is turned off.
 */
object AlertaEcCurrentConditionVoice {
    private const val PREFS = "alertaec_current_condition_voice"
    private const val REPEAT_AFTER_MILLIS = 2 * 60 * 60 * 1000L
    private const val TAG = "AlertaEcVoz"

    private val notable = setOf(
        AlertaEcPhenomenon.RAIN,
        AlertaEcPhenomenon.THUNDERSTORM,
        AlertaEcPhenomenon.HAIL,
        AlertaEcPhenomenon.FOG,
        AlertaEcPhenomenon.STRONG_WIND,
        AlertaEcPhenomenon.SNOW,
    )

    fun announceIfNew(context: Context, location: Location, nowMillis: Long = System.currentTimeMillis()) {
        val settings = SettingsManager.getInstance(context)
        if (!settings.isAlertSoundEnabled || !settings.isAlertVoiceEnabled) return
        val current = location.weather?.current ?: return
        val phenomenon = current.weatherCode?.let { AlertaEcWeatherAdapter.phenomenonFromWeatherCode(it) }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "last_${location.formattedId}"
        val previous = prefs.getString(key, null)
        val previousAt = prefs.getLong("${key}_at", 0L)
        val code = phenomenon?.name ?: "NONE"
        if (code == previous && nowMillis - previousAt < REPEAT_AFTER_MILLIS) return
        prefs.edit().putString(key, code).putLong("${key}_at", nowMillis).apply()
        if (phenomenon == null || phenomenon !in notable) return

        val celsius = current.temperature?.temperature?.inCelsius
        val city = location.city.ifBlank { location.geographicName }
        val text = "Estamos con ${phenomenon.spokenName} ahorita" +
            (if (city.isNotBlank()) " en $city. " else ". ") +
            AlertVoicePolicy.conciseSpeech(
                phenomenon = phenomenon,
                degrees = AlertaEcNotifier.spokenDegrees(celsius, settings.getTemperatureUnit(context)),
                celsius = celsius,
            )
        Log.i(TAG, "Condición actual con novedad: ${phenomenon.name} en $city")
        playTone(context, phenomenon)
        AlertVoice.speakFromBackground(
            context,
            text,
            dedupeKey = "current_${location.formattedId}_${phenomenon.name}_${nowMillis / REPEAT_AFTER_MILLIS}",
        )
    }

    /** The ALERTA EC tone of the phenomenon (not Android's generic notification sound). */
    private fun playTone(context: Context, phenomenon: AlertaEcPhenomenon) {
        val sound = AlertaEcSoundProfile.forPhenomenon(phenomenon)?.sound ?: return
        runCatching {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            context.resources.openRawResourceFd(sound).use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.prepare()
            player.start()
        }.onFailure { Log.w(TAG, "No se pudo reproducir el tono de ${phenomenon.name}", it) }
    }
}
