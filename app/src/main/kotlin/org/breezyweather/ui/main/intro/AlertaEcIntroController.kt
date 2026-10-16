package org.breezyweather.ui.main.intro

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import org.breezyweather.R

object AlertaEcIntroController {

    // Leave enough tail for rain (2.0s) and the closing cue (1.4s at 3.9s).
    private const val INTRO_DURATION = 5800L
    private const val PREFS = "alerta_ec_intro"
    private const val PREF_REDUCED_MOTION = "reduced_motion"
    private const val PREF_SOUND_ENABLED = "sound_enabled"

    // Reading must not persist a default or overwrite an existing user choice.
    internal fun isSoundEnabled(preferences: SharedPreferences): Boolean =
        preferences.getBoolean(PREF_SOUND_ENABLED, false)

    fun stop() = IntroAudioPlayback.stop()

    fun play(
        root: View,
        terrain: View,
        volcano: View,
        wildfire: View,
        waterHazard: View,
        landslide: View,
        storm: View,
        seismicRing: View,
        flash: View,
        condor: View,
        logo: View,
        title: View,
        brand: View,
        skip: View,
        reducedMotionControl: View,
        soundControl: View,
    ) {
        val preferences = root.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val systemAnimatorScale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getFloat(root.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } else {
            1f
        }
        val systemAnimationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
        var reducedMotion = IntroPlaybackPolicy.shouldReduceMotion(
            preferences.getBoolean(PREF_REDUCED_MOTION, false),
            systemAnimatorScale,
            systemAnimationsEnabled
        )
        val layers = listOf(root, terrain, volcano, wildfire, waterHazard, landslide, storm, seismicRing, flash, condor, logo, title, brand)
        val soundCues = listOf(
            IntroAudioPlayback.Cue(0L, R.raw.alertaec_intro_wind),
            IntroAudioPlayback.Cue(250L, R.raw.alertaec_intro_storm),
            IntroAudioPlayback.Cue(300L, R.raw.alertaec_intro_seismic),
            IntroAudioPlayback.Cue(1250L, R.raw.alertaec_intro_volcano),
            IntroAudioPlayback.Cue(3150L, R.raw.alertaec_intro_rain),
            IntroAudioPlayback.Cue(3350L, R.raw.alertaec_intro_landslide),
            IntroAudioPlayback.Cue(3900L, R.raw.alertaec_intro_closing),
        )
        var introStartedAt = 0L
        fun startAudioFromCurrentScene() {
            if (introStartedAt == 0L || reducedMotion) return
            val elapsed = SystemClock.elapsedRealtime() - introStartedAt
            if (elapsed >= INTRO_DURATION) return
            val upcoming = soundCues.filter { it.delayMs > elapsed }
                .map { it.copy(delayMs = it.delayMs - elapsed) }
            if (upcoming.isEmpty()) return
            IntroAudioPlayback.start(
                root.context,
                cues = upcoming,
                userEnabled = isSoundEnabled(preferences)
            )
        }
        var reducedMotionClose: Runnable? = null
        fun updateControls() {
            val motionLabel = root.context.getString(if (reducedMotion) R.string.alertaec_intro_motion_on else R.string.alertaec_intro_motion_off)
            (reducedMotionControl as TextView).apply { text = motionLabel; contentDescription = motionLabel }
            val soundLabel = root.context.getString(
                if (isSoundEnabled(preferences)) R.string.alertaec_intro_sound_enabled
                else R.string.alertaec_intro_sound_disabled
            )
            (soundControl as TextView).apply { text = soundLabel; contentDescription = soundLabel }
        }
        fun showReducedMotion() {
            IntroAudioPlayback.stop()
            layers.forEach { it.animate().cancel() }
            root.visibility = View.VISIBLE
            root.alpha = 1f
            terrain.alpha = 0.92f
            terrain.scaleX = 1f
            terrain.scaleY = 1f
            terrain.translationX = 0f
            terrain.translationY = 0f
            terrain.rotationX = 0f
            volcano.alpha = 0.62f
            wildfire.alpha = 0.55f
            waterHazard.alpha = 0.58f
            landslide.alpha = 0.55f
            storm.alpha = 0.22f
            seismicRing.alpha = 0f
            flash.alpha = 0f
            condor.alpha = 0.85f
            condor.translationX = 0f
            condor.translationY = -70f
            condor.rotation = 0f
            logo.alpha = 1f
            logo.scaleX = 1f
            logo.scaleY = 1f
            title.alpha = 1f
            brand.alpha = 1f
            reducedMotionClose?.let(root::removeCallbacks)
            val closeAction = Runnable { if (root.visibility == View.VISIBLE) root.visibility = View.GONE }
            reducedMotionClose = closeAction
            root.postDelayed(closeAction, 2200L)
        }
        skip.setOnClickListener {
            reducedMotionClose?.let(root::removeCallbacks)
            IntroAudioPlayback.stop()
            layers.forEach { it.animate().cancel() }
            root.visibility = View.GONE
        }
        reducedMotionControl.setOnClickListener {
            val requested = !preferences.getBoolean(PREF_REDUCED_MOTION, false)
            preferences.edit().putBoolean(PREF_REDUCED_MOTION, requested).apply()
            reducedMotion = IntroPlaybackPolicy.shouldReduceMotion(requested, systemAnimatorScale, systemAnimationsEnabled)
            updateControls()
            if (reducedMotion) {
                showReducedMotion()
            } else {
                reducedMotionClose?.let(root::removeCallbacks)
                play(root, terrain, volcano, wildfire, waterHazard, landslide, storm, seismicRing, flash, condor, logo, title, brand, skip, reducedMotionControl, soundControl)
            }
        }
        soundControl.setOnClickListener {
            val enabled = !isSoundEnabled(preferences)
            preferences.edit().putBoolean(PREF_SOUND_ENABLED, enabled).apply()
            if (!enabled) IntroAudioPlayback.stop()
            updateControls()
            if (enabled && root.visibility == View.VISIBLE) startAudioFromCurrentScene()
        }
        updateControls()
        root.visibility = View.VISIBLE
        root.alpha = 1f
        if (reducedMotion) {
            IntroAudioPlayback.stop()
            showReducedMotion()
            return
        }
        reducedMotionClose?.let(root::removeCallbacks)
        introStartedAt = SystemClock.elapsedRealtime()
        startAudioFromCurrentScene()

        terrain.alpha = 0f
        terrain.scaleX = 0.72f
        terrain.scaleY = 0.72f
        terrain.translationY = 65f
        terrain.rotationX = 7f

        seismicRing.alpha = 0f
        seismicRing.scaleX = 0.35f
        seismicRing.scaleY = 0.35f

        volcano.alpha = 0f
        volcano.scaleX = 0.55f
        volcano.scaleY = 0.55f
        volcano.translationY = 45f

        wildfire.alpha = 0f
        wildfire.scaleX = 0.62f
        wildfire.scaleY = 0.62f
        wildfire.translationY = 65f

        waterHazard.alpha = 0f
        waterHazard.scaleX = 0.72f
        waterHazard.scaleY = 0.72f
        waterHazard.translationX = -240f
        waterHazard.translationY = 75f

        landslide.alpha = 0f
        landslide.scaleX = 0.68f
        landslide.scaleY = 0.68f
        landslide.translationX = -110f
        landslide.translationY = 5f
        landslide.rotation = -5f

        storm.alpha = 0f
        storm.scaleX = 0.82f
        storm.scaleY = 0.82f

        condor.alpha = 0f
        condor.translationX = -700f
        condor.translationY = -180f
        condor.scaleX = 0.55f
        condor.scaleY = 0.55f
        condor.rotation = -7f

        logo.alpha = 0f
        logo.scaleX = 0.72f
        logo.scaleY = 0.72f
        title.alpha = 0f
        brand.alpha = 0f

        terrain.animate()
            .alpha(0.92f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .translationY(-18f)
            .rotationX(0f)
            .setStartDelay(100L)
            .setDuration(3200L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        seismicRing.animate()
            .alpha(0.75f)
            .scaleX(1.65f)
            .scaleY(1.65f)
            .setStartDelay(300L)
            .setDuration(900L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                seismicRing.animate().alpha(0f).setDuration(350L).start()
            }.start()

        volcano.animate()
            .alpha(0.88f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .translationY(18f)
            .setStartDelay(1250L)
            .setDuration(850L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                volcano.animate()
                    .alpha(0.32f)
                    .scaleX(1.14f)
                    .scaleY(1.14f)
                    .setDuration(900L)
                    .start()
            }.start()

        wildfire.animate()
            .alpha(0.82f)
            .scaleX(1.06f)
            .scaleY(1.06f)
            .translationY(38f)
            .setStartDelay(2350L)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                wildfire.animate()
                    .alpha(0.18f)
                    .scaleX(1.12f)
                    .scaleY(1.12f)
                    .setDuration(700L)
                    .start()
            }.start()

        waterHazard.animate()
            .alpha(0.84f)
            .translationX(25f)
            .translationY(48f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setStartDelay(3150L)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                waterHazard.animate()
                    .alpha(0.12f)
                    .translationX(110f)
                    .scaleX(1.16f)
                    .scaleY(1.16f)
                    .setDuration(600L)
                    .start()
            }.start()

        landslide.animate()
            .alpha(0.82f)
            .translationX(35f)
            .translationY(65f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .rotation(2f)
            .setStartDelay(3350L)
            .setDuration(500L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                landslide.animate()
                    .alpha(0.08f)
                    .translationX(85f)
                    .translationY(95f)
                    .scaleX(1.14f)
                    .scaleY(1.14f)
                    .setDuration(450L)
                    .start()
            }.start()

        storm.animate()
            .alpha(0.38f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setStartDelay(250L)
            .setDuration(1800L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Keep the flash layer fully transparent to avoid intense flashes.
        flash.alpha = 0f

        condor.animate()
            .alpha(0.92f)
            .translationX(720f)
            .translationY(70f)
            .scaleX(1.18f)
            .scaleY(1.18f)
            .rotation(4f)
            .setStartDelay(550L)
            .setDuration(2800L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                condor.animate().alpha(0f).setDuration(450L).start()
            }.start()

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(3900L)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate().alpha(1f).setStartDelay(4050L).setDuration(500L).start()

        brand.animate().alpha(1f).setStartDelay(4200L).setDuration(250L).start()

        root.animate()
            .alpha(0f)
            .setStartDelay(INTRO_DURATION - 500L)
            .setDuration(500L)
            .withEndAction {
                IntroAudioPlayback.stop()
                root.visibility = View.GONE
            }
            .start()
    }
}
