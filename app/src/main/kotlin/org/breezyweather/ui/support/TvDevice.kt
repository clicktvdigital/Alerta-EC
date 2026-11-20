package org.breezyweather.ui.support

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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
 * El borde aparece únicamente mientras la vista tiene el foco. Es el mismo anillo de dos tonos
 * de Inicio (azul marino por fuera, ámbar por dentro): el blanco anterior no se veía sobre los
 * botones gris claro del mapa.
 */
internal fun View.applyTvFocusRing() {
    if (!context.isTelevisionDevice()) return
    val density = resources.displayMetrics.density
    val outer = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke((6 * density).toInt(), TV_RING_OUTER)
        cornerRadius = 12 * density
    }
    val inner = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke((3 * density).toInt(), TV_RING_INNER)
        cornerRadius = 10 * density
    }
    val inset = (3 * density).toInt()
    val ring = LayerDrawable(arrayOf(outer, inner)).apply {
        setLayerInset(1, inset, inset, inset, inset)
    }
    setOnFocusChangeListener { view, hasFocus ->
        view.foreground = if (hasFocus) ring else null
    }
}

private const val TV_RING_OUTER = 0xFF0B1F3A.toInt()
private const val TV_RING_INNER = 0xFFFFC400.toInt()
