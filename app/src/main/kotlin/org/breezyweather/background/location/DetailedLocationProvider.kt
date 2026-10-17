package org.breezyweather.background.location

import android.content.Context
import breezyweather.domain.location.model.LocationAddressInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
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
            val address: LocationAddressInfo = results.firstOrNull()
                ?: throw IllegalStateException("No reverse-geocoding result")

            EcuadorAddressMapper.fromAddressInfo(address)
        }
    }
}
