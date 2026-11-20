package org.breezyweather.background.location

import android.os.SystemClock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Official postal zone of a point, from the Ecuadorian postal regulator (codigopostal.gob.ec). */
data class EcuadorPostalZone(
    val postalCode: String,
    val province: String?,
    val provinceDpa: String?,
    val canton: String?,
    val cantonDpa: String?,
    /** Planning district code (e.g. 17D05), shared by the education and health districts. */
    val district: String?,
)

/**
 * Point → official postal zone, using the public OGC WFS of the Agencia de Regulación y Control Postal
 * (layer arcp_cp:zona_postal, EPSG:4326). This is the authoritative code: OpenStreetMap/Nominatim
 * postcodes can be wrong (e.g. Atucucho: OSM says 170318, the official zone is 170528).
 *
 * Blocking: call it off the main thread. Results are cached by rounded coordinates (≈11 m) because postal
 * zones are static; failures are cached briefly so the server is not hammered.
 */
object EcuadorPostalZoneService {
    const val PROVENANCE = "Agencia de Regulación y Control Postal (codigopostal.gob.ec)"
    private const val WFS = "https://www.codigopostal.gob.ec/geoserver/arcp_cp/ows"
    private const val ATTEMPTS = 3
    private const val HIT_TTL_MS = 7L * 24 * 60 * 60 * 1000
    private const val MISS_TTL_MS = 2L * 60 * 1000
    private const val CACHE_SIZE = 64

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = object : LinkedHashMap<String, Pair<Long, EcuadorPostalZone?>>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, EcuadorPostalZone?>>) =
            size > CACHE_SIZE
    }

    fun find(latitude: Double, longitude: Double, userAgent: String): EcuadorPostalZone? {
        if (!EcuadorBounds.contains(latitude, longitude)) return null
        val key = "%.4f,%.4f".format(java.util.Locale.US, latitude, longitude)
        val now = SystemClock.elapsedRealtime()
        synchronized(cache) {
            cache[key]?.let { (at, zone) ->
                val ttl = if (zone != null) HIT_TTL_MS else MISS_TTL_MS
                if (now - at < ttl) return zone
            }
        }
        var zone: EcuadorPostalZone? = null
        for (attempt in 1..ATTEMPTS) {
            val result = runCatching { request(latitude, longitude, userAgent) }
            if (result.isSuccess) {
                zone = result.getOrNull()
                break
            }
            if (result.exceptionOrNull() !is IOException || attempt == ATTEMPTS) break
            Thread.sleep(500L * attempt * attempt) // 0,5 s, 2 s
        }
        synchronized(cache) { cache[key] = now to zone }
        return zone
    }

    private fun request(latitude: Double, longitude: Double, userAgent: String): EcuadorPostalZone? {
        // WFS 1.0.0 + EPSG:4326 point order is lon lat; geometry attribute of the layer is "shape".
        val filter = "INTERSECTS(shape,POINT($longitude $latitude))"
        val url = "$WFS?service=WFS&version=1.0.0&request=GetFeature&typeName=arcp_cp:zona_postal" +
            "&outputFormat=application/json&maxFeatures=1" +
            "&propertyName=cod_postal,dpa_provin,dpa_despro,dpa_canton,dpa_descan,cod_distri" +
            "&CQL_FILTER=" + URLEncoder.encode(filter, "UTF-8")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code >= 500) throw IOException("HTTP $code")
            if (code != HttpURLConnection.HTTP_OK) return null
            return parse(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** Parses a WFS GeoJSON response; null if there is no feature or no valid 6-digit code. */
    internal fun parse(body: String): EcuadorPostalZone? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val properties = runCatching {
            root["features"]?.jsonArray?.firstOrNull()?.jsonObject?.get("properties")?.jsonObject
        }.getOrNull() ?: return null
        fun field(name: String) = (properties[name] as? JsonPrimitive)
            ?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
        val postal = field("cod_postal")?.takeIf(EcuadorPostalCodeAdapter::isValidCode) ?: return null
        return EcuadorPostalZone(
            postalCode = postal,
            province = field("dpa_despro")?.let(::titleCase),
            provinceDpa = field("dpa_provin"),
            canton = field("dpa_descan")?.let(::titleCase),
            cantonDpa = field("dpa_canton"),
            district = field("cod_distri"),
        )
    }

    /** "SANTO DOMINGO DE LOS TSACHILAS" → "Santo Domingo de los Tsachilas" (Spanish articles kept lower). */
    internal fun titleCase(value: String): String {
        val lower = setOf("de", "del", "la", "las", "los", "y")
        return value.lowercase(java.util.Locale.forLanguageTag("es")).split(" ").mapIndexed { i, word ->
            if (i > 0 && word in lower) word else word.replaceFirstChar { it.titlecase(java.util.Locale.forLanguageTag("es")) }
        }.joinToString(" ")
    }
}
