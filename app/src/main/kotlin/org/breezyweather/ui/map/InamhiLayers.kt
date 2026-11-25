package org.breezyweather.ui.map

internal object InamhiLayers {
    const val WMS_URL = "https://geoservicios.inamhi.gob.ec/geoserver/ows"
    const val PRECIPITATION_3H = "inamhi:wrf_3h_precipitacion"
    const val PRECIPITATION_24H = "inamhi:wrf_24h_precipitacion"
    const val TEMPERATURE_3H = "inamhi:wrf_3h_temperatura"
    // INAMHI publishes no 1 h precipitation layer; its hourly WRF layers are temperature, wind and humidity.
    const val TEMPERATURE_1H = "inamhi:wrf_1h_temperatura_rafk"
    const val WIND_3H = "inamhi:wrf_3h_vientos"
    const val HUMIDITY_1H = "inamhi:wrf_1h_humedad_rafk"

    // The 24 h precipitation layer rarely has a time inside the next hours, so it is replaced by hourly humidity.
    val all = listOf(PRECIPITATION_3H, HUMIDITY_1H, TEMPERATURE_3H, TEMPERATURE_1H, WIND_3H)

    fun title(layer: String): String = when (layer) {
        PRECIPITATION_3H -> "Precipitación 3 h"
        PRECIPITATION_24H -> "Precipitación 24 h"
        TEMPERATURE_3H -> "Temperatura 3 h"
        TEMPERATURE_1H -> "Temperatura 1 h"
        WIND_3H -> "Viento 3 h"
        HUMIDITY_1H -> "Humedad 1 h"
        else -> throw IllegalArgumentException("Unsupported INAMHI layer: $layer")
    }

    // Rectángulo aproximado configurado; no demuestra datos disponibles en cada punto.
    fun covers(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -5.1403..1.5287 && longitude in -81.1259..-74.9699
}
