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

package org.breezyweather.background.updater.model

import android.os.Build
import org.breezyweather.BuildConfig

/*
 * Taken from Mihon
 * Apache License, Version 2.0
 *
 * https://github.com/mihonapp/mihon/blob/c83037eeab3b180c7b82355331131df6950f5d45/domain/src/main/java/tachiyomi/domain/release/model/Release.kt
 */

/**
 * Contains information about the latest release.
 */
data class Release(
    val version: String,
    val info: String,
    val releaseLink: String,
    private val assets: List<String>,
) {

    /**
     * Get download link of latest release from the assets.
     * @return download link of latest release.
     */
    fun getDownloadLink(): String? {
        // Solo APK del mismo flavor y arquitectura compatible para actualizar sobre la firma instalada.
        val isFreenet = BuildConfig.FLAVOR.equals("freenet", ignoreCase = true)
        val apks = assets.filter { url ->
            val fileName = url.substringAfterLast("/").substringBefore("?")
            fileName.endsWith(".apk", ignoreCase = true) &&
                url.startsWith(BuildConfig.GITHUB_RELEASE_PREFIX) &&
                (if (isFreenet) fileName.contains("freenet", ignoreCase = true) else !fileName.contains("freenet", ignoreCase = true))
        }
        for (abi in Build.SUPPORTED_ABIS.orEmpty()) {
            val match = apks.firstOrNull { url ->
                val fileName = url.substringAfterLast("/").substringBefore("?")
                fileName.contains(abi, ignoreCase = true)
            }
            if (match != null) return match
        }
        return if (apks.size == 1) apks.first() else null
    }

    /**
     * Assets class containing download url.
     */
    data class Assets(val downloadLink: String)
}
