package org.breezyweather.background.location

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object LocationHumanFormatter {
    fun decimalCoordinates(lat: Double, lon: Double) = String.format(Locale.US, "%.6f, %.6f", lat, lon)

    fun dmsCoordinates(lat: Double, lon: Double) = toDms(lat, true) + " · " + toDms(lon, false)

    fun accuracy(value: Float?) = value?.let { String.format(Locale.US, "±%.0f m", it) } ?: "No disponible"

    fun altitude(value: Double?) = value?.let { String.format(Locale.US, "%.0f m s. n. m.", it) } ?: "No disponible"

    fun pressure(value: Float?) = value?.let { String.format(Locale.US, "%.1f hPa", it) } ?: "No disponible"

    fun heading(value: AdvancedLocationSnapshot): String {
        val degrees = value.headingDegrees ?: return "No disponible"
        return String.format(
            Locale.US,
            "%.0f° · %s · %s",
            degrees,
            value.headingCardinal ?: "-",
            value.headingDirectionName ?: "-"
        )
    }

    private fun toDms(value: Double, latitude: Boolean): String {
        val a = abs(value)
        val d = floor(a).toInt()
        val mf = (a - d) * 60.0
        val m = floor(mf).toInt()
        val s = (mf - m) * 60.0
        val h = if (latitude) {
            if (value >= 0) "N" else "S"
        } else {
            if (value >= 0) "E" else "O"
        }
        return String.format(Locale.US, "%d° %02d min %05.2f sec %s", d, m, s, h)
    }
}
