package org.breezyweather.background.location

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Confidence assigned to a postal value before it is shown to the user. */
enum class PostalCodeConfidence(val rank: Int) {
    NONE(0),
    REVERSE_GEOCODER(1),
    AUTHORIZED_OFFICIAL(2),
}

data class PostalCodeCandidate(
    val code: String,
    val provenance: String,
    val confidence: PostalCodeConfidence,
)

sealed class PostalCodeParseResult {
    data class Success(val candidate: PostalCodeCandidate) : PostalCodeParseResult()
    object Empty : PostalCodeParseResult()
    data class Invalid(val reason: String) : PostalCodeParseResult()
}

/**
 * Parses a response supplied by an authorized transport. This class never performs network I/O.
 */
object EcuadorPostalCodeAdapter {
    private val codePattern = Regex("\"codigo_postal\"\\s*:\\s*\"([^\"]*)\"")
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        response: String?,
        provenance: String,
        confidence: PostalCodeConfidence = PostalCodeConfidence.AUTHORIZED_OFFICIAL,
    ): PostalCodeParseResult {
        if (response.isNullOrBlank()) return PostalCodeParseResult.Empty
        if (provenance.isBlank()) return PostalCodeParseResult.Invalid("procedencia vacía")
        val body = response.trim()
        if (!((body.startsWith("{") && body.endsWith("}")) ||
                (body.startsWith("[") && body.endsWith("]")))) {
            return PostalCodeParseResult.Invalid("respuesta JSON incompleta")
        }
        val parsed = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: return PostalCodeParseResult.Invalid("JSON malformado")
        val emptyPayload = when (parsed) {
            is JsonArray -> parsed.isEmpty()
            is JsonObject -> (parsed["data"] as? JsonArray)?.isEmpty() == true
            else -> false
        }
        if (emptyPayload) return PostalCodeParseResult.Empty
        val rawCode = codePattern.find(body)?.groupValues?.getOrNull(1)
            ?: return PostalCodeParseResult.Invalid("falta codigo_postal")
        if (!isValidCode(rawCode)) {
            return PostalCodeParseResult.Invalid("código postal ecuatoriano inválido")
        }
        return PostalCodeParseResult.Success(
            PostalCodeCandidate(rawCode, provenance, confidence)
        )
    }

    fun isValidCode(code: String?): Boolean = code?.matches(Regex("[0-9]{6}")) == true
}
