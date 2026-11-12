/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.main.widgets

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import org.breezyweather.R

class FitTabletRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        // Con mando de TV, cada fila del pronóstico debe poder recibir el foco para
        // que arriba y abajo se muevan entre filas. Al tener el foco se aclara un poco.
        // Que el halo y las filas no se recorten en los bordes.
        clipChildren = false
        clipToPadding = false
        addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                // Las filas con Compose también deben poder recibir el foco; si no, el mando
                // se queda atascado en la fila anterior. Su holder pasa el foco al primer botón
                // con su propio listener, así que no se le reemplaza aquí.
                // El encabezado de temperatura es solo informativo: no debe recibir el foco.
                if (view.id == R.id.container_main_header) return
                if (view is androidx.compose.ui.platform.AbstractComposeView) {
                    view.isFocusable = true
                    view.isFocusableInTouchMode = true
                    return
                }
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                view.setOnFocusChangeListener { v, hasFocus ->
                    // Halo neón cian alrededor de la tarjeta seleccionada, sin cambiar su tamaño.
                    v.foreground = if (hasFocus) {
                        val density = v.resources.displayMetrics.density
                        android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 16f * density
                            setStroke((5 * density).toInt(), android.graphics.Color.parseColor("#00E5FF"))
                        }
                    } else null
                    v.alpha = if (hasFocus) 1f else 0.9f
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {}
        })
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        // Con mando de TV: arriba/abajo mueven el foco entre filas y, si no hay más filas
        // en esa dirección, desplazan la lista. Izquierda/derecha mueven el foco de lado.
        if (event != null && event.action == KeyEvent.ACTION_DOWN) {
            val direction = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
                KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
                else -> null
            }
            val focused = findFocus()
            if (direction != null && focused != null) {
                val next = focused.focusSearch(direction)
                if (next != null && next !== focused && isAncestorOf(next)) {
                    next.requestFocus(direction)
                    return true
                }
                if (direction == View.FOCUS_UP || direction == View.FOCUS_DOWN) {
                    val step = resources.displayMetrics.density * 160 *
                        (if (direction == View.FOCUS_DOWN) 1 else -1)
                    smoothScrollBy(0, step.toInt())
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isAncestorOf(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            if (parent === this) return true
            parent = parent.parent
        }
        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.normal_margin).div(2)
        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)
    }
}

