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
    val imageUrl: String
)

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
        imageName: Regex,
        limit: Int = 24
    ): List<NoaaSatelliteFrame> = withContext(Dispatchers.IO) {
        val base = NOAA_CDN + directory.trim('/') + "/"
        val connection = URL(base).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "AlertaEC/0.1 (GOES NOAA satellite viewer)")
        try {
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val now = Instant.now()
            imageName.findAll(html).mapNotNull { match ->
                runCatching {
                    // YYYY + day-of-year + HH + mm (UTC), 11 digits.
                    val stamp = match.groupValues[1]
                    val day = LocalDate.ofYearDay(
                        stamp.substring(0, 4).toInt(),
                        stamp.substring(4, 7).toInt()
                    )
                    val observedAt = day.atTime(
                        stamp.substring(7, 9).toInt(),
                        stamp.substring(9, 11).toInt()
                    ).toInstant(ZoneOffset.UTC)
                    NoaaSatelliteFrame(observedAt, base + match.value)
                }.getOrNull()
            }.distinctBy { it.observedAt }
                .filter { frame ->
                    val age = java.time.Duration.between(frame.observedAt, now)
                    !age.isNegative && age.toMinutes() < 360
                }
                .sortedBy { it.observedAt }
                .toList()
                .takeLast(limit)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun recent(): List<NoaaSatelliteFrame> = fetch(
        "ABI/SECTOR/nsa/GEOCOLOR",
        Regex("([0-9]{11})_GOES19-ABI-nsa-GEOCOLOR-1800x1080[.]jpg")
    )

    suspend fun lightningRecent(): List<NoaaSatelliteFrame> = fetch(
        "GLM/SECTOR/nsa/EXTENT3",
        Regex("([0-9]{11})_GOES19-GLM-nsa-EXTENT3-1800x1080[.]jpg")
    )

    suspend fun fullDiskRecent(): List<NoaaSatelliteFrame> = fetch(
        "ABI/FD/GEOCOLOR",
        Regex("([0-9]{11})_GOES19-ABI-FD-GEOCOLOR-1808x1808[.]jpg")
    )
}
