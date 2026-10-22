package org.breezyweather.background.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

data class ActiveLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val timestamp: Long,
    val verticalAccuracyMeters: Float? = null,
)

object ActiveLocationController {

    private const val PREFS_NAME = "alerta_ec_active_location"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_ALTITUDE = "altitude"
    private const val KEY_ACCURACY = "accuracy"
    private const val KEY_VERTICAL_ACCURACY = "vertical_accuracy"
    private const val KEY_TIMESTAMP = "timestamp"

    fun start(context: Context) {
        val intent = Intent(context, ActiveLocationService::class.java).apply {
            action = ActiveLocationService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, ActiveLocationService::class.java))
    }

    fun getLatest(context: Context): ActiveLocationSnapshot? {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latitude = preferences.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        val timestamp = preferences.getLong(KEY_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (timestamp <= 0L || timestamp > now + 10_000L) return null
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null

        val altitude = preferences.getString(KEY_ALTITUDE, null)
            ?.takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
        val storedAccuracy = preferences.getFloat(KEY_ACCURACY, -1f)
        val accuracy = storedAccuracy.takeIf { it.isFinite() && it >= 0f }

        return ActiveLocationSnapshot(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy,
            timestamp = timestamp,
            verticalAccuracyMeters = preferences.getFloat(KEY_VERTICAL_ACCURACY, -1f)
                .takeIf { it.isFinite() && it >= 0f },
        )
    }
}
