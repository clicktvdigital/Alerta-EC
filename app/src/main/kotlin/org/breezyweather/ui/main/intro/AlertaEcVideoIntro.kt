package org.breezyweather.ui.main.intro

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.breezyweather.R

/**
 * Reproduce un vídeo local con sonido de forma predeterminada.
 * Respeta el volumen multimedia, el modo silencio y la preferencia de movimiento reducido.
 * No necesita Internet. Nunca toca los controles de volumen del dispositivo.
 */
internal object AlertaEcVideoIntro {
    private const val BLACK = Color.BLACK
    private const val PREPARE_TIMEOUT_MS = 5_000L
    private const val UNKNOWN_DURATION_TIMEOUT_MS = 30_000L
    private const val COMPLETION_FADE_MS = 250L
    private const val COMPLETION_GRACE_MS = 1_000L
    private val handler = Handler(Looper.getMainLooper())
    private var activeVideo: VideoView? = null
    private var activeRoot: FrameLayout? = null
    private var pendingTimeout: Runnable? = null
    private var activePoster: View? = null
    private var previousWindowState: WindowState? = null
    private var previousRootPadding: IntArray? = null

    private data class WindowState(
        val window: android.view.Window,
        val statusBarColor: Int,
        val navigationBarColor: Int,
        val systemUiVisibility: Int,
        val navigationBarContrastEnforced: Boolean?,
    )

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
            frame.setBackgroundColor(BLACK)
            previousRootPadding = intArrayOf(frame.paddingLeft, frame.paddingTop, frame.paddingRight, frame.paddingBottom)
            ViewCompat.setOnApplyWindowInsetsListener(frame) { view, insets ->
                val safeInsets = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(frame)
            (context as? Activity)?.let { activity ->
                val window = activity.window
                previousWindowState = WindowState(
                    window = window,
                    statusBarColor = window.statusBarColor,
                    navigationBarColor = window.navigationBarColor,
                    systemUiVisibility = window.decorView.systemUiVisibility,
                    navigationBarContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced else null,
                )
                window.statusBarColor = BLACK
                window.navigationBarColor = BLACK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
            }
            // SurfaceView necesita un fondo transparente para mostrar su superficie.
            view.setBackgroundColor(BLACK)
            // Insertar sobre las capas antiguas, manteniendo el póster y Omitir delante.
            frame.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply { gravity = Gravity.CENTER },
            )
            val poster = View(context).apply {
                setBackgroundColor(BLACK)
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
            handler.postDelayed(timeout, PREPARE_TIMEOUT_MS)
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
                            val duration = player.duration.takeIf { it > 0 }?.toLong()
                            handler.postDelayed(timeout, (duration ?: UNKNOWN_DURATION_TIMEOUT_MS) + COMPLETION_GRACE_MS)
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
                    val endingPoster = View(context).apply {
                        setBackgroundColor(BLACK)
                    }
                    activePoster = endingPoster
                    frame.addView(endingPoster, FrameLayout.LayoutParams(-1, -1))
                    skip.bringToFront()
                    val finish = Runnable {
                        if (activeVideo === view) {
                            frame.animate().cancel()
                            frame.animate()
                                .alpha(0f)
                                .setDuration(COMPLETION_FADE_MS)
                                .withEndAction {
                                    if (activeVideo === view) stop()
                                }
                                .start()
                        }
                    }
                    pendingTimeout = finish
                    handler.post(finish)
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
        root?.let { frame ->
            ViewCompat.setOnApplyWindowInsetsListener(frame, null)
            previousRootPadding?.let { padding ->
                frame.setPadding(padding[0], padding[1], padding[2], padding[3])
            }
            previousRootPadding = null
        }
        previousWindowState?.let { state ->
            state.window.statusBarColor = state.statusBarColor
            state.window.navigationBarColor = state.navigationBarColor
            state.window.decorView.systemUiVisibility = state.systemUiVisibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && state.navigationBarContrastEnforced != null) {
                state.window.isNavigationBarContrastEnforced = state.navigationBarContrastEnforced
            }
        }
        previousWindowState = null
        root?.let {
            it.animate().cancel()
            it.alpha = 1f
            it.visibility = View.GONE
        }
    }
}
