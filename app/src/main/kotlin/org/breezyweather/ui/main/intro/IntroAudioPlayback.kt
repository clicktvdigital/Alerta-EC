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
    private const val MAX_VOLUME = 0.12f
    private val handler = Handler(Looper.getMainLooper())
    private val players = mutableSetOf<MediaPlayer>()
    private var pendingCues = 0
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change < 0) stop()
    }

    data class Cue(val delayMs: Long, val resourceId: Int)

    fun start(context: Context, cues: List<Cue>, userEnabled: Boolean) {
        stop()
        if (!userEnabled || cues.isEmpty() || cues.any { it.resourceId == 0 }) return
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (!IntroPlaybackPolicy.shouldPlayAudio(
                userEnabled,
                cues.firstOrNull()?.resourceId ?: 0,
                manager.ringerMode,
                manager.getStreamVolume(AudioManager.STREAM_MUSIC),
            )
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
            pendingCues = cues.size
            cues.forEach { cue ->
                handler.postDelayed({ playCue(context.applicationContext, attributes, cue) }, cue.delayMs)
            }
        } catch (_: RuntimeException) {
            stop()
        }
    }

    private fun playCue(context: Context, attributes: AudioAttributes, cue: Cue) {
        pendingCues--
        try {
            val activePlayer = MediaPlayer.create(context, cue.resourceId)
            if (activePlayer == null) {
                if (pendingCues == 0 && players.isEmpty()) abandonFocus()
                return
            }
            players += activePlayer
            activePlayer.setAudioAttributes(attributes)
            activePlayer.setVolume(MAX_VOLUME, MAX_VOLUME)
            activePlayer.setOnCompletionListener {
                players.remove(it)
                it.release()
                if (pendingCues == 0 && players.isEmpty()) abandonFocus()
            }
            activePlayer.start()
        } catch (_: RuntimeException) {
            stop()
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        players.toList().forEach { activePlayer ->
            runCatching { activePlayer.stop() }
            runCatching { activePlayer.release() }
        }
        players.clear()
        pendingCues = 0
        abandonFocus()
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
