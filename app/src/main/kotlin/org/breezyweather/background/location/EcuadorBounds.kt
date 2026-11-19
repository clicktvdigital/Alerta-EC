package org.breezyweather.background.location

/** Rough bounding box of mainland Ecuador plus Galápagos, for cheap "is this point in Ecuador?" checks. */
object EcuadorBounds {
    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -5.1..1.7 && longitude in -92.1..-75.0
}
