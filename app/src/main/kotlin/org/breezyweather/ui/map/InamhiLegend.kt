package org.breezyweather.ui.map

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** One colour step of an INAMHI WMS colour map: values from [quantity] upwards are painted [color]. */
internal data class InamhiLegendStep(val quantity: Double, val color: Int)

/**
 * Official colour scale of each INAMHI layer, read from the WMS itself (GetLegendGraphic, JSON), so the
 * legend always matches what the tiles paint. Shown as a strip of coloured value chips, readable on a TV.
 */
internal object InamhiLegend {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, List<InamhiLegendStep>>()

    fun unit(layer: String): String = when (layer) {
        InamhiLayers.TEMPERATURE_3H -> "°C"
        else -> "mm"
    }

    fun cached(layer: String): List<InamhiLegendStep>? = synchronized(cache) { cache[layer] }

    suspend fun load(layer: String): List<InamhiLegendStep>? = withContext(Dispatchers.IO) {
        cached(layer)?.let { return@withContext it }
        val url = InamhiLayers.WMS_URL + "?service=WMS&request=GetLegendGraphic&version=1.1.1" +
            "&format=application/json&layer=" + URLEncoder.encode(layer, "UTF-8")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            parse(connection.inputStream.bufferedReader().use { it.readText() })
                ?.also { steps -> synchronized(cache) { cache[layer] = steps } }
        } catch (_: java.io.IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** Parses GeoServer's JSON legend (Legend[0].rules[0].symbolizers[0].Raster.colormap.entries). */
    fun parse(body: String): List<InamhiLegendStep>? = runCatching {
        val legend = json.parseToJsonElement(body).jsonObject["Legend"]!!.jsonArray[0].jsonObject
        val raster = (legend["rules"] as JsonArray)[0].jsonObject["symbolizers"]!!.jsonArray[0]
            .jsonObject["Raster"]!!.jsonObject
        val entries = raster["colormap"]!!.jsonObject["entries"]!!.jsonArray
        entries.mapNotNull { entry ->
            val item = entry as? JsonObject ?: return@mapNotNull null
            val quantity = item["quantity"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
            val color = item["color"]?.jsonPrimitive?.content?.let(::parseHexColor) ?: return@mapNotNull null
            InamhiLegendStep(quantity, color)
        }.sortedBy { it.quantity }.takeIf { it.isNotEmpty() }
    }.getOrNull()

    /** "Escala oficial (mm): [1][5][10]…" with each value on its own colour and a readable text colour. */
    fun strip(title: String, unit: String, steps: List<InamhiLegendStep>): CharSequence {
        val text = SpannableStringBuilder("   $title ($unit): ")
        steps.filter { it.quantity > 0.0 || steps.size == 1 }.forEach { step ->
            val start = text.length
            text.append(" ").append(format(step.quantity)).append(" ")
            text.setSpan(BackgroundColorSpan(step.color), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(
                ForegroundColorSpan(if (luminance(step.color) > 0.5) Color.BLACK else Color.WHITE),
                start,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text.append(" ")
        }
        return text
    }

    /**
     * Vertical scale for a side panel: title, then one row per step from the highest value down, each value on its
     * own colour. Keeps the map clear instead of a long strip across its middle.
     */
    fun column(title: String, unit: String, steps: List<InamhiLegendStep>): CharSequence {
        val text = SpannableStringBuilder("$title\n($unit)")
        steps.filter { it.quantity > 0.0 || steps.size == 1 }.asReversed().forEach { step ->
            text.append("\n")
            val start = text.length
            text.append("  ").append(format(step.quantity).padStart(4)).append("  ")
            text.setSpan(BackgroundColorSpan(step.color), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(
                ForegroundColorSpan(if (luminance(step.color) > 0.5) Color.BLACK else Color.WHITE),
                start,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return text
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.forLanguageTag("es-EC"), "%.1f", value)

    /** "#RRGGBB" → opaque ARGB int; null if malformed. Pure (no android.graphics), so it is unit-testable. */
    internal fun parseHexColor(value: String): Int? {
        val hex = value.trim().removePrefix("#")
        if (hex.length != 6 || hex.any { it !in "0123456789abcdefABCDEF" }) return null
        return (0xFF shl 24) or hex.toInt(16)
    }

    /** Relative luminance (sRGB approximation), 0 = black, 1 = white. */
    internal fun luminance(color: Int): Double {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
    }
}
