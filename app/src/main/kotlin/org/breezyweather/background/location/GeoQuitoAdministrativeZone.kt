package org.breezyweather.background.location

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object GeoQuitoAdministrativeZone {
    private const val ENDPOINT =
        "https://geoquito.quito.gob.ec/server/rest/services/web_reference_dmot/division_politica_administrativa/MapServer/1/query"

    suspend fun find(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            if (!latitude.isFinite() || !longitude.isFinite()) return@withContext null
            if (latitude !in -0.8..0.3 || longitude !in -79.5..-77.8) return@withContext null

            val geometry = URLEncoder.encode("$longitude,$latitude", "UTF-8")
            val url = "$ENDPOINT?geometry=$geometry" +
                "&geometryType=esriGeometryPoint&inSR=4326" +
                "&spatialRel=esriSpatialRelIntersects" +
                "&outFields=adm_zonal&returnGeometry=false&f=json"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            try {
                if (connection.responseCode != 200) return@withContext null
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val features = JSONObject(response).optJSONArray("features")
                    ?: return@withContext null
                if (features.length() != 1) return@withContext null
                features.optJSONObject(0)
                    ?.optJSONObject("attributes")
                    ?.optString("adm_zonal")
                    ?.takeIf { it.isNotBlank() }
            } finally {
                connection.disconnect()
            }
        }
}
