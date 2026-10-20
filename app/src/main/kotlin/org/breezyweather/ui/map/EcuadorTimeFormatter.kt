package org.breezyweather.ui.map

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object EcuadorTimeFormatter {
    // Límites aproximados del recurso breezytz_ec.json; no representan costas ni fronteras.
    fun isGalapagosCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -1.4016..1.6644 &&
            longitude in -92.0116..-89.2548


    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-EC"))

    fun format(instant: Instant, galapagos: Boolean = false): String {
        val zone = ZoneId.of(if (galapagos) "Pacific/Galapagos" else "America/Guayaquil")
        val label = if (galapagos) "Galápagos (UTC−6)" else "Ecuador continental (Quito, UTC−5)"
        return formatter.withZone(zone).format(instant) + " · " + label
    }
}
