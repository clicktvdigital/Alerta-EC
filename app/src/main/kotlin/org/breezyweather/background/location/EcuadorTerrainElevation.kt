package org.breezyweather.background.location

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class TerrainElevation(
    val meters: Double,
    val source: String,
)

internal object EcuadorTerrainElevation {
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L
    private const val FAILURE_RETRY_MS = 5 * 60 * 1000L
    private const val CACHE_DISTANCE_METERS = 100f

    private var cachedLatitude: Double? = null
    private var cachedLongitude: Double? = null
    private var cachedResult: TerrainElevation? = null
    private var cachedAtMillis = 0L
    private val cacheMutex = Mutex()

    suspend fun find(latitude: Double, longitude: Double): TerrainElevation? =
        withContext(Dispatchers.IO) {
            if (!latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                return@withContext null
            }
            cacheMutex.withLock {
                val now = android.os.SystemClock.elapsedRealtime()
                val previousLat = cachedLatitude
                val previousLon = cachedLongitude
                if (previousLat != null && previousLon != null && cachedAtMillis > 0L) {
                    val distance = FloatArray(1)
                    android.location.Location.distanceBetween(
                        previousLat, previousLon, latitude, longitude, distance
                    )
                    val duration = if (cachedResult == null) FAILURE_RETRY_MS else CACHE_DURATION_MS
                    if (distance[0] < CACHE_DISTANCE_METERS &&
                        now >= cachedAtMillis && now - cachedAtMillis < duration) {
                        return@withLock cachedResult
                    }
                }
                val result = requestElevation(latitude, longitude)
                currentCoroutineContext().ensureActive()
                cachedLatitude = latitude
                cachedLongitude = longitude
                cachedResult = result
                cachedAtMillis = android.os.SystemClock.elapsedRealtime()
                result
            }
        }

    private suspend fun requestElevation(latitude: Double, longitude: Double): TerrainElevation? =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/elevation" +
                "?latitude=$latitude&longitude=$longitude"
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                try {
                    if (connection.responseCode != 200) return@withContext null
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    currentCoroutineContext().ensureActive()
                    val elevations = JSONObject(body).optJSONArray("elevation")
                        ?: return@withContext null
                    if (elevations.length() != 1 || elevations.isNull(0)) return@withContext null
                    val meters = elevations.optDouble(0, Double.NaN)
                    if (!meters.isFinite() || meters < -500 || meters > 9000) {
                        return@withContext null
                    }
                    TerrainElevation(meters = meters, source = "Open-Meteo Elevation API")
                } finally {
                    connection.disconnect()
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("AlertaEcElevation", "Elevación del terreno no disponible", error)
                null
            }
        }
}
