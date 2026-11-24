package org.breezyweather.ui.main.intro

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 * Intro común a los 3 perfiles: logo inicial Full HD (1300 ms) → video → logo final Full HD (1300 ms) → Home.
 * - Android TV: logo 1920×1080 + video horizontal 1920×1080.
 * - TV Box (o TV con pocos recursos): logo 1920×1080 + video horizontal 1280×720.
 * - Celular: logo vertical 1080×1920 + video vertical 720×1280.
 * Ver [IntroPlaybackPolicy] para la detección del perfil y el respaldo si falta un recurso.
 *
 * Logs con la etiqueta [TAG] mediante Log.println: R8 elimina Log.v/d/i/w/e en release (proguard-rules.pro).
 */
internal object AlertaEcVideoIntro {
    private const val TAG = "AlertaEcIntro"
    private const val BRAND_BG = 0xFF0A244A.toInt()
    private const val LOGO_HOLD_MS = 1_300L
    private const val LOGO_FADE_MS = 220L
    private const val PREPARE_TIMEOUT_MS = 8_000L
    private const val UNKNOWN_DURATION_TIMEOUT_MS = 30_000L
    private const val COMPLETION_FADE_MS = 280L
    private const val COMPLETION_GRACE_MS = 1_500L
    private const val AUDIO_FALLBACK_MS = 1_500L

    private enum class Phase { INITIAL_LOGO, VIDEO, FINAL_LOGO }

    private val handler = Handler(Looper.getMainLooper())
    private var activeVideo: VideoView? = null
    private var activeRoot: FrameLayout? = null
    private var activeSplash: ImageView? = null
    private var activeBackdrop: ImageView? = null
    private var activeSkip: View? = null
    private var pendingTimeout: Runnable? = null
    private var pendingHold: Runnable? = null
    private var pendingAudioFallback: Runnable? = null
    private var previousWindowState: WindowState? = null
    private var previousSkipMargins: IntArray? = null

    private var phase = Phase.INITIAL_LOGO
    private var videoPrepared = false
    private var initialHoldElapsed = false
    private var playbackStarted = false
    private var paused = false
    private var videoDurationMs: Long? = null

    private data class WindowState(
        val window: android.view.Window,
        val statusBarColor: Int,
        val navigationBarColor: Int,
        val navigationBarContrastEnforced: Boolean?,
    )

    private fun log(message: String) {
        Log.println(Log.INFO, TAG, message)
    }

    private fun detectProfile(context: Context): IntroDeviceProfile {
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val metrics = context.resources.displayMetrics
        val longSide = maxOf(metrics.widthPixels, metrics.heightPixels)
        // Solo se consulta el decodificador en televisores; en celular no hace falta.
        val supportsFhd = !isTv || decoderSupportsFhd()
        val profile = IntroPlaybackPolicy.deviceProfile(
            isTelevision = isTv,
            isLandscape = isLandscape,
            lowRamDevice = activityManager?.isLowRamDevice == true,
            totalMemoryBytes = memory.totalMem,
            displayLongSidePx = longSide,
            decoderSupportsFhd = supportsFhd,
        )
        log(
            "Perfil $profile (tv=$isTv, horizontal=$isLandscape, ram=${memory.totalMem / 1_000_000} MB, " +
                "pantalla=${metrics.widthPixels}x${metrics.heightPixels}, h264_1080p=$supportsFhd)"
        )
        return profile
    }

