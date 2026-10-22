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
        }
    }
}
