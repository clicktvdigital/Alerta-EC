package org.breezyweather.background.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

data class DeviceSensorCapabilities(
    val pressure: Boolean,
    val accelerometer: Boolean,
    val gyroscope: Boolean,
    val magneticField: Boolean,
    val rotationVector: Boolean,
)

class DeviceSensorMonitor(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val pressureSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val accelerometerSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val gyroscopeSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val magneticFieldSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val rotationVectorSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun capabilities() = DeviceSensorCapabilities(
        pressure = pressureSensor != null,
        accelerometer = accelerometerSensor != null,
        gyroscope = gyroscopeSensor != null,
        magneticField = magneticFieldSensor != null,
        rotationVector = rotationVectorSensor != null,
    )
}
