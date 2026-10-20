/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.sources.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.rx3.rxObservable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.breezyweather.common.exceptions.LocationAccessOffException
import org.breezyweather.common.exceptions.LocationException
import org.breezyweather.common.extensions.hasPermission
import org.breezyweather.common.source.LocationPositionWrapper
import org.breezyweather.common.source.LocationSource
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

@SuppressLint("MissingPermission")
class AndroidLocationService @Inject constructor() : LocationSource {

    override val id = "native"
    override val name = "Android"

    override fun requestLocation(context: Context): Observable<LocationPositionWrapper> = rxObservable {
        if (!hasPermissions(context)) throw LocationException()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(manager)) throw LocationAccessOffException()
        val fine = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            if (fine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { it in manager.allProviders && manager.isProviderEnabled(it) }
        if (providers.isEmpty()) throw LocationException()

        var callbackBest: Location? = null
        val measured = withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val requestedProviders = providers.toMutableSet()
                val respondedProviders = mutableSetOf<String>()
                val listener = object : LocationListenerCompat {
                    override fun onLocationChanged(location: Location) {
                        if (!isRecent(location) || !continuation.isActive) return
                        synchronized(requestedProviders) {
                            if (!continuation.isActive) return
                            if (isBetterLocation(location, callbackBest)) callbackBest = location
                            location.provider?.let(respondedProviders::add)
                            if (respondedProviders.containsAll(requestedProviders)) {
                                manager.removeUpdates(this)
                                continuation.resume(callbackBest)
                            }
                        }
                    }
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                val request = LocationRequestCompat.Builder(1000)
                    .setQuality(
                        if (fine) LocationRequestCompat.QUALITY_HIGH_ACCURACY
                        else LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
                    )
                    .build()
                providers.forEach { provider ->
                    try {
                        LocationManagerCompat.requestLocationUpdates(
                            manager, provider, request, listener, Looper.getMainLooper()
                        )
                    } catch (_: Exception) {
                        synchronized(requestedProviders) {
                            requestedProviders.remove(provider)
                        }
                    }
                }
                synchronized(requestedProviders) {
                    if (continuation.isActive) {
                        if (requestedProviders.isEmpty()) {
                            continuation.resume(null)
                        } else if (respondedProviders.containsAll(requestedProviders)) {
                            manager.removeUpdates(listener)
                            continuation.resume(callbackBest)
                        }
                    }
                }
            }
        }
        val fallbackCandidates = providers.mapNotNull { manager.getLastKnownLocation(it) } +
            listOfNotNull(callbackBest)
        val selected = measured ?: fallbackCandidates
            .filter { isRecent(it) }
            .maxWithOrNull(LOCATION_COMPARATOR)
            ?: throw LocationException()

        send(
            LocationPositionWrapper(
                latitude = selected.latitude,
                longitude = selected.longitude,
                altitude = if (selected.hasAltitude()) selected.altitude else null,
                accuracy = if (selected.hasAccuracy()) selected.accuracy else null,
                timestampMillis = selected.time,
                provider = selected.provider
            )
        )
    }

    override val permissions: Array<String>
        get() = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    companion object {
        private val TIMEOUT_MILLIS = 15.seconds.inWholeMilliseconds

        private val LOCATION_COMPARATOR = Comparator<Location> { first, second ->
            val firstElapsed = first.elapsedRealtimeNanos
            val secondElapsed = second.elapsedRealtimeNanos
            if (firstElapsed > 0L && secondElapsed > 0L && firstElapsed != secondElapsed) {
                firstElapsed.compareTo(secondElapsed)
            } else if (first.time != second.time) {
                first.time.compareTo(second.time)
            } else {
                val firstAccuracy = if (first.hasAccuracy()) first.accuracy else Float.MAX_VALUE
                val secondAccuracy = if (second.hasAccuracy()) second.accuracy else Float.MAX_VALUE
                secondAccuracy.compareTo(firstAccuracy)
            }
        }

        private fun isBetterLocation(candidate: Location, current: Location?): Boolean =
            current == null || LOCATION_COMPARATOR.compare(candidate, current) > 0

        private fun isRecent(location: Location): Boolean {
            val nowNanos = SystemClock.elapsedRealtimeNanos()
            val ageNanos = nowNanos - location.elapsedRealtimeNanos
            val monotonicRecent = location.elapsedRealtimeNanos > 0L &&
                ageNanos in 0..120_000_000_000L
            val wallAge = System.currentTimeMillis() - location.time
            val wallRecent = location.time > 0L && wallAge in 0..120_000L
            return (monotonicRecent || wallRecent) &&
                location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
                location.longitude.isFinite() && location.longitude in -180.0..180.0
        }
    }
}
