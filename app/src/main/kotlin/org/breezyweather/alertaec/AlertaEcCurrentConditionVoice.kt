package org.breezyweather.alertaec

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.reference.WeatherCode
import org.breezyweather.R
import org.breezyweather.alertaec.accessibility.AlertVoice
import org.breezyweather.alertaec.accessibility.AlertVoicePolicy
import org.breezyweather.domain.settings.SettingsManager

/**
 * After a weather update with news, ALERTA EC plays the tone of the real current condition and then says
 * it: "Estamos con niebla ahorita en Quito. 12 grados. Usa luces bajas." Fog/haze, rain, thunderstorm, hail,
 * strong wind and snow each have their own tone; when it clears up after one of them, a soft closing tone
 * and "Ya despejó en Quito." Once per change (the same condition is not repeated for 2 hours) and only if
 * this update cycle has not already sounded (see [AlertaEcAudibleGate]).
 */
object AlertaEcCurrentConditionVoice {
    private const val PREFS = "alertaec_current_condition_voice"
    private const val REPEAT_AFTER_MILLIS = 2 * 60 * 60 * 1000L
    private const val TAG = "AlertaEcVoz"
    private const val CLEAR = "CLEAR"

    private val notable = setOf(
        AlertaEcPhenomenon.RAIN,
        AlertaEcPhenomenon.THUNDERSTORM,
        AlertaEcPhenomenon.HAIL,
        AlertaEcPhenomenon.FOG,
        AlertaEcPhenomenon.STRONG_WIND,
        AlertaEcPhenomenon.SNOW,
    )

    /** Real current condition → phenomenon; haze ("neblina") counts as fog here. */
    internal fun conditionOf(code: WeatherCode?): String = when (code) {
        null -> "NONE"
        WeatherCode.HAZE -> AlertaEcPhenomenon.FOG.name
        WeatherCode.SLEET -> AlertaEcPhenomenon.RAIN.name
        WeatherCode.CLEAR, WeatherCode.PARTLY_CLOUDY -> CLEAR
        else -> AlertaEcWeatherAdapter.phenomenonFromWeatherCode(code)?.takeIf { it in notable }?.name ?: "NONE"
    }

    fun announceIfNew(context: Context, location: Location, nowMillis: Long = System.currentTimeMillis()) {
        val settings = SettingsManager.getInstance(context)
        if (!settings.isAlertSoundEnabled || !settings.isAlertVoiceEnabled) return
        val current = location.weather?.current ?: return
        val code = conditionOf(current.weatherCode)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "last_${location.formattedId}"
        val previous = prefs.getString(key, null)
        val previousAt = prefs.getLong("${key}_at", 0L)
        if (code == previous && nowMillis - previousAt < REPEAT_AFTER_MILLIS) return
        prefs.edit().putString(key, code).putLong("${key}_at", nowMillis).apply()

        val city = location.city.ifBlank { location.geographicName }
        val place = if (city.isNotBlank()) " en $city" else ""
        val celsius = current.temperature?.temperature?.inCelsius
        val degrees = AlertaEcNotifier.spokenDegrees(celsius, settings.getTemperatureUnit(context))
        val phenomenon = runCatching { AlertaEcPhenomenon.valueOf(code) }.getOrNull()
        val clearedUp = code == CLEAR && previous != null && previous != CLEAR && previous != "NONE"
        val (tone, text) = when {
            phenomenon != null && phenomenon in notable ->
                (AlertaEcSoundProfile.forPhenomenon(phenomenon)?.sound) to
                    "Estamos con ${phenomenon.spokenName} ahorita$place. " +
                    AlertVoicePolicy.conciseSpeech(phenomenon = phenomenon, degrees = degrees, celsius = celsius)
            clearedUp ->
                R.raw.alertaec_intro_closing to
                    "Ya despejó$place." + (degrees?.let { if (it == 1 || it == -1) " $it grado." else " $it grados." } ?: "")
            else -> return
        }
        // Only the first notice of an update cycle sounds (official alerts and imminent rain go first).
        if (!AlertaEcAudibleGate.claim(context, nowMillis = nowMillis)) {
            Log.i(TAG, "Condición $code$place: este ciclo ya sonó, no se repite")
            return
        }
        Log.i(TAG, "Condición actual con novedad: $code$place")
        tone?.let { playTone(context, it) }
        phenomenon?.let { AlertaEcSoundProfile.forPhenomenon(it) }?.let { AlertaEcAudibleGate.vibrate(context, it.vibration) }
        AlertVoice.speakFromBackground(
            context,
            text,
            dedupeKey = "current_${location.formattedId}_${code}_${nowMillis / REPEAT_AFTER_MILLIS}",
        )
    }

    /** The ALERTA EC tone of the condition (not Android's generic notification sound). */
    private fun playTone(context: Context, sound: Int) {
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
        }.onFailure { Log.w(TAG, "No se pudo reproducir el tono", it) }
    }
}
