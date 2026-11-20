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

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.breezyweather.R
import org.breezyweather.ui.support.isTelevisionDevice

/**
 * Reproduce un vídeo local con sonido de forma predeterminada.
 * Respeta el volumen multimedia, el modo silencio y la preferencia de movimiento reducido.
 * No necesita Internet. Nunca toca los controles de volumen del dispositivo.
 */
internal object AlertaEcVideoIntro {
    private const val BLACK = Color.BLACK
    private const val PREPARE_TIMEOUT_MS = 12_000L
    private const val UNKNOWN_DURATION_TIMEOUT_MS = 30_000L
    private const val COMPLETION_FADE_MS = 250L
    private const val COMPLETION_GRACE_MS = 1_000L
    /**
     * The start image stays on screen this long before the video begins.
     * Was 5 s: that fixed hold was most of the 11 s between "intro play" and "intro video start".
     */
    private const val START_IMAGE_MS = 1_500L
    /**
     * Logos horizontales Full HD del intro de TV, reemplazables sin tocar código:
     *  - Inicio: app/src/main/res/drawable-nodpi/alertaec_tv_intro_start.jpg
     *  - Cierre: app/src/main/res/drawable-nodpi/alertaec_tv_intro_end.jpg
     * Conservar el mismo nombre base. Si cambia la extensión (p. ej. .png), borrar el archivo anterior.
     */
    private val TV_START_LOGO = R.drawable.alertaec_tv_intro_start
    private val TV_END_LOGO = R.drawable.alertaec_tv_intro_end
    /** The end image stays on screen this long after the video ends. */
    private const val END_IMAGE_MS = 1_200L
    private const val AUDIO_FALLBACK_MS = 1_500L
    private val handler = Handler(Looper.getMainLooper())
    private var activeVideo: IntroVideoView? = null

    fun isActive(): Boolean = activeVideo != null
    private var activeRoot: FrameLayout? = null
    private var pendingTimeout: Runnable? = null
    private var pendingAudioFallback: Runnable? = null
    private var previousWindowState: WindowState? = null
    private var previousRootPadding: IntArray? = null
    /** Fondo original del contenedor; se restaura al terminar para soltar el logo de TV de memoria. */
    private var previousRootBackground: android.graphics.drawable.Drawable? = null

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
        val isTelevision = context.isTelevisionDevice()
        val useTvVisuals = isTelevision ||
            context.resources.configuration.smallestScreenWidthDp >= 600

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
        android.util.Log.i("AlertaEcStartup", "intro play: tv=$useTvVisuals reduceMotion=$reduceMotion")
        stop()
        if (reduceMotion) {
            frame.visibility = View.GONE
            return true
        }

