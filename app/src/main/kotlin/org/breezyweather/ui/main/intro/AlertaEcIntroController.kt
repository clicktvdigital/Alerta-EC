package org.breezyweather.ui.main.intro

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object AlertaEcIntroController {

    private const val INTRO_DURATION = 5000L

    fun play(root: View, storm: View, seismicRing: View, flash: View, condor: View, logo: View, title: View, brand: View) {
        root.visibility = View.VISIBLE
        root.alpha = 1f

        seismicRing.alpha = 0f
        seismicRing.scaleX = 0.35f
        seismicRing.scaleY = 0.35f

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
            .setDuration(900L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate().alpha(1f).setStartDelay(650L).setDuration(700L).start()
        brand.animate().alpha(1f).setStartDelay(1050L).setDuration(700L).start()

        root.animate()
            .alpha(0f)
            .setStartDelay(INTRO_DURATION - 500L)
            .setDuration(500L)
            .withEndAction { root.visibility = View.GONE }
            .start()
    }
}
