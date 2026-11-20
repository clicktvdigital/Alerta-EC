package org.breezyweather.ui.map

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NoaaListingTest {
    private val base = "https://cdn.star.nesdis.noaa.gov/GOES19/ABI/SECTOR/nsa/GEOCOLOR/"
    private val suffix = "_GOES19-ABI-nsa-GEOCOLOR-3600x2160.jpg"

    private fun row(stamp: String, name: String = stamp + suffix) =
        "<a href=\"$name\">$name</a>  20-Nov-2026 03:50  2871984\n"

    @Test
    fun keepsNewestFramesOldestFirstAndDeduplicatesHrefAndText() {
        val html = buildString {
            append(row("20263240340"))
            append(row("20263240350"))
            append(row("20263240330"))
            append(row("20263240350", "20263240350_GOES19-ABI-nsa-GEOCOLOR-1800x1080.jpg"))
            append(row("latest00000", "latest.jpg"))
        }
        val frames = NoaaSatelliteFrames.parseListing(html, base, suffix, limit = 2, crop = NoaaSatelliteFrames.ECUADOR)
        assertEquals(
            listOf(Instant.parse("2026-11-20T03:40:00Z"), Instant.parse("2026-11-20T03:50:00Z")),
            frames.map { it.observedAt },
        )
        assertEquals(base + "20263240350" + suffix, frames.last().imageUrl)
        assertTrue(frames.all { it.crop == NoaaSatelliteFrames.ECUADOR })
    }

    @Test
    fun ignoresNamesWithoutAnElevenDigitStamp() {
        val html = "<a href=\"x$suffix\">x$suffix</a> <a href=\"2026324035$suffix\">bad</a>"
        assertTrue(NoaaSatelliteFrames.parseListing(html, base, suffix, limit = 8).isEmpty())
    }

    @Test
    fun parsesALargeListingQuickly() {
        // ~1,400 frames × href + text, the size of the real 1.1 MB listing that hung on the TV with Regex.findAll.
        val html = buildString {
            for (minute in 0 until 1400) {
                val day = 318 + minute / 288
                val hh = (minute % 288) / 12
                val mm = (minute % 12) * 5
                append(row("2026%03d%02d%02d".format(day, hh, mm)))
                append("<td>padding padding padding padding padding padding padding padding padding</td>\n".repeat(6))
            }
        }
        assertTrue(html.length > 1_000_000)
        val started = System.nanoTime()
        val frames = NoaaSatelliteFrames.parseListing(html, base, suffix, limit = 8)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(8, frames.size)
        assertTrue(elapsedMs < 2_000, "parse took $elapsedMs ms")
    }
}
