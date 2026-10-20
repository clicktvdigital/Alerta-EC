package org.breezyweather.ui.map

internal object InamhiLayers {
    const val WMS_URL = "https://geoservicios.inamhi.gob.ec/geoserver/ows"
    const val PRECIPITATION_3H = "inamhi:wrf_3h_precipitacion"
    const val PRECIPITATION_24H = "inamhi:wrf_24h_precipitacion"
    const val TEMPERATURE_3H = "inamhi:wrf_3h_temperatura"

    val all = listOf(PRECIPITATION_3H, PRECIPITATION_24H, TEMPERATURE_3H)

    fun title(layer: String): String = when (layer) {
        PRECIPITATION_3H -> "Precipitación 3 h"
        PRECIPITATION_24H -> "Precipitación 24 h"
        TEMPERATURE_3H -> "Temperatura 3 h"
        else -> throw IllegalArgumentException("Unsupported INAMHI layer: $layer")
    }

    // Rectángulo aproximado configurado; no demuestra datos disponibles en cada punto.
    fun covers(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -5.1403..1.5287 && longitude in -81.1259..-74.9699
}
