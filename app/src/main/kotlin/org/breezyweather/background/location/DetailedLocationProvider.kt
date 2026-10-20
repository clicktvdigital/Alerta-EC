package org.breezyweather.background.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import org.breezyweather.sources.SourceManager
import javax.inject.Inject

class DetailedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceManager: SourceManager,
) {
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
        }.flatMapSingle { address ->
            Single.fromCallable {
                val zone = runCatching {
                    runBlocking { GeoQuitoAdministrativeZone.find(latitude, longitude) }
                }.getOrNull()
                val parish = runCatching {
                    runBlocking { GeoQuitoParish.find(latitude, longitude) }
                }.getOrNull()
                address.copy(
                    administrativeZone = zone ?: address.administrativeZone,
                    parish = parish?.parish ?: address.parish,
                    canton = parish?.canton ?: address.canton,
                    province = parish?.province ?: address.province,
                )
            }.subscribeOn(Schedulers.io())
        }
    }
}
