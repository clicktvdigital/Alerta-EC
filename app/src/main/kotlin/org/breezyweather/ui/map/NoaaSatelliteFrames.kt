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
            val now = Instant.now()
            // The directory listing is 1-2 MB: read it line by line instead of holding it as one string.
            val found = ArrayList<NoaaSatelliteFrame>()
            connection.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    imageName.findAll(line).forEach { match ->
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
                            val age = java.time.Duration.between(observedAt, now)
                            if (!age.isNegative && age.toMinutes() < 360) {
                                found += NoaaSatelliteFrame(observedAt, base + match.value)
                            }
                        }
                    }
                }
            }
            found.distinctBy { it.observedAt }
                .sortedBy { it.observedAt }
                .takeLast(limit)
        } finally {
            connection.disconnect()
        }
    }

    /** Android TV / low-memory boxes get the 900x540 (678 px full disk) JPEGs and fewer frames: 4x less to download. */
    private fun size(compact: Boolean) = if (compact) "900x540" else "1800x1080"
    private fun frames(compact: Boolean) = if (compact) 12 else 24

    suspend fun recent(compact: Boolean = false): List<NoaaSatelliteFrame> = fetch(
        "ABI/SECTOR/nsa/GEOCOLOR",
        Regex("([0-9]{11})_GOES19-ABI-nsa-GEOCOLOR-${size(compact)}[.]jpg"),
        frames(compact)
    )

    /** Rain: the "Sandwich" product highlights deep convective (storm) clouds, where heavy rain falls. */
    suspend fun rainRecent(compact: Boolean = false): List<NoaaSatelliteFrame> = fetch(
        "ABI/SECTOR/nsa/Sandwich",
        Regex("([0-9]{11})_GOES19-ABI-nsa-Sandwich-${size(compact)}[.]jpg"),
        frames(compact)
    )

    suspend fun lightningRecent(compact: Boolean = false): List<NoaaSatelliteFrame> = fetch(
        "GLM/SECTOR/nsa/EXTENT3",
        Regex("([0-9]{11})_GOES19-GLM-nsa-EXTENT3-${size(compact)}[.]jpg"),
        frames(compact)
    )

    suspend fun fullDiskRecent(compact: Boolean = false): List<NoaaSatelliteFrame> = fetch(
        "ABI/FD/GEOCOLOR",
        Regex("([0-9]{11})_GOES19-ABI-FD-GEOCOLOR-${if (compact) "678x678" else "1808x1808"}[.]jpg"),
        frames(compact)
    )
}
