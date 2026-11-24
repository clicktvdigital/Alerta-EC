package org.breezyweather.background.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.runBlocking
import org.breezyweather.sources.SourceManager
import javax.inject.Inject

class DetailedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceManager: SourceManager,
) {

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
        val primarySource = sourceManager.getReverseGeocodingSourceOrDefault("nominatim")
        val fallbackSource = sourceManager.getReverseGeocodingSourceOrDefault("android")

        return primarySource.requestNearestLocation(
            context = context,
            latitude = latitude,
            longitude = longitude,
        ).onErrorResumeNext {
            fallbackSource.requestNearestLocation(context = context, latitude = latitude, longitude = longitude)
        }.flatMap { results ->
            val mapped = results.firstOrNull()?.let { info ->
                EcuadorAddressMapper.fromAddressInfo(info.copy(latitude = latitude, longitude = longitude))
            }
            val needsAndroidEnrichment = mapped == null ||
                mapped.road.isNullOrBlank() ||
                mapped.postalCode.isNullOrBlank() ||
                mapped.canton.isNullOrBlank()
            if (!needsAndroidEnrichment) {
                Observable.just(mapped!!)
            } else {
                fallbackSource.requestNearestLocation(context = context, latitude = latitude, longitude = longitude)
                    .map { fbResults ->
                        val fbMapped = fbResults.firstOrNull()?.let { info ->
                            EcuadorAddressMapper.fromAddressInfo(info.copy(latitude = latitude, longitude = longitude))
                        }
                        if (mapped == null) {
                            fbMapped ?: DetailedLocationAddress(latitude = latitude, longitude = longitude)
                        } else if (fbMapped == null) {
                            mapped
                        } else {
                            mapped.copy(
                                road = mapped.road ?: fbMapped.road,
                                houseNumber = mapped.houseNumber ?: fbMapped.houseNumber,
                                crossStreet = mapped.crossStreet ?: fbMapped.crossStreet,
                                postalCode = mapped.postalCode ?: fbMapped.postalCode,
                                sector = mapped.sector ?: fbMapped.sector,
                                neighborhood = mapped.neighborhood ?: fbMapped.neighborhood,
                                parish = mapped.parish ?: fbMapped.parish,
                                canton = mapped.canton ?: fbMapped.canton,
                                province = mapped.province ?: fbMapped.province,
                                administrativeZone = mapped.administrativeZone ?: fbMapped.administrativeZone,
                            )
                        }
                    }
                    .onErrorReturnItem(mapped ?: DetailedLocationAddress(latitude = latitude, longitude = longitude))
            }
        }.map { address ->
            val elevation = runCatching {
                runBlocking { EcuadorTerrainElevation.find(latitude, longitude)?.meters }
            }.getOrNull()
            val enriched = address.copy(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = elevation ?: address.altitudeMeters,
            )
            applyUserVerifiedZone(latitude, longitude, enriched)
        }
    }
}
