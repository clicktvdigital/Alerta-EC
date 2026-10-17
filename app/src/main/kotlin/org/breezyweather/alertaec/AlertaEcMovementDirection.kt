package org.breezyweather.alertaec

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Direccion geografica de desplazamiento entre dos observaciones.
 * No deduce trayectoria a partir del viento ni de una sola imagen.
 */
object AlertaEcMovementDirection {
    private val directions = listOf(
        "Norte", "Nororiente", "Oriente", "Suroriente",
        "Sur", "Suroccidente", "Occidente", "Noroccidente"
    )

    fun between(
        initialLatitude: Double,
        initialLongitude: Double,
        finalLatitude: Double,
        finalLongitude: Double
    ): String? {
        require(
            initialLatitude in -90.0..90.0 &&
            finalLatitude in -90.0..90.0 &&
            initialLongitude in -180.0..180.0 &&
            finalLongitude in -180.0..180.0
        ) { "Coordenadas fuera de rango" }

        if (initialLatitude == finalLatitude &&
            initialLongitude == finalLongitude
        ) return null

        val lat1 = Math.toRadians(initialLatitude)
        val lat2 = Math.toRadians(finalLatitude)
        val longitudeDifference =
            Math.toRadians(finalLongitude - initialLongitude)

        val y = sin(longitudeDifference) * cos(lat2)
        val x = cos(lat1) * sin(lat2) -
            sin(lat1) * cos(lat2) * cos(longitudeDifference)

        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        val sector = ((bearing + 22.5) / 45.0).toInt() % 8
        return directions[sector]
    }
}
