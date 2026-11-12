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
                // Las filas con Compose (botones de mapa, reporte y evidencia) manejan su
                // propio foco; marcarlas como enfocables les quitaba el OK.
                if (containsCompose(view)) return
                view.isFocusable = true
                view.isFocusableInTouchMode = false
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
                    } else null
                    v.scaleX = if (hasFocus) 1.03f else 1f
                    v.scaleY = if (hasFocus) 1.03f else 1f
                    v.alpha = if (hasFocus) 1f else 0.9f
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {}
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.normal_margin).div(2)
        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)
    }
}

private fun containsCompose(view: View): Boolean =
    view is androidx.compose.ui.platform.AbstractComposeView ||
        (view is android.view.ViewGroup && (0 until view.childCount).any { containsCompose(view.getChildAt(it)) })
