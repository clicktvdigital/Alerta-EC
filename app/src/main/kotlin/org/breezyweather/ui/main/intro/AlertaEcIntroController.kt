package org.breezyweather.ui.main.intro

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object AlertaEcIntroController {

    private const val INTRO_DURATION = 5000L

    fun play(root: View, terrain: View, volcano: View, wildfire: View, waterHazard: View, landslide: View, storm: View, seismicRing: View, flash: View, condor: View, logo: View, title: View, brand: View) {
        root.visibility = View.VISIBLE
        root.alpha = 1f

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
            .setStartDelay(3700L)
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

        flash.alpha = 0f
        flash.animate()
            .alpha(0.42f)
            .setStartDelay(850L)
            .setDuration(70L)
            .withEndAction {
                flash.animate().alpha(0f).setDuration(90L).withEndAction {
                    flash.animate().alpha(0.68f).setDuration(55L).withEndAction {
                        flash.animate().alpha(0f).setDuration(180L).start()
                    }.start()
                }.start()
            }.start()

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
            .setStartDelay(3650L)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate().alpha(1f).setStartDelay(3850L).setDuration(500L).start()

        brand.animate().alpha(1f).setStartDelay(4050L).setDuration(400L).start()

        root.animate()
            .alpha(0f)
            .setStartDelay(INTRO_DURATION - 500L)
            .setDuration(500L)
            .withEndAction { root.visibility = View.GONE }
            .start()
    }
}
