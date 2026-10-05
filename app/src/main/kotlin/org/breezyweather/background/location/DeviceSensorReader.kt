package org.breezyweather.background.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI

class DeviceSensorReader(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _snapshot = MutableStateFlow(DeviceSensorSnapshot())
    val snapshot: StateFlow<DeviceSensorSnapshot> = _snapshot.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true

        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val current = _snapshot.value

        _snapshot.value = when (event.sensor.type) {
            Sensor.TYPE_PRESSURE -> current.copy(
                pressureHpa = event.values.firstOrNull(),
                timestampMillis = System.currentTimeMillis(),
            )

            Sensor.TYPE_ACCELEROMETER -> current.copy(
                accelerationX = event.values.getOrNull(0),
                accelerationY = event.values.getOrNull(1),
                accelerationZ = event.values.getOrNull(2),
                timestampMillis = System.currentTimeMillis(),
            )

            Sensor.TYPE_GYROSCOPE -> current.copy(
                gyroscopeX = event.values.getOrNull(0),
                gyroscopeY = event.values.getOrNull(1),
                gyroscopeZ = event.values.getOrNull(2),
                timestampMillis = System.currentTimeMillis(),
            )

            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                var heading = (orientation[0] * 180.0 / PI).toFloat()
                if (heading < 0f) heading += 360f

                current.copy(
                    headingDegrees = heading,
                    timestampMillis = System.currentTimeMillis(),
                )
            }

            else -> current
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
