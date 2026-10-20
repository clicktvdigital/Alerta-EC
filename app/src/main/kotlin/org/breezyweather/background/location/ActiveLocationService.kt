package org.breezyweather.background.location

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import org.breezyweather.R
import org.breezyweather.remoteviews.Notifications
import org.breezyweather.ui.main.MainActivity

class ActiveLocationService : Service(), LocationListenerCompat {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startForegroundMode()
            if (!startTracking()) return START_NOT_STICKY
        } catch (_: SecurityException) {
            stopTracking()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundMode() {
        val openIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ActiveLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_ACTIVE_LOCATION)
            .setSmallIcon(R.drawable.ic_running_in_background)
            .setContentTitle(getString(R.string.active_location_notification_title))
            .setContentText(getString(R.string.active_location_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.active_location_notification_stop), stopPendingIntent)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
    }

    private fun startTracking(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val provider = buildList {
            if (fine) add(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.firstOrNull { it in locationManager.allProviders && locationManager.isProviderEnabled(it) }
        if (provider == null) {
            stopTracking()
            return false
        }

        val request = LocationRequestCompat.Builder(UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setQuality(
                if (fine) LocationRequestCompat.QUALITY_HIGH_ACCURACY
                else LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
            )
            .build()

        LocationManagerCompat.requestLocationUpdates(
            locationManager,
            provider,
            request,
            this,
            Looper.getMainLooper()
        )
        return true
    }

    override fun onLocationChanged(location: Location) {
        if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
            !location.longitude.isFinite() || location.longitude !in -180.0..180.0 ||
            location.time <= 0L || location.time > System.currentTimeMillis() + 10_000L
        ) return
        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val previousTimestamp = preferences.getLong(KEY_TIMESTAMP, 0L)
        if (previousTimestamp > 0L &&
            previousTimestamp <= System.currentTimeMillis() + 10_000L &&
            location.time < previousTimestamp
        ) return
        preferences.edit()
            .putString(KEY_LATITUDE, location.latitude.toString())
            .putString(KEY_LONGITUDE, location.longitude.toString())
            .putString(KEY_ALTITUDE, if (location.hasAltitude() && location.altitude.isFinite()) location.altitude.toString() else "")
            .putFloat(KEY_ACCURACY, if (location.hasAccuracy() && location.accuracy.isFinite() && location.accuracy >= 0f) location.accuracy else -1f)
            .putFloat(KEY_VERTICAL_ACCURACY, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy() && location.verticalAccuracyMeters.isFinite() && location.verticalAccuracyMeters >= 0f) location.verticalAccuracyMeters else -1f)
            .putLong(KEY_TIMESTAMP, location.time)
            .apply()
    }

    private fun stopTracking() {
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        super.onDestroy()
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.clicktvdigital.alertaec.action.START_ACTIVE_LOCATION"
        const val ACTION_STOP = "com.clicktvdigital.alertaec.action.STOP_ACTIVE_LOCATION"

        private const val NOTIFICATION_ID = 4101
        private const val UPDATE_INTERVAL_MS = 5_000L
        private const val MIN_UPDATE_INTERVAL_MS = 2_000L

        private const val PREFS_NAME = "alerta_ec_active_location"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_ALTITUDE = "altitude"
        private const val KEY_ACCURACY = "accuracy"
        private const val KEY_VERTICAL_ACCURACY = "vertical_accuracy"
        private const val KEY_TIMESTAMP = "timestamp"
    }
}
