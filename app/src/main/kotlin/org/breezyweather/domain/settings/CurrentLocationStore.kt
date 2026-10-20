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

package org.breezyweather.domain.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

/**
 * Store the current location independently from the current location, so that in a future update, we could have
 * multiple current locations with different sources
 */
class CurrentLocationStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val config: ConfigStore = ConfigStore(context, CURRENT_LOCATION_STORE)

    // Separate keys preserve compatibility with the former Float preferences.
    val lastKnownLongitude: Double
        get() = config.getString("longitude_double", null)?.toDoubleOrNull()
            ?: config.getFloat(KEY_LAST_KNOWN_LONGITUDE, 0f).toDouble()

    val lastKnownLatitude: Double
        get() = config.getString("latitude_double", null)?.toDoubleOrNull()
            ?: config.getFloat(KEY_LAST_KNOWN_LATITUDE, 0f).toDouble()

    val accuracyMeters: Float?
        get() = config.getFloat("accuracy_meters", -1f).takeIf { it.isFinite() && it >= 0f }
    val altitudeMeters: Double?
        get() = config.getString("altitude_meters", null)?.toDoubleOrNull()?.takeIf { it.isFinite() }
    val measuredAtMillis: Long?
        get() = config.getLong("measured_at", 0).takeIf { it > 0 }
    val provider: String?
        get() = config.getString("provider", null)
    val lastSuccessfulRefresh: Long
        get() = config.getLong(KEY_LAST_SUCCESSFUL_REFRESH, 0)

    val isRecent: Boolean
        get() {
            val measured = measuredAtMillis ?: return false
            val age = System.currentTimeMillis() - measured
            return isUsable && age >= 0L && age <= 120_000L
        }

    val isUsable: Boolean
        get() = lastSuccessfulRefresh > 0 && lastKnownLatitude.isFinite() && lastKnownLongitude.isFinite() &&
            lastKnownLatitude in -90.0..90.0 && lastKnownLongitude in -180.0..180.0

    fun updateCurrentLocation(
        longitude: Double,
        latitude: Double,
        accuracy: Float? = null,
        altitude: Double? = null,
        timestampMillis: Long? = null,
        provider: String? = null,
    ) {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        synchronized(LOCATION_WRITE_LOCK) {
            val incomingTimestamp = timestampMillis?.takeIf { it > 0L }
            val storedTimestamp = runCatching {
                config.getLong("measured_at", 0L)
            }.getOrDefault(0L)
            if (incomingTimestamp == null && storedTimestamp > 0L) return
            if (incomingTimestamp != null && storedTimestamp > 0L && incomingTimestamp < storedTimestamp) return
            config.edit()
                .putString("longitude_double", longitude.toString())
                .putString("latitude_double", latitude.toString())
                .putFloat("accuracy_meters", accuracy?.takeIf { it.isFinite() && it >= 0f } ?: -1f)
                .putString("altitude_meters", altitude?.takeIf { it.isFinite() }?.toString())
                .putLong("measured_at", incomingTimestamp ?: 0L)
                .putString("provider", provider)
                .putLong(KEY_LAST_SUCCESSFUL_REFRESH, Date().time)
                .apply()
        }
    }

    fun clearCurrentLocation() {
        synchronized(LOCATION_WRITE_LOCK) {
            config.edit().clear().apply()
        }
    }

    companion object {
        private val LOCATION_WRITE_LOCK = Any()
        private const val CURRENT_LOCATION_STORE = "current_location"
        private const val KEY_LAST_SUCCESSFUL_REFRESH = "last_successful_refresh"
        private const val KEY_LAST_KNOWN_LONGITUDE = "last_known_longitude"
        private const val KEY_LAST_KNOWN_LATITUDE = "last_known_latitude"
    }
}
