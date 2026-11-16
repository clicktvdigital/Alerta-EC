package org.breezyweather.ui.support

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Detección única de Android TV / TV Box. Todo ajuste exclusivo de TV debe pasar por aquí
 * para que los teléfonos y tablets sigan con la experiencia táctil de siempre.
 */
internal fun Context.isTelevisionDevice(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature("android.software.leanback") ||
        packageManager.hasSystemFeature("android.hardware.type.television")

/**
 * Borde de foco visible para el mando D-pad. Solo se activa en TV: en teléfono no hace nada.
 * El borde aparece únicamente mientras la vista tiene el foco.
 */
internal fun View.applyTvFocusRing() {
    if (!context.isTelevisionDevice()) return
    val density = resources.displayMetrics.density
    val ring = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke((3 * density).toInt(), Color.WHITE)
        cornerRadius = 10 * density
    }
    setOnFocusChangeListener { view, hasFocus ->
        view.foreground = if (hasFocus) ring else null
    }
}
