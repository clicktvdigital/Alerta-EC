package org.breezyweather.background.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.SourceManager
import javax.inject.Inject

class DetailedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceManager: SourceManager,
) {

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
        val source = sourceManager.getReverseGeocodingSourceOrDefault(sourceId)

        return source.requestNearestLocation(
            context = context,
            latitude = latitude,
            longitude = longitude,
        ).map { results ->
            results.firstOrNull()?.let(EcuadorAddressMapper::fromAddressInfo)
                ?: DetailedLocationAddress()
        }.map { address -> applyUserVerifiedZone(latitude, longitude, address) }
    }
}
