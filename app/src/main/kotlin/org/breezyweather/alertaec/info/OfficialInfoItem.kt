package org.breezyweather.alertaec.info

import java.net.URI

/** A source-neutral, location-general item suitable for bounded offline storage. */
data class OfficialInfoItem(
    val sourceId: String,
    val bulletinId: String,
    val category: OfficialInfoCategory,
    val title: String,
    val summary: String,
    val body: String,
    val country: String?,
    val province: String?,
    val canton: String?,
    val region: String?,
    val publishedAtMillis: Long?,
    val updatedAtMillis: Long?,
    val fetchedAtMillis: Long,
    val expiresAtMillis: Long?,
    val verification: OfficialInfoVerification,
    val sourceUrl: String?
) {
    fun validationErrors(): List<OfficialInfoValidationError> = mutableListOf<OfficialInfoValidationError>().apply {
        if (!IDENTIFIER.matches(sourceId)) add(OfficialInfoValidationError.INVALID_SOURCE_ID)
        if (!IDENTIFIER.matches(bulletinId)) add(OfficialInfoValidationError.INVALID_BULLETIN_ID)
        if (title.isBlank() || title.length > MAX_TITLE) add(OfficialInfoValidationError.INVALID_TITLE)
        if (summary.length > MAX_SUMMARY) add(OfficialInfoValidationError.INVALID_SUMMARY)
        if (body.length > MAX_BODY) add(OfficialInfoValidationError.INVALID_BODY)
        listOf(country, province, canton, region).forEach { value ->
            if (value != null && value.length > MAX_LOCATION) {
                add(OfficialInfoValidationError.INVALID_LOCATION)
            }
        }
        val published = publishedAtMillis
        val updated = updatedAtMillis
        if (fetchedAtMillis < 0L || published != null && published < 0L || updated != null && updated < 0L || expiresAtMillis != null && expiresAtMillis < 0L) {
            add(OfficialInfoValidationError.INVALID_TIMESTAMP)
        }
        if (published != null && updated != null && updated < published) {
            add(OfficialInfoValidationError.INCONSISTENT_TIMESTAMPS)
        }
        if (updated != null && updated > fetchedAtMillis) {
            add(OfficialInfoValidationError.INCONSISTENT_TIMESTAMPS)
        }
        if (published != null && published > fetchedAtMillis) {
            add(OfficialInfoValidationError.INCONSISTENT_TIMESTAMPS)
        }
        if (published != null && expiresAtMillis != null && expiresAtMillis < published) {
            add(OfficialInfoValidationError.INCONSISTENT_TIMESTAMPS)
        }
        if (updated != null && expiresAtMillis != null && expiresAtMillis < updated) {
            add(OfficialInfoValidationError.INCONSISTENT_TIMESTAMPS)
        }
        if (sourceUrl != null && !isHttpsUrl(sourceUrl)) {
            add(OfficialInfoValidationError.INVALID_SOURCE_URL)
        }
    }

    fun isValid(): Boolean = validationErrors().isEmpty()

    companion object {
        const val MAX_TITLE = 160
        const val MAX_SUMMARY = 400
        const val MAX_BODY = 10_000
        const val MAX_LOCATION = 120

        private val IDENTIFIER = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")

        private fun isHttpsUrl(value: String): Boolean = runCatching {
            val uri = URI(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null &&
                isSafeHost(uri.host)
        }.getOrDefault(false)

        private fun isSafeHost(host: String): Boolean {
            val normalized = host.trim().trimEnd('.').lowercase()
            if (normalized == "localhost" || normalized.endsWith(".local") ||
                normalized == "::1" || normalized == "[::1]") return false
            val ipv4 = normalized.split('.').takeIf { parts ->
                parts.size == 4 && parts.all { it.toIntOrNull() != null }
            }?.map { it.toInt() }
            if (ipv4 != null) {
                val first = ipv4[0]
                val second = ipv4[1]
                if (first == 0 || first == 10 || first == 127 ||
                    (first == 100 && second in 64..127) ||
                    (first == 169 && second == 254) ||
                    (first == 172 && second in 16..31) ||
                    (first == 192 && second == 168)) return false
            }
            val ipv6 = normalized.removePrefix("[").removeSuffix("]")
            if (':' in ipv6 && (ipv6 == "::1" || ipv6.startsWith("fc") ||
                    ipv6.startsWith("fd") || ipv6.startsWith("fe8") ||
                    ipv6.startsWith("fe9") || ipv6.startsWith("fea") ||
                    ipv6.startsWith("feb"))) return false
            return true
        }
    }
}

enum class OfficialInfoCategory {
    ALERT,
    INCIDENT,
    FORECAST,
    RECOMMENDATION,
    HELP
}

enum class OfficialInfoVerification {
    UNVERIFIED,
    SOURCE_VERIFIED,
    OFFICIAL
}

enum class OfficialInfoValidationError {
    INVALID_SOURCE_ID,
    INVALID_BULLETIN_ID,
    INVALID_TITLE,
    INVALID_SUMMARY,
    INVALID_BODY,
    INVALID_LOCATION,
    INVALID_TIMESTAMP,
    INCONSISTENT_TIMESTAMPS,
    INVALID_SOURCE_URL
}
