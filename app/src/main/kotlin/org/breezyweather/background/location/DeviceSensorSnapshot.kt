package org.breezyweather.background.location

data class DeviceSensorSnapshot(
    val pressureHpa: Float? = null,
    val accelerationX: Float? = null,
    val accelerationY: Float? = null,
    val accelerationZ: Float? = null,
    val gyroscopeX: Float? = null,
    val gyroscopeY: Float? = null,
    val gyroscopeZ: Float? = null,
    val headingDegrees: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val hasPressure: Boolean
        get() = pressureHpa != null

    val hasMotion: Boolean
        get() = accelerationX != null || accelerationY != null || accelerationZ != null ||
            gyroscopeX != null || gyroscopeY != null || gyroscopeZ != null

    val hasHeading: Boolean
        get() = headingDegrees != null
}
