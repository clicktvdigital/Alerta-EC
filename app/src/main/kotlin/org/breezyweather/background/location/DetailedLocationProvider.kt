package org.breezyweather.background.location

import android.content.Context
import android.location.Location as AndroidLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.SourceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceManager: SourceManager,
) {
    private data class CacheEntry(
        val latitude: Double,
        val longitude: Double,
        val sourceId: String,
        val address: DetailedLocationAddress,
        val timestamp: Long,
    )

    private val cache = mutableListOf<CacheEntry>()
    private val cacheLifetimeMs = 30 * 60 * 1000L

    @Synchronized
    fun getCached(latitude: Double, longitude: Double, sourceId: String = "nominatim"): DetailedLocationAddress? {
        val now = android.os.SystemClock.elapsedRealtime()
        cache.removeAll { now - it.timestamp > cacheLifetimeMs }
        return cache.lastOrNull {
            it.sourceId == sourceId &&
                FloatArray(1).also { distance ->
                    AndroidLocation.distanceBetween(latitude, longitude, it.latitude, it.longitude, distance)
                }[0] <= 30f
        }?.address
    }

    @Synchronized
    private fun remember(latitude: Double, longitude: Double, sourceId: String, address: DetailedLocationAddress) {
        if (address.completeAddressLines.isEmpty()) return
        cache.add(CacheEntry(latitude, longitude, sourceId, address, android.os.SystemClock.elapsedRealtime()))
        while (cache.size > 32) cache.removeAt(0)
    }


    // La corrección del usuario es LOCAL a un punto; no se aplica a todo Quito.
    // Nunca se presenta como dirección certificada por el geocodificador.
    private fun applyUserVerifiedZone(
        latitude: Double, longitude: Double, result: DetailedLocationAddress
    ): DetailedLocationAddress {
        val prefs = context.getSharedPreferences("alerta_ec_direccion_usuario", Context.MODE_PRIVATE)
        val storedLat = prefs.getString("lat", null)?.toDoubleOrNull() ?: return result
        val storedLon = prefs.getString("lon", null)?.toDoubleOrNull() ?: return result
        if (!storedLat.isFinite() || !storedLon.isFinite() ||
            !latitude.isFinite() || !longitude.isFinite()) return result
        val distance = FloatArray(1)
        android.location.Location.distanceBetween(storedLat, storedLon, latitude, longitude, distance)
        if (distance[0] > 75f) return result
        val postal = prefs.getString("postal", null)?.trim().orEmpty()
        val sector = prefs.getString("sector", null)?.trim().orEmpty()
        if (!Regex("[0-9]{6}").matches(postal) || sector.isBlank()) return result
        return result.copy(
            sector = sector,
            postalCode = postal,
            postalCodeProvenance = "Aportado por el usuario (ubicación cercana)"
        )
    }

    fun request(
        latitude: Double,
        longitude: Double,
        sourceId: String = "nominatim",
    ): Observable<DetailedLocationAddress> {
        getCached(latitude, longitude, sourceId)?.let { return Observable.just(applyUserVerifiedZone(latitude, longitude, it)) }
        val source = sourceManager.getReverseGeocodingSourceOrDefault(sourceId)

        return source.requestNearestLocation(
            context = context,
            latitude = latitude,
            longitude = longitude,
        ).map { results ->
            val address = results.firstOrNull()?.let(EcuadorAddressMapper::fromAddressInfo)
                ?: DetailedLocationAddress()
            remember(latitude, longitude, sourceId, address)
            address
        }.map { address -> applyUserVerifiedZone(latitude, longitude, address) }
    }
}
