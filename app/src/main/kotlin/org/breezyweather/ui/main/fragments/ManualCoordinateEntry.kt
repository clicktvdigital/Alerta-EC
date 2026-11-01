package org.breezyweather.ui.main.fragments

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import org.breezyweather.ui.search.ManualCoordinates

internal fun showManualCoordinateEntry(
    context: Context,
    onCoordinates: (Double, Double, String?) -> Unit
) {
    val input = EditText(context).apply {
        hint = "Latitud, longitud"
        setSingleLine(true)
    }

    val labelInput = EditText(context).apply {
        hint = "Nombre del barrio o sector (opcional)"
        setSingleLine(true)
    }
    val fields = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        setPadding(pad, 0, pad, 0)
        addView(input)
        addView(labelInput)
    }

    val dialog = AlertDialog.Builder(context)
        .setTitle("📍 Ubicación por coordenadas")
        .setMessage("Introduce primero la latitud y después la longitud. Ejemplo: -0.15, -78.50")
        .setView(fields)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Continuar", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val coordinates = ManualCoordinates.parse(input.text.toString())
            if (coordinates == null) {
                input.error = "Introduce una latitud y longitud válidas"
            } else {
                dialog.dismiss()
                onCoordinates(
                    coordinates.first,
                    coordinates.second,
                    labelInput.text.toString().trim().take(60).ifBlank { null }
                )
            }
        }
    }

    dialog.show()
}