        val view = IntroVideoView(context)
        // Must stay VISIBLE: a SurfaceView only creates its surface while visible, and VideoView
        // does not open (prepare) the video without a surface. An INVISIBLE VideoView never fired
        // onPrepared, so the intro always timed out on TV. With media-overlay z-order and no frame
        // posted yet, the surface is transparent, so the start image still shows until playback.
        view.visibility = View.VISIBLE
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
            previousRootBackground = frame.background
            if (useTvVisuals) {
                frame.setBackgroundResource(TV_START_LOGO)
            } else {
                frame.setBackgroundColor(BLACK)
            }
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
            // TextureView is transparent by itself (isOpaque = false). It must NOT get a background:
            // TextureView throws UnsupportedOperationException on setBackgroundColor, which aborted the intro.
            // Insertar sobre las capas antiguas, manteniendo Omitir delante.
            frame.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply { gravity = Gravity.CENTER },
            )
            skip.visibility = View.VISIBLE
            skip.alpha = 1f
            skip.bringToFront()
            // Visible focus ring so the remote shows where OK will press "Omitir".
            skip.isFocusable = true
            skip.foreground = android.graphics.drawable.GradientDrawable().apply {
                val density = context.resources.displayMetrics.density
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke((3 * density).toInt(), android.graphics.Color.WHITE)
                cornerRadius = 12 * density
            }
            skip.setOnClickListener { stop() }

            // No dejar una pantalla negra si el dispositivo no puede decodificar el MP4.
            val timeout = Runnable { if (activeVideo === view) stop() }
            scheduleTimeout(timeout, PREPARE_TIMEOUT_MS)
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
                    var renderingStarted = false
                    val audioFallback = Runnable {
                        if (activeVideo === view && !renderingStarted && soundEnabled) {
                            player.setVolume(volume, volume)
                        }
                    }
                    player.setOnInfoListener { _, what, _ ->
                        if (activeVideo === view && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            renderingStarted = true
                            // First real frame on screen: drop the cover image without a black flash.
                            frame.setBackgroundColor(BLACK)
                            cancelAudioFallback()
                            player.setVolume(volume, volume)
                        }
                        false
                    }
                    // Hold the start image, then reveal and start the video.
                    cancelTimeout()
                    handler.postDelayed({
                        if (activeVideo !== view) return@postDelayed
                        // La portada se quita al recibir el primer cuadro (MEDIA_INFO_VIDEO_RENDERING_START).
                        view.bringToFront()
                        skip.bringToFront()
                        android.util.Log.i("AlertaEcStartup", "intro video start")
                        runCatching { view.start() }
                            .onSuccess {
                                // Sustituir el límite de preparación por uno único de reproducción.
                                val duration = player.duration.takeIf { it > 0 }?.toLong()
                                scheduleTimeout(
                                    timeout,
                                    (duration ?: UNKNOWN_DURATION_TIMEOUT_MS) + COMPLETION_GRACE_MS,
                                )
                                if (soundEnabled) {
                                    pendingAudioFallback = audioFallback
                                    handler.postDelayed(audioFallback, AUDIO_FALLBACK_MS)
                                }
                            }
                            .onFailure { stop() }
                    }, START_IMAGE_MS)
                }
            }
            view.setOnCompletionListener {
                if (activeVideo === view) {
                    cancelTimeout()
                    cancelAudioFallback()
                    skip.visibility = View.GONE
                    // Cierre: logo fijo breve y fundido rápido hacia la pantalla principal (sin negro largo).
                    if (useTvVisuals) frame.setBackgroundResource(TV_END_LOGO)
                    view.visibility = View.GONE
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
                    scheduleTimeout(finish, if (useTvVisuals) END_IMAGE_MS else 0L)
                }
            }
            view.setOnErrorListener { _, _, _ ->
                if (activeVideo === view) stop()
                true
            }
            view.setVideoURI(
                Uri.parse("android.resource://${context.packageName}/${introVideoResource(context, useTvVisuals)}")
            )
            // El foco va a "Omitir", no al video: así OK funciona con el primer pulso (TV y móvil).
            skip.requestFocus()
        } catch (e: RuntimeException) {
            // Silent before: the intro vanished with no trace. Log the cause so the TV log shows it.
            android.util.Log.e("AlertaEcStartup", "intro setup failed: ${e.javaClass.name}: ${e.message}", e)
            stop()
        }
        // Aunque falle el vídeo, volver a la pantalla principal sin lanzar animaciones antiguas.
        return true
    }

    fun stop() {
        android.util.Log.i("AlertaEcStartup", "intro stop: active=${activeVideo != null}")
        cancelTimeout()
        cancelAudioFallback()
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
            // Soltar el logo de TV (bitmap a pantalla completa) en cuanto termina el intro.
            frame.background = previousRootBackground
            previousRootBackground = null
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
            restoreTelevisionFocus(it)
        }
    }

    private fun restoreTelevisionFocus(frame: View) {
        val activity = frame.context as? Activity ?: return
        if (!activity.isTelevisionDevice()) return

        // Las tarjetas pueden terminar de cargarse despues del video.
        // Reintentar brevemente sin bloquear la pantalla principal.
        fun attemptFocus(remaining: Int) {
            frame.post {
                if (activity.isFinishing || activity.isDestroyed) return@post

                if (activity.hasWindowFocus()) {
                    for (areaId in listOf(R.id.fragment_drawer, R.id.fragment_home)) {
                        val area = activity.findViewById<View>(areaId) ?: continue
                        if (!area.isShown) continue
                        val target = area.getFocusables(View.FOCUS_FORWARD).firstOrNull {
                            it.isShown && it.isEnabled &&
                                it.isFocusable && it.hasOnClickListeners()
                        }
                        if (target?.requestFocus() == true) return@post
                    }
                }

                if (remaining > 1) {
                    frame.postDelayed({ attemptFocus(remaining - 1) }, 250L)
                }
            }
        }

        attemptFocus(10)
    }

    private fun scheduleTimeout(timeout: Runnable, delayMs: Long) {
        cancelTimeout()
        pendingTimeout = timeout
        handler.postDelayed(timeout, delayMs)
    }

    private fun cancelTimeout() {
        pendingTimeout?.let(handler::removeCallbacks)
        pendingTimeout = null
    }

    private fun cancelAudioFallback() {
        pendingAudioFallback?.let(handler::removeCallbacks)
        pendingAudioFallback = null
    }

    /** Raw video for this device profile (see [IntroPlaybackPolicy.videoProfile]). */
    private fun introVideoResource(context: Context, useTvVisuals: Boolean): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memory = android.app.ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val profile = IntroPlaybackPolicy.videoProfile(
            tvVisuals = useTvVisuals,
            totalMemBytes = memory.totalMem,
            isLowRamDevice = activityManager?.isLowRamDevice ?: true,
        )
        android.util.Log.i("AlertaEcStartup", "intro profile=$profile totalMem=${memory.totalMem}")
        return when (profile) {
            IntroVideoProfile.TV_FULL_HD -> R.raw.alertaec_cinematic_intro_tv_fhd
            IntroVideoProfile.TV_BOX_HD -> R.raw.alertaec_cinematic_intro_tv
            IntroVideoProfile.PHONE_VERTICAL -> R.raw.alertaec_cinematic_intro
        }
    }
}
