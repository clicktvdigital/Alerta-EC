package org.breezyweather.ui.search

/**
 * Reads decimal GPS coordinates entered by the user.
 *
 * Accepted examples:
 * -0.15, -78.50
 * -0.15; -78.50
 * -0.15 -78.50
 *
 * Latitude must come first, followed by longitude.
 */
internal object ManualCoordinates {

    fun parse(input: String): Pair<Double, Double>? {
        val parts = input.trim().split(Regex("[,;\\s]+"))
        if (parts.size != 2) return null

        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null

        if (!latitude.isFinite() || !longitude.isFinite()) return null
        if (latitude !in -90.0..90.0) return null
        if (longitude !in -180.0..180.0) return null
        if (latitude == 0.0 && longitude == 0.0) return null

        return latitude to longitude
    }
}
