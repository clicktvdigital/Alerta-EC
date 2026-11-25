package org.breezyweather.alertaec

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.AudioAttributes
import org.breezyweather.domain.settings.SettingsManager

/**
 * One sound and one voice per weather update. A refresh can produce an official alert, an imminent-rain
 * notice, forecast events and a current-condition announcement at the same moment; only the first one
 * (in that priority order, which is the order WeatherUpdateJob runs them) plays its tone and voice, the
 * rest are posted silently. Shared across processes through SharedPreferences.
 */
object AlertaEcAudibleGate {
    private const val PREFS = "alertaec_audible_gate"
    private const val KEY_LAST = "last_audible_at"

    /** Window that groups everything produced by one update cycle. */
    const val CYCLE_WINDOW_MILLIS = 90_000L

    internal fun canClaim(lastAudibleAt: Long, nowMillis: Long, priority: Boolean): Boolean =
        priority || lastAudibleAt <= 0L || nowMillis - lastAudibleAt !in 0 until CYCLE_WINDOW_MILLIS

    /**
     * True when this notification may sound (and speak). [priority] (extreme official alerts) always sounds.
     */
    @Synchronized
    fun claim(context: Context, priority: Boolean = false, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST, 0L)
        if (!canClaim(last, nowMillis, priority)) return false
        prefs.edit().putLong(KEY_LAST, nowMillis).commit()
        return true
    }

    /** Vibrates with the phenomenon pattern when the user keeps vibration on (channels v6 do not vibrate). */
    fun vibrate(context: Context, pattern: LongArray) {
        if (!SettingsManager.getInstance(context).isAlertVibrationEnabled || pattern.size < 2) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), attributes)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1, attributes)
            }
        }
    }
}
