package org.breezyweather.alertaec.tv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.support.isTelevisionDevice

/**
 * Emergency voice on Android TV / TV Box: reads a WARNING/EMERGENCY alert aloud even while another app (a film,
 * YouTube/TizenTube) is playing. Opt-in: only if the user enabled alert voice (off by default on TV).
 *
 * It asks for transient audio focus, so the video pauses (or ducks) while the alert is spoken and resumes after.
 * Unlike [org.breezyweather.alertaec.accessibility.AlertVoice] it does not require the app on screen and does not
 * stay silent when media is playing: that is exactly the case it exists for.
 */
object AlertaEcEmergencyVoice {
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null

    fun speakIfAllowed(context: Context, text: String) {
        val app = context.applicationContext
        if (!app.isTelevisionDevice()) return
        val settings = SettingsManager.getInstance(app)
        if (!settings.isAlertVoiceEnabled || !settings.isAlertSoundEnabled) return
        handler.post { speak(app, text) }
    }

    private fun speak(app: Context, text: String) {
        release()
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val focus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
        } else {
            null
        }
        fun done() = handler.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focus?.let { audio.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audio.abandonAudioFocus(null)
            }
            release()
        }
        var engine: TextToSpeech? = null
        engine = TextToSpeech(app) { status ->
            val voice = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                done()
                return@TextToSpeech
            }
            val ecuador = Locale.forLanguageTag("es-EC")
            if (voice.isLanguageAvailable(ecuador) >= TextToSpeech.LANG_AVAILABLE) {
                voice.language = ecuador
            } else {
                voice.language = Locale.forLanguageTag("es")
            }
            voice.setAudioAttributes(attributes)
            voice.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    done()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    done()
                }
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focus?.let { audio.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audio.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
            voice.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alertaec_emergency")
        }
        tts = engine
    }

    private fun release() {
        tts?.let { runCatching { it.stop(); it.shutdown() } }
        tts = null
    }
}
