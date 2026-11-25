package org.breezyweather.ui.map

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Colour scale of an INAMHI WRF layer, read from the GeoServer itself (WMS GetLegendGraphic, JSON), so the
 * legend always matches the colours drawn on the map.
 */
internal object InamhiLegend {
    data class Entry(val value: String, val color: Int)
    data class Legend(val title: String, val unit: String, val entries: List<Entry>)

    private val cache = HashMap<String, Legend>()

    fun unit(layer: String): String = when (layer) {
        InamhiLayers.PRECIPITATION_3H, InamhiLayers.PRECIPITATION_24H -> "mm"
        InamhiLayers.TEMPERATURE_3H, InamhiLayers.TEMPERATURE_1H -> "°C"
        InamhiLayers.WIND_3H -> "km/h"
        else -> ""
    }

    suspend fun load(layer: String): Legend? = withContext(Dispatchers.IO) {
        synchronized(cache) { cache[layer] }?.let { return@withContext it }
        val url = InamhiLayers.WMS_URL + "?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetLegendGraphic" +
            "&FORMAT=application/json&LAYER=" + URLEncoder.encode(layer, "UTF-8")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "AlertaEC/0.1 (Android; leyenda INAMHI)")
        }
        val legend = runCatching {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val first = JSONObject(body).getJSONArray("Legend").getJSONObject(0)
            val raster = first.getJSONArray("rules").getJSONObject(0)
                .getJSONArray("symbolizers").getJSONObject(0).getJSONObject("Raster")
            val array = raster.getJSONObject("colormap").getJSONArray("entries")
            val entries = (0 until array.length()).mapNotNull { i ->
                val item = array.getJSONObject(i)
                val color = runCatching { android.graphics.Color.parseColor(item.getString("color")) }.getOrNull()
                color?.let { Entry(item.optString("quantity"), it) }
            }
            Legend(InamhiLayers.title(layer), unit(layer), entries)
        }.getOrNull()
        connection.disconnect()
        legend?.takeIf { it.entries.isNotEmpty() }?.also { synchronized(cache) { cache[layer] = it } }
    }
}
