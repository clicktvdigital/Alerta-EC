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

package org.breezyweather.background.updater.data

import org.breezyweather.background.updater.model.Release
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

/**
 * Taken from Mihon
 * Apache License, Version 2.0
 *
 * https://github.com/mihonapp/mihon/blob/02864ebd60ac9eb974a1b54b06368d20b0ca3ce5/data/src/main/java/tachiyomi/data/release/ReleaseServiceImpl.kt
 */
class ReleaseService @Inject constructor(
    @Named("JsonClient") val client: Retrofit.Builder,
) {

    /**
     * ALERTA EC publishes every signed build on its download site (GitHub Pages, version.json), not as a
     * GitHub Release, so that is the source of truth; GitHub Releases is only the fallback.
     */
    suspend fun latest(org: String, repository: String): Release =
        runCatching { fromDownloadSite() }.getOrElse { fromGithub(org, repository) }

    private fun fromDownloadSite(): Release {
        val connection = (java.net.URL("$DOWNLOAD_SITE_URL/version.json").openConnection() as java.net.HttpURLConnection)
            .apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                useCaches = false
            }
        val body = try {
            check(connection.responseCode == 200) { "version.json HTTP ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val json = org.json.JSONObject(body)
        val version = json.getString("version")
        val profiles = json.getJSONArray("perfiles")
        val pages = (0 until profiles.length()).associate {
            profiles.getJSONObject(it).let { p -> p.getString("perfil") to p.getString("pagina") }
        }
        val apks = (0 until profiles.length()).map { profiles.getJSONObject(it).getString("apk") }
        val isTv = runCatching {
            org.breezyweather.BreezyWeather.instance.packageManager
                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        }.getOrDefault(false)
        val profile = when {
            isTv -> "tv"
            android.os.Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a" -> "celular"
            else -> "celular-v7"
        }
        return Release(
            version = version,
            info = "",
            releaseLink = pages[profile] ?: "$DOWNLOAD_SITE_URL/",
            assets = apks,
            versionCode = json.optInt("version_code", 0).takeIf { it > 0 },
        )
    }

    private suspend fun fromGithub(org: String, repository: String): Release {
        return client
            .baseUrl("https://api.github.com/")
            .build()
            .create(GithubApi::class.java)
            .getReleases(org, repository)
            .firstOrNull { !it.draft }
            ?.let(releaseMapper)
            ?: error("No hay publicaciones disponibles en GitHub")
    }

    companion object {
        const val DOWNLOAD_SITE_URL = "https://clicktvdigital.github.io/Alerta-EC"
    }
}
