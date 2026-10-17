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

        val measured = withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<Location> { continuation ->
                val listener = object : LocationListenerCompat {
                    override fun onLocationChanged(location: Location) {
                        if (isRecent(location) && continuation.isActive) {
                            manager.removeUpdates(this)
                            continuation.resume(location)
                        }
                    }
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                try {
                    LocationManagerCompat.requestLocationUpdates(
                        manager,
                        providers.first(),
                        LocationRequestCompat.Builder(1000)
                            .setQuality(
                                if (fine) LocationRequestCompat.QUALITY_HIGH_ACCURACY
                                else LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
                            )
                            .build(),
                        listener,
                        Looper.getMainLooper()
                    )
                    if (!continuation.isActive) manager.removeUpdates(listener)
                } catch (e: Exception) {
                    manager.removeUpdates(listener)
                    continuation.cancel(e)
                }
            }
        } ?: providers.mapNotNull { manager.getLastKnownLocation(it) }
            .filter { isRecent(it) }
            .maxByOrNull { it.elapsedRealtimeNanos }
            ?: throw LocationException()

        send(
            LocationPositionWrapper(
                latitude = measured.latitude,
                longitude = measured.longitude,
                altitude = if (measured.hasAltitude()) measured.altitude else null,
                accuracy = if (measured.hasAccuracy()) measured.accuracy else null,
                timestampMillis = measured.time,
                provider = measured.provider
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

        private fun isRecent(location: Location): Boolean {
            val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
            return ageNanos in 0..120_000_000_000L &&
                location.latitude.isFinite() && location.latitude in -90.0..90.0 &&
                location.longitude.isFinite() && location.longitude in -180.0..180.0
        }
    }
}
