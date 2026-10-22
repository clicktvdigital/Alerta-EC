package org.breezyweather.alertaec.accessibility

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import org.breezyweather.domain.settings.SettingsManager
import java.lang.ref.WeakReference

/** Short, local speech only while the requesting activity is visible. No service or audio backlog. */
object AlertVoice {
    private val handler = Handler(Looper.getMainLooper())
    private var screen = WeakReference<Activity>(null)
    private var ready = false
    private var engine: TextToSpeech? = null
    private var audio: AudioManager? = null
    private var focus: AudioFocusRequest? = null
    private var generation = 0
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change != AudioManager.AUDIOFOCUS_GAIN) stop()
    }

    fun resume(activity: Activity) {
        screen = WeakReference(activity)
        ready = false
    }

    /** Called only after interaction with the loaded main screen, outside the introduction. */
    fun ready() { ready = true }

    fun pause() {
        ready = false
        screen.clear()
        stop()
    }

    fun stop() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { stop() }
            return
        }
        generation++
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine?.shutdown()
        engine = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focus?.let { audio?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audio?.abandonAudioFocus(focusListener)
        }
        focus = null
        audio = null
    }

    fun alert(context: Context, channel: String, phenomenon: String) {
        handler.post {
            val activity = screen.get() ?: return@post
            if (!ready) return@post
            speak(activity, channel, AlertVoicePolicy.summary(phenomenon), false) { }
        }
    }

    fun test(activity: Activity, result: (String) -> Unit) {
        speak(activity, null, "Esta es una prueba de voz de ALERTA EC. No es una alerta real.", true, result)
    }

    private fun permitted(activity: Activity, channel: String?, test: Boolean): Boolean {
        val settings = SettingsManager.getInstance(activity)
        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notifications = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val accessibility = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (activity.isFinishing || activity.isDestroyed ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
            !activity.hasWindowFocus() ||
            (activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked ||
            !settings.isAlertVoiceEnabled || !settings.isAlertSoundEnabled ||
            audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
            audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0 ||
            audioManager.isMusicActive || audioManager.mode != AudioManager.MODE_NORMAL ||
            notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        ) return false
        // TalkBack owns automatic announcements; an explicit voice test remains available.
        if (!test && (!ready || accessibility.isTouchExplorationEnabled || !settings.isAlertPushEnabled)) return false
        if (!test) {
            val manager = NotificationManagerCompat.from(activity)
            if (!manager.areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = manager.getNotificationChannel(channel ?: return false) ?: return false
                if (notificationChannel.importance < NotificationManager.IMPORTANCE_DEFAULT ||
                    notificationChannel.sound == null
                ) return false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    notificationChannel.group?.let { manager.getNotificationChannelGroup(it)?.isBlocked } == true
                ) return false
            }
        }
        return true
    }

    private fun speak(activity: Activity, channel: String?, text: String, test: Boolean, result: (String) -> Unit) {
        if (!permitted(activity, channel, test)) {
            result("Voz no disponible: revisa la preferencia de voz, sonido, volumen y No molestar.")
            return
        }
        if (engine != null) {
            result("Ya hay una lectura en curso. Puedes detenerla antes de probar otra vez.")
            return
        }
        val token = ++generation
        fun finish(message: String) {
            handler.post {
                if (token == generation) {
                    stop()
                    result(message)
                }
            }
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        engine = TextToSpeech(activity.applicationContext) { status ->
            handler.post {
                if (token != generation) return@post
                val tts = engine ?: return@post
                val voice = if (status == TextToSpeech.SUCCESS) {
                    tts.voices?.filter {
                        it.locale.language == "es" && !it.isNetworkConnectionRequired &&
                            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
                    }?.sortedBy { if (it.locale.country == "EC") 0 else 1 }?.firstOrNull()
                } else null
                if (voice == null) {
                    finish("No hay una voz local en español disponible. Revisa el motor de texto a voz de Android.")
                    return@post
                }
                if (!permitted(activity, channel, test)) {
                    finish("Lectura cancelada por las preferencias o el estado de Android.")
                    return@post
                }
                audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener).build()
                    audio!!.requestAudioFocus(focus!!)
                } else {
                    @Suppress("DEPRECATION")
                    audio!!.requestAudioFocus(focusListener, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                }
                if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED ||
                    tts.setVoice(voice) == TextToSpeech.ERROR
                ) {
                    finish("Android bloqueó el audio o no pudo iniciar la voz.")
                    return@post
                }
                tts.setAudioAttributes(attributes)
                tts.setSpeechRate(AlertVoicePolicy.speechRate(1.0f))
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { finish("Prueba de voz finalizada.") }
                    @Deprecated("Required by Android")
                    override fun onError(utteranceId: String?) { finish("El motor de voz no pudo completar la lectura.") }
                })
                if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alerta-ec-$token") == TextToSpeech.ERROR) {
                    finish("No se pudo iniciar la lectura.")
                } else {
                    result("Reproduciendo prueba de voz.")
                }
            }
        }
        // Recheck lock, silence, preference and focus throughout initialization/playback.
        val guard = object : Runnable {
            override fun run() {
                if (token != generation) return
                if (!permitted(activity, channel, test)) {
                    finish("Lectura detenida por el estado de Android.")
                } else handler.postDelayed(this, 250)
            }
        }
        handler.postDelayed(guard, 250)
        handler.postDelayed({ finish("Tiempo de lectura agotado.") }, 20_000)
    }
}
