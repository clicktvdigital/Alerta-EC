package org.breezyweather.alertaec.community

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.breezyweather.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Community weather reports, comments and reviews in the ALERTA EC Supabase project
 * (`bnnpdwrduktchmgbwlha`), through its REST API (PostgREST) with the PUBLIC key only.
 *
 * - The key comes from the build (`ALERTAEC_SUPABASE_ANON_KEY`, a GitHub secret / gradle property).
 *   A `service_role` JWT or an `sb_secret_` key is refused at runtime: the feature stays off.
 * - Row Level Security on the server decides what anonymous users may insert and read
 *   (see docs/supabase/alerta_ec_comunidad.sql).
 * - Privacy: no account, no device id, no name; coordinates are only sent when the user ticks the GPS box,
 *   and then rounded to 0.01° (about 1 km).
 */
object AlertaEcCommunity {
    const val PROJECT_URL = "https://bnnpdwrduktchmgbwlha.supabase.co"
    private const val TIMEOUT_MS = 12_000

    data class Report(
        val id: Long,
        val severity: String,
        val category: String,
        val description: String,
        val latitude: Double?,
        val longitude: Double?,
        val createdAt: String,
    )

    sealed class Result<out T> {
        data class Ok<T>(val value: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val key: String get() = BuildConfig.ALERTAEC_SUPABASE_ANON_KEY.trim()

    /** True only with a public key (anon JWT or sb_publishable_). */
    val isConfigured: Boolean get() = isPublicKey(key)

    internal fun isPublicKey(value: String): Boolean {
        if (value.isEmpty() || value.startsWith("sb_secret_")) return false
        if (value.startsWith("sb_publishable_")) return true
        val parts = value.split('.')
        if (parts.size != 3) return false
        val payload = runCatching {
            String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        }.getOrNull() ?: return false
        return runCatching { JSONObject(payload).optString("role") == "anon" }.getOrDefault(false)
    }

    private fun round(value: Double?): Double? = value?.takeIf { it.isFinite() }?.let {
        String.format(Locale.US, "%.2f", it).toDouble()
    }

    /** Severity scale shown in the report form (stored as text, checked by the database). */
    val SEVERITIES = listOf("Leve", "Moderada", "Alta", "Crítica")

    /** Reasons to flag a report; three flags hide it until someone reviews it. */
    val FLAG_REASONS = listOf(
        "Información falsa o engañosa",
        "Muestra rostros, placas o documentos",
        "Lenguaje ofensivo",
        "Publicidad o spam",
        "Otro motivo",
    )

    suspend fun publishReport(
        category: String,
        severity: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<Unit> = post(
        "community_reports",
        JSONObject().apply {
            put("category", category.take(60))
            put("severity", severity.takeIf { it in SEVERITIES } ?: "Leve")
            put("description", description.trim().take(1000))
            round(latitude)?.let { put("latitude", it) }
            round(longitude)?.let { put("longitude", it) }
            put("app_version", BuildConfig.VERSION_NAME.take(40))
        }
    )

    suspend fun flagReport(reportId: Long, reason: String): Result<Unit> = post(
        "report_flags",
        JSONObject().apply {
            put("report_id", reportId)
            put("reason", reason.take(60))
        }
    )

    suspend fun publishReview(stars: Int, comment: String): Result<Unit> = post(
        "app_reviews",
        JSONObject().apply {
            put("stars", stars.coerceIn(1, 5))
            put("comment", comment.trim().take(1000))
            put("app_version", BuildConfig.VERSION_NAME.take(40))
        }
    )

    /** Reports of the last [hours] hours, nearest area first when coordinates are known (±0.5°, ~55 km). */
    suspend fun recentReports(latitude: Double?, longitude: Double?, hours: Long = 12): Result<List<Report>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.Error(NOT_CONFIGURED)
            val since = Instant.now().minusSeconds(hours * 3600).toString()
            val filters = buildString {
                append("select=id,severity,category,description,latitude,longitude,created_at")
                append("&created_at=gte.").append(URLEncoder.encode(since, "UTF-8"))
                if (latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite()) {
                    append(String.format(Locale.US, "&latitude=gte.%.2f&latitude=lte.%.2f", latitude - 0.5, latitude + 0.5))
                    append(String.format(Locale.US, "&longitude=gte.%.2f&longitude=lte.%.2f", longitude - 0.5, longitude + 0.5))
                }
                append("&order=created_at.desc&limit=30")
            }
            request("GET", "community_reports?$filters", null).let { response ->
                when (response) {
                    is Result.Error -> response
                    is Result.Ok -> runCatching {
                        val array = JSONArray(response.value)
                        Result.Ok(
                            (0 until array.length()).map { i ->
                                val o = array.getJSONObject(i)
                                Report(
                                    id = o.optLong("id"),
                                    severity = o.optString("severity", "Leve"),
                                    category = o.optString("category"),
                                    description = o.optString("description"),
                                    latitude = o.optDouble("latitude").takeIf { it.isFinite() },
                                    longitude = o.optDouble("longitude").takeIf { it.isFinite() },
                                    createdAt = o.optString("created_at"),
                                )
                            }
                        )
                    }.getOrElse { Result.Error("Respuesta no válida del servidor comunitario") }
                }
            }
        }

    private suspend fun post(table: String, body: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.Error(NOT_CONFIGURED)
        when (val response = request("POST", table, body.toString())) {
            is Result.Ok -> Result.Ok(Unit)
            is Result.Error -> response
        }
    }

    private fun request(method: String, path: String, body: String?): Result<String> {
        val connection = try {
            (URL("$PROJECT_URL/rest/v1/$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("apikey", key)
                if (!key.startsWith("sb_publishable_")) setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                }
            }
        } catch (e: Exception) {
            return Result.Error("No se pudo preparar la conexión")
        }
        return try {
            body?.let { connection.outputStream.use { out -> out.write(it.toByteArray()) } }
            val code = connection.responseCode
            if (code in 200..299) {
                Result.Ok(connection.inputStream.bufferedReader().use { it.readText() })
            } else {
                Result.Error(
                    when (code) {
                        401, 403 -> "El servidor comunitario rechazó la solicitud (permisos RLS o clave pública)"
                        404 -> "La tabla comunitaria aún no existe en Supabase"
                        409, 422, 400 -> "El servidor rechazó el contenido (revisa la longitud del texto)"
                        429 -> "Demasiados envíos seguidos; inténtalo en unos minutos"
                        else -> "Servidor comunitario no disponible (código $code)"
                    }
                )
            }
        } catch (e: java.io.IOException) {
            Result.Error("Sin conexión con el servidor comunitario. Comprueba Internet.")
        } finally {
            connection.disconnect()
        }
    }

    const val NOT_CONFIGURED = "La comunidad ALERTA EC aún no tiene configurada su clave pública en esta compilación."
}
