package org.breezyweather.alertaec

import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * Pure, Android-free identity material for ALERTA EC events.
 *
 * SHA-256 without a secret key does not guarantee anonymity against dictionary
 * attacks. V2 only avoids putting source, coordinates, names, addresses,
 * custom names, or alert IDs in clear text; it is not a privacy primitive.
 */
object AlertaEcEventIdentityV2 {
    private const val VERSION = "aec-v2"

    data class LocationInput(val latitude: Double?, val longitude: Double?)

    data class ForecastInput(
        val source: String?,
        val location: LocationInput,
        val phenomenon: AlertaEcPhenomenon?,
        val startTimeMillis: Long?,
        val severity: AlertaEcSeverity? = null,
        val endTimeMillis: Long? = null,
    )

    data class OfficialInput(
        val effectiveSource: String?,
        val location: LocationInput,
        val alertId: String?,
        val severity: AlertaEcSeverity? = null,
        val startTimeMillis: Long? = null,
        val endTimeMillis: Long? = null,
        val instruction: String? = null,
    )

    sealed class Result {
        data class Valid(val id: String) : Result()
        data class Invalid(val reason: String) : Result()
    }

    fun forecast(input: ForecastInput): Result {
        val source = required(input.source) ?: return Result.Invalid("source vacío")
        val location = canonicalLocation(input.location) ?: return Result.Invalid("ubicación inválida")
        val phenomenon = input.phenomenon ?: return Result.Invalid("phenomenon ausente")
        val start = input.startTimeMillis
        validateTemporal(start, input.endTimeMillis, startRequired = true)?.let { return Result.Invalid(it) }
        val sourceHash = digest(fields("source", source))
        val locationHash = digest(fields("location", location))
        val eventHash = digest(fields("phenomenon", phenomenon.name, "start", start.toString()))
        val revisionHash = digest(
            fields(
                "severity", input.severity?.name,
                "start", start.toString(),
                "end", input.endTimeMillis?.toString(),
            ),
        )
        return Result.Valid("$VERSION-forecast-$sourceHash-$locationHash-$eventHash-$revisionHash")
    }

    fun official(input: OfficialInput): Result {
        val source = required(input.effectiveSource)
            ?: return Result.Invalid("effectiveSource vacío")
        val location = canonicalLocation(input.location) ?: return Result.Invalid("ubicación inválida")
        val alertId = required(input.alertId) ?: return Result.Invalid("alertId vacío")
        validateTemporal(input.startTimeMillis, input.endTimeMillis, startRequired = false)
            ?.let { return Result.Invalid(it) }
        val sourceHash = digest(fields("source", source))
        val locationHash = digest(fields("location", location))
        val eventHash = digest(fields("alertId", alertId))
        val revisionHash = digest(
            fields(
                "severity", input.severity?.name,
                "start", input.startTimeMillis?.toString(),
                "end", input.endTimeMillis?.toString(),
                "instruction", input.instruction?.let(::canonicalText),
            ),
        )
        return Result.Valid("$VERSION-official-$sourceHash-$locationHash-$eventHash-$revisionHash")
    }

    /**
     * Temporary V1 compatibility only. These aliases can contain sensitive raw data;
     * never log or expose them. They are deliberately not used by either V2 function.
     */
    fun forecastAliasV1(
        legacyRawFormattedId: String,
        legacyRawPhenomenonName: String,
        legacyRawHourDateTimeMillis: Long,
    ): String =
        "forecast-$legacyRawFormattedId-$legacyRawPhenomenonName-$legacyRawHourDateTimeMillis"

    /**
     * Temporary V1 compatibility only. This alias can contain a sensitive raw alert ID;
     * never log or expose it. It is deliberately not used by the V2 function.
     */
    fun officialAliasV1(legacyRawAlertId: String): String = "official-$legacyRawAlertId"

    // Forecast requires a positive start. Official events may omit start; any present
    // start/end must be positive, and end must be later only when both are present.
    // Invalid temporal material returns Invalid; no dates are invented or inferred.
    private fun validateTemporal(start: Long?, end: Long?, startRequired: Boolean): String? {
        if (startRequired && start == null) return "inicio ausente"
        if (start != null && start <= 0L) return "inicio inválido"
        if (end != null && end <= 0L) return "fin inválido"
        if (start != null && end != null && end <= start) return "fin no posterior al inicio"
        return null
    }

    private fun required(value: String?): String? = value?.let(::canonicalText)?.takeIf { it.isNotEmpty() }

    private fun canonicalLocation(input: LocationInput): String? {
        val latitude = input.latitude ?: return null
        val longitude = input.longitude ?: return null
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return "${number(latitude)},${number(longitude)}"
    }

    private fun number(value: Double): String =
        if (value == 0.0) "0" else BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private fun canonicalText(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)

    private fun fields(vararg values: String?): ByteArray = buildString {
        values.forEach { value ->
            val text = value ?: ""
            append(text.toByteArray(StandardCharsets.UTF_8).size)
            append(':')
            append(text)
            append(';')
        }
    }.toByteArray(StandardCharsets.UTF_8)

    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
}
