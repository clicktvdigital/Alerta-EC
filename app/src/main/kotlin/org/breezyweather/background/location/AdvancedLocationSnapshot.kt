package org.breezyweather.background.location

data class AdvancedLocationSnapshot(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val horizontalAccuracyMeters: Float? = null,
    val gpsAltitudeMeters: Double? = null,
    val locationTimestampMillis: Long? = null,

    val pressureHpa: Float? = null,
    val headingDegrees: Float? = null,

    val accelerationX: Float? = null,
    val accelerationY: Float? = null,
    val accelerationZ: Float? = null,

    val gyroscopeX: Float? = null,
    val gyroscopeY: Float? = null,
    val gyroscopeZ: Float? = null,

    val sensorTimestampMillis: Long? = null,
) {
    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    val hasAltitude: Boolean
        get() = gpsAltitudeMeters != null

    val hasPressure: Boolean
        get() = pressureHpa != null

    val hasHeading: Boolean
        get() = headingDegrees != null

    val headingDirectionName: String?
        get() = headingDegrees?.let { degrees ->
            when (((degrees + 22.5f) / 45f).toInt() % 8) {
                0 -> "Norte"
                1 -> "Nororiente"
                2 -> "Oriente"
                3 -> "Suroriente"
                4 -> "Sur"
                5 -> "Suroccidente"
                6 -> "Occidente"
                else -> "Noroccidente"
            }
        }

    val headingCardinal: String?
        get() = headingDegrees?.let { degrees ->
            when (((degrees + 22.5f) / 45f).toInt() % 8) {
                0 -> "N"
                1 -> "NE"
                2 -> "E"
                3 -> "SE"
                4 -> "S"
                5 -> "SO"
                6 -> "O"
                else -> "NO"
            }
        }
}
