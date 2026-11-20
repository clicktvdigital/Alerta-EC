package org.breezyweather.ui.map

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class NoaaSatelliteFrame(
    val observedAt: Instant,
    val imageUrl: String,
    /** Region to decode, as fractions of the image (left, top, right, bottom); null = whole image. */
    val crop: NoaaCrop? = null,
) {
    /** Cache/identity key: the same NOAA image can be shown whole or cropped. */
    val key: String get() = if (crop == null) imageUrl else "$imageUrl#$crop"
}

/** Fractional crop of a NOAA image; values in 0..1, left < right, top < bottom. */
internal data class NoaaCrop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left in 0f..1f && right in 0f..1f && top in 0f..1f && bottom in 0f..1f)
        require(left < right && top < bottom)
    }
}

/**
 * V5: listar los JPEG fechados publicados por NOAA, en vez de asumir el formato
 * de enlaces de sector_band.php. No presentar imágenes caducadas como recientes.
 * La región nsa cubre el norte de Sudamérica; no equivale a una imagen geolocal
 * de una vivienda ni a una alerta operacional.
 */
internal object NoaaSatelliteFrames {
    private const val NOAA_CDN = "https://cdn.star.nesdis.noaa.gov/GOES19/"

    private suspend fun fetch(
        directory: String,
        fileSuffix: String,
        limit: Int = 8,
        crop: NoaaCrop? = null,
    ): List<NoaaSatelliteFrame> = withContext(Dispatchers.IO) {
        val base = NOAA_CDN + directory.trim('/') + "/"
        val connection = URL(base).openConnection() as HttpURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "AlertaEC/0.1 (GOES NOAA satellite viewer)")
        try {
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            android.util.Log.i("AlertaEC-Map", "NOAA $base bytes=${html.length} code=${connection.responseCode}")
            parseListing(html, base, fileSuffix, limit, crop)
        } catch (e: Exception) {
            android.util.Log.e("AlertaEC-Map", "NOAA fallo $base", e)
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Newest [limit] frames named `<YYYYDDDHHmm>[fileSuffix]` in a NOAA directory listing, oldest first.
     *
     * Plain indexOf on purpose: Kotlin's Regex.findAll calls Matcher.find(start), which on Android resets the
     * ICU matcher and copies the whole input to native memory on every match. On the 1.1–2.2 MB NOAA listings
     * (~1,400 matches) that never finished on the TV and grew the native heap until lowmemorykiller killed the
     * app in the foreground (2026-11-20).
     */
    internal fun parseListing(
        html: String,
        base: String,
        fileSuffix: String,
        limit: Int,
        crop: NoaaCrop? = null,
    ): List<NoaaSatelliteFrame> {
        val byTime = HashMap<Instant, NoaaSatelliteFrame>()
        var at = html.indexOf(fileSuffix)
        while (at >= 0) {
            val start = at - STAMP_LENGTH
            if (start >= 0) {
                val stamp = html.substring(start, at)
                if (stamp.all { it in '0'..'9' }) {
                    runCatching {
                        val day = LocalDate.ofYearDay(stamp.substring(0, 4).toInt(), stamp.substring(4, 7).toInt())
                        val observedAt = day.atTime(stamp.substring(7, 9).toInt(), stamp.substring(9, 11).toInt())
                            .toInstant(ZoneOffset.UTC)
                        byTime.getOrPut(observedAt) { NoaaSatelliteFrame(observedAt, base + stamp + fileSuffix, crop) }
                    }
                }
            }
            at = html.indexOf(fileSuffix, at + fileSuffix.length)
        }
        return byTime.values.sortedBy { it.observedAt }.takeLast(limit)
    }

    /** YYYY + day-of-year + HH + mm (UTC). */
    private const val STAMP_LENGTH = 11

    /**
     * Continental Ecuador inside the GOES-19 "nsa" sector (calibrated on the 1800x1080 and 3600x2160
     * frames, which share the projection): includes southern Colombia and northern Peru as context.
     * Galápagos lies west of this sector and is not covered.
     */
    val ECUADOR = NoaaCrop(left = 0.035f, top = 0.40f, right = 0.265f, bottom = 0.67f)

    /** Clouds (GeoColor), framed on Ecuador. 3600 px source so the crop stays sharp on a TV. */
    suspend fun cloudsEcuador(): List<NoaaSatelliteFrame> = fetch(
        "ABI/SECTOR/nsa/GEOCOLOR",
        "_GOES19-ABI-nsa-GEOCOLOR-3600x2160.jpg",
        crop = ECUADOR,
    )

    /** Lightning (GLM flash extent), framed on Ecuador. */
    suspend fun lightningEcuador(): List<NoaaSatelliteFrame> = fetch(
        "GLM/SECTOR/nsa/EXTENT3",
        "_GOES19-GLM-nsa-EXTENT3-3600x2160.jpg",
        crop = ECUADOR,
    )

    /**
     * Rain-producing storm clouds (ABI "Sandwich": deep convection, cold cloud tops), framed on Ecuador.
     * It shows where storms are, not measured rainfall; INAMHI precipitation layers cover amounts.
     */
    suspend fun rainCloudsEcuador(): List<NoaaSatelliteFrame> = fetch(
        "ABI/SECTOR/nsa/Sandwich",
        "_GOES19-ABI-nsa-Sandwich-3600x2160.jpg",
        crop = ECUADOR,
    )

    /** Whole northern South America sector (Ecuador and neighbours), uncropped. */
    suspend fun fullMapRegion(): List<NoaaSatelliteFrame> = fetch(
        "ABI/SECTOR/nsa/GEOCOLOR",
        "_GOES19-ABI-nsa-GEOCOLOR-1800x1080.jpg"
    )
}
