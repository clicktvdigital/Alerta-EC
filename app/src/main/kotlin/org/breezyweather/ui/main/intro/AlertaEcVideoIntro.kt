package org.breezyweather.ui.main.intro

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import org.breezyweather.R

/**
 * Reproduce un vídeo local con sonido de forma predeterminada.
 * Respeta el volumen multimedia, el modo silencio y la preferencia de movimiento reducido.
 * No necesita Internet. Nunca toca los controles de volumen del dispositivo.
 */
internal object AlertaEcVideoIntro {
    private val handler = Handler(Looper.getMainLooper())
    private var activeVideo: VideoView? = null
    private var activeRoot: FrameLayout? = null
    private var pendingTimeout: Runnable? = null
    private var activePoster: ImageView? = null

    fun play(
        root: View,
        visualLayers: List<View>,
        skip: View,
        reducedMotionControl: View,
        soundControl: View,
    ): Boolean {
        // Solo insertar VideoView en un contenedor compatible.
        val frame = root as? FrameLayout ?: return false
        val context = frame.context
        val preferences = context.getSharedPreferences("alerta_ec_intro", Context.MODE_PRIVATE)
        val animatorScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        val animationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            ValueAnimator.areAnimatorsEnabled()
        val reduceMotion = IntroPlaybackPolicy.shouldReduceMotion(
            preferences.getBoolean("reduced_motion", false),
            animatorScale,
            animationsEnabled,
        )
        stop()
        if (reduceMotion) {
            frame.visibility = View.GONE
            return true
        }

        val view = VideoView(context)
        activeVideo = view
        activeRoot = frame
        try {
            // Ocultar SOLO las capas antiguas de la presentación.
            visualLayers.forEach { layer ->
                layer.animate().cancel()
                layer.visibility = View.GONE
            }
            reducedMotionControl.visibility = View.GONE
            soundControl.visibility = View.GONE

            frame.alpha = 1f
            frame.visibility = View.VISIBLE
            frame.setBackgroundColor(Color.rgb(0,87,184))
            // SurfaceView necesita un fondo transparente para mostrar su superficie.
            view.setBackgroundColor(Color.TRANSPARENT)
            // Insertar sobre las capas antiguas, manteniendo el póster y Omitir delante.
            frame.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            val poster = ImageView(context).apply {
                setImageResource(R.drawable.alertaec_logo_acerca)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.rgb(0,87,184))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            activePoster = poster
            frame.addView(
                poster,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            skip.visibility = View.VISIBLE
            skip.alpha = 1f
            skip.bringToFront()
            skip.setOnClickListener { stop() }

            // No dejar una pantalla negra si el dispositivo no puede decodificar el MP4.
            val timeout = Runnable { if (activeVideo === view) stop() }
            pendingTimeout = timeout
            handler.postDelayed(timeout, 4_000L)
            view.setOnPreparedListener { player ->
                if (activeVideo === view) {
                    player.isLooping = false
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    // Nueva preferencia: ON por defecto; sin depender del opt-in de sonidos anteriores.
                    val soundEnabled = preferences.getBoolean("cinematic_sound_enabled", true) &&
                        audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                        ((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) > 0)
                    val volume = if (soundEnabled) 1f else 0f
                    // Preparado no significa que haya imagen. No reproducir audio a ciegas.
                    player.setVolume(0f, 0f)
                    player.setOnInfoListener { _, what, _ ->
                        if (activeVideo === view && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            (poster.parent as? ViewGroup)?.removeView(poster)
                            activePoster = null
                            // El límite general se programa desde start(), no desde este aviso.
                            player.setVolume(volume, volume)
                        }
                        false
                    }
                    runCatching { view.start() }
                        .onSuccess {
                            // Sustituir el límite de preparación por uno único de reproducción.
                            pendingTimeout?.let(handler::removeCallbacks)
                            handler.postDelayed(timeout, 16_000L)
                        }
                        .onFailure { stop() }
                }
            }
            view.setOnCompletionListener {
                if (activeVideo === view) {
                    pendingTimeout?.let(handler::removeCallbacks)
                    pendingTimeout = null
                    skip.visibility = View.GONE
                    view.visibility = View.GONE
                    val endingPoster = ImageView(context).apply {
                        setImageResource(R.drawable.alertaec_logo_acerca)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setBackgroundColor(Color.rgb(0, 87, 184))
                    }
                    activePoster = endingPoster
                    frame.addView(endingPoster, FrameLayout.LayoutParams(-1, -1))
                    skip.bringToFront()
                    val finish = Runnable {
                        if (activeVideo === view) {
                            frame.animate().cancel()
                            frame.animate()
                                .alpha(0f)
                                .setDuration(600L)
                                .withEndAction {
                                    if (activeVideo === view) stop()
                                }
                                .start()
                        }
                    }
                    pendingTimeout = finish
                    handler.postDelayed(finish, 3000L)
                }
            }
            view.setOnErrorListener { _, _, _ ->
                if (activeVideo === view) stop()
                true
            }
            view.setVideoURI(
                Uri.parse("android.resource://${context.packageName}/${R.raw.alertaec_cinematic_intro}")
            )
            view.requestFocus()
        } catch (_: RuntimeException) {
            stop()
        }
        // Aunque falle el vídeo, volver a la pantalla principal sin lanzar animaciones antiguas.
        return true
    }

    fun stop() {
        pendingTimeout?.let(handler::removeCallbacks)
        pendingTimeout = null
        activePoster?.let { (it.parent as? ViewGroup)?.removeView(it) }
        activePoster = null
        val view = activeVideo
        activeVideo = null
        val root = activeRoot
        activeRoot = null
        if (view != null) {
            runCatching { view.stopPlayback() }
            (view.parent as? ViewGroup)?.removeView(view)
        }
        root?.let {
            it.animate().cancel()
            it.alpha = 1f
            it.visibility = View.GONE
        }
    }
}
