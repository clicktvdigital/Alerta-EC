package org.breezyweather.ui.main.fragments

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import breezyweather.domain.location.model.Location
import org.breezyweather.domain.location.model.getPlace
import org.breezyweather.sources.gadgetbridge.LauncherWeatherLocation
import java.util.Locale

internal fun showLauncherWeatherLocationPicker(
    context: Context,
    locations: List<Location>,
    onSelectionChanged: () -> Unit
) {
    val selectedId = LauncherWeatherLocation.preferredId(context)

    val names = listOf("Automático: GPS o primera ubicación") +
        locations.map { location ->
            if (location.isCurrentPosition) {
                "GPS actual"
            } else {
                val name = location.customName?.takeIf { it.isNotBlank() }
                    ?: location.getPlace(context).ifBlank { "Ubicación guardada" }
                val point = String.format(
                    Locale.US,
                    "%.5f, %.5f",
                    location.latitude,
                    location.longitude
                )
                "$name ($point)"
            }
        }

    val selected = locations.indexOfFirst {
        it.formattedId == selectedId
    }.let { if (it < 0) 0 else it + 1 }

    AlertDialog.Builder(context)
        .setTitle("📺 Ubicación del clima en CLICK TV")
        .setSingleChoiceItems(names.toTypedArray(), selected) { dialog, index ->
            if (index == 0) {
                LauncherWeatherLocation.clear(context)
            } else {
                LauncherWeatherLocation.select(context, locations[index - 1])
            }
            onSelectionChanged()
            dialog.dismiss()
            Toast.makeText(
                context,
                "Ubicación guardada. Se aplicará con la próxima actualización meteorológica.",
                Toast.LENGTH_LONG
            ).show()
        }
        .setNegativeButton("Cancelar", null)
        .show()
}
