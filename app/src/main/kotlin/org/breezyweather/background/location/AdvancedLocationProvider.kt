package org.breezyweather.background.location

object AdvancedLocationProvider {

    fun combine(
        location: ActiveLocationSnapshot?,
        sensors: DeviceSensorSnapshot,
    ): AdvancedLocationSnapshot {
        return AdvancedLocationSnapshot(
            latitude = location?.latitude,
            longitude = location?.longitude,
            horizontalAccuracyMeters = location?.accuracy,
            gpsAltitudeMeters = location?.altitude,
            locationTimestampMillis = location?.timestamp,

            pressureHpa = sensors.pressureHpa,
            headingDegrees = sensors.headingDegrees,

            accelerationX = sensors.accelerationX,
            accelerationY = sensors.accelerationY,
            accelerationZ = sensors.accelerationZ,

            gyroscopeX = sensors.gyroscopeX,
            gyroscopeY = sensors.gyroscopeY,
            gyroscopeZ = sensors.gyroscopeZ,

            sensorTimestampMillis = sensors.timestampMillis,
        )
    }

    fun current(
        context: android.content.Context,
        sensors: DeviceSensorSnapshot,
    ): AdvancedLocationSnapshot {
        return combine(
            location = ActiveLocationController.getLatest(context),
            sensors = sensors,
        )
    }
}
