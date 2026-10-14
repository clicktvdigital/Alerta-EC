package org.breezyweather.ui.main.intro

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper

/** Plays only an explicitly supplied bundled track after the user opts in. */
internal object IntroAudioPlayback {
    private const val MAX_VOLUME = 0.22f
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change < 0) stop()
    }

    fun start(context: Context, rawResourceId: Int, userEnabled: Boolean) {
        stop()
        if (!userEnabled || rawResourceId == 0) return
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (manager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
            manager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        ) return
        audioManager = manager
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val focusGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(false)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        if (!focusGranted) {
            abandonFocus()
            return
        }
        try {
            player = MediaPlayer.create(context.applicationContext, rawResourceId)?.apply {
                setAudioAttributes(attributes)
                setVolume(0f, 0f)
                setOnCompletionListener { stop() }
                start()
            }
            if (player == null) {
                abandonFocus()
                return
            }
            fadeIn()
        } catch (_: RuntimeException) {
            stop()
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.let { activePlayer -> runCatching { activePlayer.stop() } }
        player?.release()
        player = null
        abandonFocus()
    }

    private fun fadeIn(step: Int = 0) {
        val activePlayer = player ?: return
        val volume = (step / 8f * MAX_VOLUME).coerceIn(0f, MAX_VOLUME)
        activePlayer.setVolume(volume, volume)
        if (step < 8) handler.postDelayed({ fadeIn(step + 1) }, 35L)
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(manager::abandonAudioFocusRequest)
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(focusListener)
        }
        audioManager = null
    }
}
