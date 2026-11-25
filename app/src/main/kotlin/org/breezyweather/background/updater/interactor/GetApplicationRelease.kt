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

package org.breezyweather.background.updater.interactor

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.breezyweather.background.updater.data.ReleaseService
import org.breezyweather.background.updater.model.Release
import org.breezyweather.domain.settings.SettingsManager
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Taken from Mihon
 * Apache License, Version 2.0
 *
 * https://github.com/mihonapp/mihon/blob/aa498360db90350f2642e6320dc55e7d474df1fd/domain/src/main/java/tachiyomi/domain/release/interactor/GetApplicationRelease.kt
 */
class GetApplicationRelease @Inject constructor(
    @ApplicationContext val context: Context,
    val service: ReleaseService,
) {

    suspend fun await(
        arguments: Arguments,
    ): Result {
        val now = Date().time

        val lastChecked = SettingsManager.getInstance(context).appUpdateCheckLastTimestamp

        // Limit checks to once every day at most
        if (!arguments.forceCheck && now < lastChecked + 1.days.inWholeMilliseconds) {
            return Result.NoNewUpdate
        }

        val release = service.latest(arguments.org, arguments.repository)

        SettingsManager.getInstance(context).appUpdateCheckLastTimestamp = now

        // Check if latest version is different from current version
        val isNewVersion = isNewer(
            localName = arguments.versionName,
            localCode = arguments.versionCode,
            remoteName = release.version,
            remoteCode = release.versionCode,
        )
        return when {
            isNewVersion -> Result.NewUpdate(release)
            else -> Result.NoNewUpdate
        }
    }

    companion object {
        /**
         * The published versionCode decides when it is known (download site); otherwise the version names are
         * compared part by part ("v0.1.24", "0.1.24-r3" and "0.1.24" are the same version).
         */
        internal fun isNewer(localName: String, localCode: Int?, remoteName: String, remoteCode: Int?): Boolean {
            if (localCode != null && localCode > 0 && remoteCode != null && remoteCode > 0) {
                return remoteCode > localCode
            }
            val remote = numericParts(remoteName)
            val local = numericParts(localName)
            for (i in 0 until maxOf(remote.size, local.size)) {
                val r = remote.getOrElse(i) { 0 }
                val l = local.getOrElse(i) { 0 }
                if (r != l) return r > l
            }
            return false
        }

        private fun numericParts(version: String): List<Int> =
            version.trim().removePrefix("v").substringBefore('-').substringBefore('_')
                .split('.').mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
    }

    data class Arguments(
        val versionName: String,
        val org: String,
        val repository: String,
        val forceCheck: Boolean = false,
        val versionCode: Int? = null,
    )

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
        data object UnsupportedFeature : Result
    }
}
