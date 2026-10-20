package org.breezyweather.background.location

import android.location.Location
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object NearbyLandmarksProvider {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    private val mutex = Mutex()
    private var cacheLatitude = Double.NaN
    private var cacheLongitude = Double.NaN
    private var cacheTime = 0L
    private var cachedResults: List<NearbyLandmark> = emptyList()


    suspend fun find(latitude: Double, longitude: Double): List<NearbyLandmark> =
        mutex.withLock {
            val now = SystemClock.elapsedRealtime()
            if (latitude.isFinite() && longitude.isFinite() &&
                cacheLatitude.isFinite() && cacheLongitude.isFinite() &&
                now - cacheTime in 0..900_000L) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    latitude, longitude,
                    cacheLatitude, cacheLongitude,
                    distance
                )
                if (distance[0] < 150f) return@withLock cachedResults
            }
            val results = fetch(latitude, longitude)
            cacheLatitude = latitude
            cacheLongitude = longitude
            cacheTime = SystemClock.elapsedRealtime()
            cachedResults = results
            results
        }

    private suspend fun fetch(latitude: Double, longitude: Double): List<NearbyLandmark> =
        withContext(Dispatchers.IO) {
            if (!latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                return@withContext emptyList()
            }

            val query = "[out:json][timeout:20];(" +
                "nwr[shop][name](around:300,$latitude,$longitude);" +
                "nwr[craft][name](around:300,$latitude,$longitude);" +
                ");out center 40;"
            val url = ENDPOINT + "?data=" + URLEncoder.encode(query, "UTF-8")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 25000
            try {
                if (connection.responseCode != 200) return@withContext emptyList()
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val elements = JSONObject(body).optJSONArray("elements")
                    ?: return@withContext emptyList()
                val results = mutableListOf<NearbyLandmark>()
                for (i in 0 until elements.length()) {
                    val item = elements.optJSONObject(i) ?: continue
                    val tags = item.optJSONObject("tags") ?: continue
                    val name = tags.optString("name").trim()
                    if (name.isBlank()) continue
                    val center = item.optJSONObject("center")
                    val lat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat")
                    val lon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon")
                    if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite()) continue
                    val distance = FloatArray(1)
                    Location.distanceBetween(latitude, longitude, lat, lon, distance)
                    results.add(
                        NearbyLandmark(
                            name = name,
                            category = tags.optString("shop").takeIf { it.isNotBlank() }
                                ?: tags.optString("craft"),
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = distance[0].toInt(),
                        )
                    )
                }
                results.sortedBy { it.distanceMeters }.distinctBy {
                    it.name.lowercase() to (it.distanceMeters / 15)
                }.take(10)
            } finally {
                connection.disconnect()
            }
        }
}
