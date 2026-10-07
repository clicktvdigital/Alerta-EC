package org.breezyweather.ui.main.intro

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object AlertaEcIntroController {

    private const val INTRO_DURATION = 5000L

    fun play(root: View, logo: View, title: View, brand: View) {
        root.visibility = View.VISIBLE
        root.alpha = 1f

        logo.alpha = 0f
        logo.scaleX = 0.72f
        logo.scaleY = 0.72f
        title.alpha = 0f
        brand.alpha = 0f

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
