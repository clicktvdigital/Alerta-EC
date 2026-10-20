package org.breezyweather.background.location

internal data class NearbyLandmark(
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val source: String = "OpenStreetMap",
) {
    val distanceLabel: String
        get() = if (distanceMeters < 1000) {
            "$distanceMeters m"
        } else {
            String.format(
                java.util.Locale.forLanguageTag("es-EC"),
                "%.1f km",
                distanceMeters / 1000.0,
            )
        }
}
