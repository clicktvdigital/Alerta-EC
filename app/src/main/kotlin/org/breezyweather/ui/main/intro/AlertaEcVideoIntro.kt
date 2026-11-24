package org.breezyweather.ui.main.intro

import android.animation.ValueAnimator
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.breezyweather.R

internal object AlertaEcVideoIntro {
    private const val BLACK = Color.BLACK
    private const val BRAND_BG = 0xFF0A244A.toInt()
    private const val LOGO_HOLD_MS = 1_300L
    private const val PREPARE_TIMEOUT_MS = 5_000L
    private const val UNKNOWN_DURATION_TIMEOUT_MS = 30_000L
    private const val COMPLETION_FADE_MS = 280L
    private const val COMPLETION_GRACE_MS = 1_000L
    private const val AUDIO_FALLBACK_MS = 1_500L

    private val handler = Handler(Looper.getMainLooper())
    private var activeVideo: VideoView? = null
    private var activeRoot: FrameLayout? = null
    private var activeSplash: ImageView? = null
    private var pendingTimeout: Runnable? = null
    private var pendingAudioFallback: Runnable? = null
    private var previousWindowState: WindowState? = null
    private var previousRootPadding: IntArray? = null

    private data class WindowState(
        val window: android.view.Window,
        val statusBarColor: Int,
        val navigationBarColor: Int,
        val systemUiVisibility: Int,
        val navigationBarContrastEnforced: Boolean?,
    )

    private fun isPanoramicTvOrLandscape(context: Context): Boolean {
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return isTv || isLandscape
    }

    /**
     * Logo de apertura y cierre: banner panorámico Full HD 16:9 en Android TV/horizontal, y logo
     * vertical en celular. FIT_CENTER nunca recorta: el arte (alas del cóndor incluidas) cabe completo.
     */
    private fun createBrandSplashView(context: Context, panoramic: Boolean): ImageView {
        return ImageView(context).apply {
            setBackgroundColor(BRAND_BG)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
            if (panoramic) {
                setImageResource(R.drawable.tv_banner)
            } else {
                setImageResource(R.drawable.alerta_ec_intro_logo)
                // Margen vertical equilibrado (arriba = abajo) para el logo vertical.
                val margin = (context.resources.displayMetrics.heightPixels * 0.04f).toInt()
                setPadding(0, margin, 0, margin)
            }
        }
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
            frame.visibility = View.GONE
            return true
        }

        val panoramic = isPanoramicTvOrLandscape(context)
        val view = VideoView(context)
        val splashView = createBrandSplashView(context, panoramic)
        activeVideo = view
        activeRoot = frame
        activeSplash = splashView

        try {
            visualLayers.forEach { layer ->
                layer.animate().cancel()
                layer.visibility = View.GONE
            }
            reducedMotionControl.visibility = View.GONE
            soundControl.visibility = View.GONE

            frame.alpha = 1f
            frame.visibility = View.VISIBLE
            frame.setBackgroundColor(BRAND_BG)
            previousRootPadding = intArrayOf(frame.paddingLeft, frame.paddingTop, frame.paddingRight, frame.paddingBottom)
            ViewCompat.setOnApplyWindowInsetsListener(frame) { v, insets ->
                val safeInsets = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
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
                window.statusBarColor = BRAND_BG
                window.navigationBarColor = BRAND_BG
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
            }

            view.setBackgroundColor(Color.TRANSPARENT)
            view.visibility = View.INVISIBLE
            frame.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply { gravity = Gravity.CENTER },
            )
            // Fase 1: Imagen inicial (vertical en celular / horizontal panoramica en Android TV)
            splashView.alpha = 1f
            splashView.visibility = View.VISIBLE
            frame.addView(
                splashView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply { gravity = Gravity.CENTER },
            )

            skip.visibility = View.VISIBLE
            skip.alpha = 1f
            skip.bringToFront()
            skip.setOnClickListener { stop() }

            var videoPrepared = false
            var initialHoldElapsed = false
            var playbackStarted = false

            fun startVideoWhenReady() {
                if (activeVideo !== view || playbackStarted || !videoPrepared || !initialHoldElapsed) return
                playbackStarted = true
                frame.setBackgroundColor(BLACK)
                view.visibility = View.VISIBLE
                splashView.animate()
                    .alpha(0f)
                    .setDuration(220L)
                    .withEndAction {
                        if (activeSplash === splashView) splashView.visibility = View.GONE
                    }
                    .start()
                skip.bringToFront()
                runCatching { view.start() }
                    .onFailure { stop() }
            }

            val holdRunnable = Runnable {
                if (activeVideo === view) {
                    initialHoldElapsed = true
                    startVideoWhenReady()
                }
            }
            handler.postDelayed(holdRunnable, LOGO_HOLD_MS)

            val timeout = Runnable { if (activeVideo === view) stop() }
            scheduleTimeout(timeout, PREPARE_TIMEOUT_MS + LOGO_HOLD_MS)

            view.setOnPreparedListener { player ->
                if (activeVideo === view) {
                    player.isLooping = false
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val soundEnabled = preferences.getBoolean("cinematic_sound_enabled", true) &&
                        audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                        ((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) > 0)
                    val volume = if (soundEnabled) 1f else 0f
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
                            cancelAudioFallback()
                            player.setVolume(volume, volume)
                        }
                        false
                    }
                    val duration = player.duration.takeIf { it > 0 }?.toLong()
                    scheduleTimeout(
                        timeout,
                        LOGO_HOLD_MS + (duration ?: UNKNOWN_DURATION_TIMEOUT_MS) + COMPLETION_GRACE_MS + LOGO_HOLD_MS,
                    )
                    if (soundEnabled) {
                        pendingAudioFallback = audioFallback
                        handler.postDelayed(audioFallback, LOGO_HOLD_MS + AUDIO_FALLBACK_MS)
                    }
                    videoPrepared = true
                    startVideoWhenReady()
                }
            }

            // Fase 3: Imagen final de cierre antes de ingresar a la aplicacion
            view.setOnCompletionListener {
                if (activeVideo === view) {
                    cancelTimeout()
                    cancelAudioFallback()
                    skip.visibility = View.GONE
                    view.visibility = View.GONE
                    frame.setBackgroundColor(BRAND_BG)
                    splashView.visibility = View.VISIBLE
                    splashView.alpha = 0f
                    splashView.bringToFront()
                    splashView.animate()
                        .alpha(1f)
                        .setDuration(220L)
                        .start()

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
                    scheduleTimeout(finish, LOGO_HOLD_MS)
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
        return true
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
        val video = activeVideo
        val root = activeRoot
        val splash = activeSplash
        activeVideo = null
        activeRoot = null
        activeSplash = null
        runCatching { video?.stopPlayback() }
        if (root != null) {
            if (video != null) root.removeView(video)
            if (splash != null) root.removeView(splash)
            previousRootPadding?.let { p ->
                if (p.size == 4) root.setPadding(p[0], p[1], p[2], p[3])
            }
            previousRootPadding = null
            ViewCompat.setOnApplyWindowInsetsListener(root, null)
            root.alpha = 1f
            root.visibility = View.GONE
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
    }
}