    private fun decoderSupportsFhd(): Boolean = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals("video/avc", ignoreCase = true) } &&
                info.getCapabilitiesForType("video/avc").videoCapabilities
                    ?.areSizeAndRateSupported(1920, 1080, 24.0) == true
        }
    }.getOrDefault(true)

    @android.annotation.SuppressLint("DiscouragedApi")
    private fun resolve(context: Context, names: List<String>, type: String): Pair<String, Int>? =
        IntroPlaybackPolicy.firstAvailable(names) { context.resources.getIdentifier(it, type, context.packageName) }

    /**
     * Logo de apertura y cierre. FIT_CENTER sin padding: el arte (alas del cóndor incluidas) nunca se recorta.
     * El espacio sobrante no queda negro: lo cubre [createBackdropView].
     */
    private fun createBrandSplashView(context: Context, logo: Int): ImageView =
        ImageView(context).apply {
            setImageResource(logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
            setBackgroundColor(Color.TRANSPARENT)
        }

    /**
     * Fondo a pantalla completa con el mismo arte ampliado, oscurecido (y desenfocado en Android 12+).
     * Rellena las franjas del logo vertical y del video 9:16 en vez de dejarlas negras.
     */
    private fun createBackdropView(context: Context, logo: Int): ImageView =
        ImageView(context).apply {
            setBackgroundColor(BRAND_BG)
            setImageResource(logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setColorFilter(0x99061630.toInt(), android.graphics.PorterDuff.Mode.SRC_ATOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(48f, 48f, Shader.TileMode.CLAMP))
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

    fun play(
        root: View,
        visualLayers: List<View>,
        skip: View,
        reducedMotionControl: View,
        soundControl: View,
    ): Boolean {
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
            log("Intro omitida: movimiento reducido activo")
            frame.visibility = View.GONE
            return true
        }

        val profile = detectProfile(context)
        val logo = resolve(context, IntroPlaybackPolicy.logoCandidates(profile), "drawable")
        val video = resolve(context, IntroPlaybackPolicy.videoCandidates(profile), "raw")
        if (logo == null || video == null) {
            log("Intro omitida: falta recurso (logo=${logo?.first}, video=${video?.first})")
            frame.visibility = View.GONE
            return true
        }
        log("Recursos: logo=${logo.first}, video=${video.first}")
        val view = VideoView(context)
        val backdrop = createBackdropView(context, logo.second)
        val splashView = createBrandSplashView(context, logo.second)
        activeVideo = view
        activeRoot = frame
        activeSplash = splashView
        activeBackdrop = backdrop
        activeSkip = skip
        phase = Phase.INITIAL_LOGO
        videoPrepared = false
        initialHoldElapsed = false
        playbackStarted = false
        paused = false
        videoDurationMs = null

        try {
            visualLayers.forEach { layer ->
                layer.animate().cancel()
                layer.visibility = View.GONE
            }
            reducedMotionControl.visibility = View.GONE
            soundControl.visibility = View.GONE

            frame.alpha = 1f
            frame.visibility = View.VISIBLE
            // Nunca negro: el color institucional queda debajo de todo.
            frame.setBackgroundColor(BRAND_BG)

            // La app es edge-to-edge: los insets solo desplazan el botón «Omitir», no encogen el logo
            // ni el video (antes el padding del contenedor generaba franjas arriba y abajo).
            (skip.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                previousSkipMargins = intArrayOf(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
            }
            ViewCompat.setOnApplyWindowInsetsListener(frame) { _, insets ->
                val safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                previousSkipMargins?.let { base ->
                    skip.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        setMargins(base[0] + safe.left, base[1] + safe.top, base[2] + safe.right, base[3] + safe.bottom)
                    }
                }
                insets
            }
            ViewCompat.requestApplyInsets(frame)

            (context as? Activity)?.let { activity ->
                val window = activity.window
                previousWindowState = WindowState(
                    window = window,
                    statusBarColor = window.statusBarColor,
                    navigationBarColor = window.navigationBarColor,
                    navigationBarContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced else null,
                )
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
            }

            val fullScreen = {
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply { gravity = Gravity.CENTER }
            }
            frame.addView(backdrop, fullScreen())
            // El VideoView debe estar VISIBLE desde el inicio: un SurfaceView invisible no crea su superficie
            // y el video nunca se prepara (la intro terminaba por timeout sin video ni logo final).
            // Queda tapado por el logo opaco hasta que arranca la reproducción.
            view.setBackgroundColor(Color.TRANSPARENT)
            view.visibility = View.VISIBLE
            frame.addView(view, fullScreen())
            splashView.alpha = 1f
            splashView.visibility = View.VISIBLE
            frame.addView(splashView, fullScreen())

            skip.visibility = View.VISIBLE
            skip.alpha = 1f
            skip.bringToFront()
            skip.setOnClickListener {
                log("Intro omitida por el usuario")
                stop()
            }

            log("Etapa 1: logo inicial Full HD (${logo.first}) durante $LOGO_HOLD_MS ms")
            scheduleHold(LOGO_HOLD_MS)
            scheduleTimeout(
                Runnable {
                    if (activeVideo === view) {
                        log("Error: el video no se preparó a tiempo; se muestra el logo final")
                        showFinalLogo(view)
                    }
                },
                LOGO_HOLD_MS + PREPARE_TIMEOUT_MS,
            )

            view.setOnPreparedListener { player ->
                if (activeVideo !== view || videoPrepared) return@setOnPreparedListener
                player.isLooping = false
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val soundEnabled = preferences.getBoolean("cinematic_sound_enabled", true) &&
                    audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                    ((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) > 0)
                val volume = if (soundEnabled) 1f else 0f
                player.setVolume(0f, 0f)
                var renderingStarted = false
                player.setOnInfoListener { _, what, _ ->
                    if (activeVideo === view && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        renderingStarted = true
                        cancelAudioFallback()
                        player.setVolume(volume, volume)
                        log("Video: primer fotograma en pantalla")
                    }
                    false
                }
                if (soundEnabled) {
                    val audioFallback = Runnable {
                        if (activeVideo === view && !renderingStarted) player.setVolume(volume, volume)
                    }
                    pendingAudioFallback = audioFallback
                    handler.postDelayed(audioFallback, LOGO_HOLD_MS + AUDIO_FALLBACK_MS)
                }
                videoDurationMs = player.duration.takeIf { it > 0 }?.toLong()
                videoPrepared = true
                log("Video preparado (${player.videoWidth}x${player.videoHeight}, ${videoDurationMs ?: "?"} ms, sonido=$soundEnabled)")
                startVideoWhenReady()
            }

            view.setOnCompletionListener {
                if (activeVideo === view) {
                    log("Video: fin de reproducción")
                    showFinalLogo(view)
                }
            }

            view.setOnErrorListener { _, what, extra ->
                if (activeVideo === view) {
                    log("Error de video (what=$what, extra=$extra); se muestra el logo final")
                    showFinalLogo(view)
                }
                true
            }
            view.setVideoURI(
                Uri.parse("android.resource://${context.packageName}/${video.second}")
            )
            view.requestFocus()
        } catch (e: RuntimeException) {
            log("Error al iniciar la intro: ${e.javaClass.simpleName}: ${e.message}")
            stop()
        }
        return true
    }

    private fun scheduleHold(delayMs: Long) {
        pendingHold?.let(handler::removeCallbacks)
        val hold = Runnable {
            pendingHold = null
            if (activeVideo != null && !paused) {
                initialHoldElapsed = true
                startVideoWhenReady()
            }
        }
        pendingHold = hold
        handler.postDelayed(hold, delayMs)
    }

    private fun startVideoWhenReady() {
        val view = activeVideo ?: return
        val splash = activeSplash ?: return
        if (paused || playbackStarted || !videoPrepared || !initialHoldElapsed || phase != Phase.INITIAL_LOGO) return
        playbackStarted = true
        phase = Phase.VIDEO
        log("Etapa 2: inicio del video")
        splash.animate()
            .alpha(0f)
            .setDuration(LOGO_FADE_MS)
            .withEndAction { if (activeSplash === splash && phase == Phase.VIDEO) splash.visibility = View.GONE }
            .start()
        activeSkip?.bringToFront()
        runCatching { view.start() }
            .onSuccess { scheduleVideoTimeout(view, 0L) }
            .onFailure {
                log("Error al reproducir el video: ${it.message}; se muestra el logo final")
                showFinalLogo(view)
            }
    }

    /** Red de seguridad por si Android no entrega onCompletion (decodificadores de TV). */
    private fun scheduleVideoTimeout(view: VideoView, positionMs: Long) {
        val remaining = (videoDurationMs ?: UNKNOWN_DURATION_TIMEOUT_MS) - positionMs
        scheduleTimeout(
            Runnable {
                if (activeVideo === view && phase == Phase.VIDEO) {
                    log("Video sin evento de fin a tiempo; se muestra el logo final")
                    showFinalLogo(view)
                }
            },
            remaining.coerceAtLeast(0L) + COMPLETION_GRACE_MS,
        )
    }

    /** Etapa 3: logo final (vertical o banner TV) durante [LOGO_HOLD_MS] y cierre. */
    private fun showFinalLogo(view: VideoView) {
        if (activeVideo !== view || phase == Phase.FINAL_LOGO) return
        phase = Phase.FINAL_LOGO
        cancelTimeout()
        cancelAudioFallback()
        pendingHold?.let(handler::removeCallbacks)
        pendingHold = null
        runCatching { view.stopPlayback() }
        view.visibility = View.GONE
        activeSkip?.visibility = View.GONE
        val splash = activeSplash ?: return stop()
        log("Etapa 3: logo final durante $LOGO_HOLD_MS ms")
        splash.animate().cancel()
        splash.visibility = View.VISIBLE
        splash.alpha = 0f
        splash.bringToFront()
        splash.animate().alpha(1f).setDuration(LOGO_FADE_MS).start()
        scheduleTimeout(Runnable { finish(view) }, LOGO_FADE_MS + LOGO_HOLD_MS)
    }

    private fun finish(view: VideoView) {
        val frame = activeRoot ?: return
        if (activeVideo !== view) return
        log("Cierre de la intro: paso al Home")
        frame.animate().cancel()
        frame.animate()
            .alpha(0f)
            .setDuration(COMPLETION_FADE_MS)
            .withEndAction { if (activeVideo === view) stop() }
            .start()
    }

    /** Pausa por un diálogo (p. ej. permisos) sin destruir la intro; antes onPause la cerraba a medias. */
    fun pause() {
        val view = activeVideo ?: return
        if (paused) return
        paused = true
        log("Intro en pausa (fase $phase)")
        pendingHold?.let(handler::removeCallbacks)
        pendingHold = null
        when (phase) {
            Phase.VIDEO -> {
                cancelTimeout()
                cancelAudioFallback()
                runCatching { view.pause() }
            }
            Phase.INITIAL_LOGO -> cancelTimeout()
            Phase.FINAL_LOGO -> Unit // el cierre programado sigue su curso
        }
    }

    fun resume() {
        val view = activeVideo ?: return
        if (!paused) return
        paused = false
        log("Intro reanudada (fase $phase)")
        when (phase) {
            Phase.INITIAL_LOGO -> {
                scheduleTimeout(
                    Runnable {
                        if (activeVideo === view && phase == Phase.INITIAL_LOGO) {
                            log("Error: el video no se preparó tras reanudar; se muestra el logo final")
                            showFinalLogo(view)
                        }
                    },
                    LOGO_HOLD_MS + PREPARE_TIMEOUT_MS,
                )
                if (initialHoldElapsed) startVideoWhenReady() else scheduleHold(LOGO_HOLD_MS)
            }
            Phase.VIDEO -> runCatching { view.start() }
                .onSuccess { scheduleVideoTimeout(view, view.currentPosition.toLong()) }
                .onFailure { showFinalLogo(view) }
            Phase.FINAL_LOGO -> Unit
        }
    }

    private fun scheduleTimeout(runnable: Runnable, delayMs: Long) {
        cancelTimeout()
        pendingTimeout = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun cancelTimeout() {
        pendingTimeout?.let(handler::removeCallbacks)
        pendingTimeout = null
    }

    private fun cancelAudioFallback() {
        pendingAudioFallback?.let(handler::removeCallbacks)
        pendingAudioFallback = null
    }

    fun stop() {
        cancelTimeout()
        cancelAudioFallback()
        pendingHold?.let(handler::removeCallbacks)
        pendingHold = null
        val video = activeVideo
        val root = activeRoot
        val splash = activeSplash
        val backdrop = activeBackdrop
        val skip = activeSkip
        activeVideo = null
        activeRoot = null
        activeSplash = null
        activeBackdrop = null
        activeSkip = null
        paused = false
        if (video != null) log("Intro detenida y retirada (fase $phase)")
        runCatching { video?.stopPlayback() }
        if (root != null) {
            if (video != null) root.removeView(video)
            if (splash != null) root.removeView(splash)
            if (backdrop != null) root.removeView(backdrop)
            ViewCompat.setOnApplyWindowInsetsListener(root, null)
            root.alpha = 1f
            root.visibility = View.GONE
        }
        previousSkipMargins?.let { base ->
            skip?.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(base[0], base[1], base[2], base[3]) }
        }
        previousSkipMargins = null
        previousWindowState?.let { state ->
            state.window.statusBarColor = state.statusBarColor
            state.window.navigationBarColor = state.navigationBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && state.navigationBarContrastEnforced != null) {
                state.window.isNavigationBarContrastEnforced = state.navigationBarContrastEnforced
            }
        }
        previousWindowState = null
    }
}
