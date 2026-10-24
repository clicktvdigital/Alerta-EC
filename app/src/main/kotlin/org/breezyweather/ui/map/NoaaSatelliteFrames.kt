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

internal object NoaaSatelliteFrames {
    private const val PAGE = "https://www.goes.noaa.gov/sector_band.php?band=GEOCOLOR&length=24&sat=G19&sector=nsa"

    suspend fun lightningRecent(): List<NoaaSatelliteFrame> =
        withContext(Dispatchers.IO) {
            val page = "https://www.star.nesdis.noaa.gov/goes/sector_band.php?band=EXTENT3&length=24&sat=G19&sector=nsa"
            val connection = URL(page).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 20000
            try {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val pattern = Regex(
                    "https://cdn[.]star[.]nesdis[.]noaa[.]gov/GOES19/GLM/SECTOR/nsa/EXTENT3/([0-9]{11})_GOES19-GLM-nsa-EXTENT3-1800x1080[.]jpg"
                )
                val now = Instant.now()
                pattern.findAll(html).mapNotNull { match ->
                    runCatching {
                        val stamp = match.groupValues[1]
                        val date = LocalDate.ofYearDay(
                            stamp.substring(0, 4).toInt(),
                            stamp.substring(4, 7).toInt()
                        )
                        val observed = date.atTime(
                            stamp.substring(7, 9).toInt(),
                            stamp.substring(9, 11).toInt()
                        ).toInstant(ZoneOffset.UTC)
                        NoaaSatelliteFrame(observed, match.value)
                    }.getOrNull()
                }.distinctBy { it.observedAt }
                    .filter {
                        val age = java.time.Duration.between(it.observedAt, now)
                        !age.isNegative && age.toMinutes() < 360
                    }.sortedBy { it.observedAt }.toList()
            } finally {
                connection.disconnect()
            }
        }

    suspend fun fullDiskRecent(): List<NoaaSatelliteFrame> =
        withContext(Dispatchers.IO) {
            val base = "https://cdn.star.nesdis.noaa.gov/GOES19/ABI/FD/GEOCOLOR/"
            val connection = URL(base).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 20000
            try {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val pattern = Regex("[0-9]{11}_GOES19-ABI-FD-GEOCOLOR-1808x1808[.]jpg")
                val now = Instant.now()
                pattern.findAll(html).mapNotNull { match ->
                    runCatching {
                        val stamp = match.value.substring(0, 11)
                        val date = LocalDate.ofYearDay(
                            stamp.substring(0, 4).toInt(),
                            stamp.substring(4, 7).toInt()
                        )
                        val observed = date.atTime(
                            stamp.substring(7, 9).toInt(),
                            stamp.substring(9, 11).toInt()
                        ).toInstant(ZoneOffset.UTC)
                        NoaaSatelliteFrame(observed, base + match.value)
                    }.getOrNull()
                }.distinctBy { it.observedAt }
                    .filter {
                        val age = java.time.Duration.between(it.observedAt, now)
                        !age.isNegative && age.toMinutes() < 360
                    }.sortedBy { it.observedAt }.toList().takeLast(24)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun recent(): List<NoaaSatelliteFrame> = withContext(Dispatchers.IO) {
        val connection = URL(PAGE).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        try {
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val pattern = "https://cdn[.]star[.]nesdis[.]noaa[.]gov/GOES19/ABI/SECTOR/nsa/GEOCOLOR/([0-9]{11})_GOES19-ABI-nsa-GEOCOLOR-1800x1080[.]jpg"
            val now = Instant.now()
            Regex(pattern).findAll(html).mapNotNull { match ->
                runCatching {
                    val stamp = match.groupValues[1]
                    val date = LocalDate.ofYearDay(stamp.substring(0, 4).toInt(), stamp.substring(4, 7).toInt())
                    val observed = date.atTime(stamp.substring(7, 9).toInt(), stamp.substring(9, 11).toInt()).toInstant(ZoneOffset.UTC)
                    NoaaSatelliteFrame(observed, match.value)
                }.getOrNull()
            }.distinctBy { it.observedAt }.filter {
                val age = java.time.Duration.between(it.observedAt, now)
                !age.isNegative && age.toMinutes() < 360
            }.sortedBy { it.observedAt }.toList()
        } finally {
            connection.disconnect()
        }
    }
}
