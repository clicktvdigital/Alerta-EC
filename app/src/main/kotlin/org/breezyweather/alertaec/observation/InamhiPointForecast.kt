package org.breezyweather.alertaec.observation

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * INAMHI (Ecuador's met service) WRF model at one point, read from its public GeoServer
 * (`geoservicios.inamhi.gob.ec`, the same WMS layers the weather map draws) with WMS GetFeatureInfo:
 * precipitation accumulated in 3 h and 2 m air temperature. Continental Ecuador only (no Galápagos).
 */
object InamhiPointForecast {
    data class Values(val precipitation3hMm: Double?, val temperatureC: Double?)

    private const val WMS_URL = "https://geoservicios.inamhi.gob.ec/geoserver/ows"
    private const val PRECIPITATION_3H = "inamhi:wrf_3h_precipitacion"
    private const val TEMPERATURE_3H = "inamhi:wrf_3h_temperatura"
    private const val CACHE_MS = 30 * 60 * 1000L
    private val cache = HashMap<String, Pair<Long, Values>>()

    /** Continental Ecuador, where the WRF domain is configured. */
    fun covers(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -5.1..1.6 && longitude in -81.2..-75.1

    suspend fun at(latitude: Double, longitude: Double): Values? = withContext(Dispatchers.IO) {
        if (!covers(latitude, longitude)) return@withContext null
        val key = String.format(Locale.US, "%.3f,%.3f", latitude, longitude)
        val now = System.currentTimeMillis()
        synchronized(cache) { cache[key] }?.takeIf { now - it.first < CACHE_MS }?.let { return@withContext it.second }
        val values = Values(
            precipitation3hMm = runCatching { query(PRECIPITATION_3H, latitude, longitude) }.getOrNull()
                ?.takeIf { it >= 0.0 },
            temperatureC = runCatching { query(TEMPERATURE_3H, latitude, longitude) }.getOrNull()
                ?.takeIf { it in -40.0..60.0 },
        )
        if (values.precipitation3hMm == null && values.temperatureC == null) return@withContext null
        synchronized(cache) { cache[key] = now to values }
        values
    }

    private fun query(layer: String, latitude: Double, longitude: Double): Double? {
        val d = 0.01
        val bbox = String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f", longitude - d, latitude - d, longitude + d, latitude + d)
        val encoded = URLEncoder.encode(layer, "UTF-8")
        val url = "$WMS_URL?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&LAYERS=$encoded&QUERY_LAYERS=$encoded" +
            "&SRS=EPSG:4326&BBOX=$bbox&WIDTH=101&HEIGHT=101&X=50&Y=50&INFO_FORMAT=application/json"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "AlertaEC/0.1 (Android; pronostico INAMHI)")
        }
        return try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            // {"features":[{"properties":{"Precipitacion_acumulada_cada_3_horas":0.44}}]}
            Regex("\"properties\"\\s*:\\s*\\{\\s*\"[^\"]+\"\\s*:\\s*(-?[0-9.eE+-]+)").find(body)
                ?.groupValues?.get(1)?.toDoubleOrNull()
        } finally {
            connection.disconnect()
        }
    }
}
