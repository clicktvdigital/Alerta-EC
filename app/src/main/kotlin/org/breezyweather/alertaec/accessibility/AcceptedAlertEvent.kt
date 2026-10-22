package org.breezyweather.alertaec.accessibility

import org.breezyweather.alertaec.AlertaEcPhenomenon
import org.breezyweather.alertaec.AlertaEcSeverity
import java.time.Instant

/**
 * Minimal, in-process payload for a future automatic voice consumer.
 *
 * The identity is stable only when [producerEventId] is stable. The producer owns that
 * definitive identity; this contract accepts only the versioned opaque form `v1-` plus a
 * complete lowercase hexadecimal SHA-256 digest. The digest must not be generated from
 * personal data, precise locations, free-form descriptions, or sensitive content. A digest
 * does not guarantee semantic uniqueness when producers use equal or unstable inputs.
 *
 * [briefTextSpanish] is generated exclusively from allow-listed enum values. It cannot contain
 * personal names, coordinates, external instructions, or unsanitized official descriptions.
 * [acceptanceEvidence] is declarative evidence supplied by a producer decision; it is not a
 * confirmation from Android that a notification was actually delivered or accepted.
 */
data class AcceptedAlertEvent(
    val producerEventId: String,
    val origin: Origin,
    val phenomenon: AlertaEcPhenomenon,
    val severity: AlertaEcSeverity,
    val start: Instant,
    val end: Instant,
    val acceptanceEvidence: AcceptanceEvidence,
) {
    val id: String = "${origin.name}:$producerEventId"
    val briefTextSpanish: String = "Alerta ${severityLabel(severity)} de ${phenomenon.spokenName}."

    init {
        require(producerEventId.length in MIN_PRODUCER_EVENT_ID_LENGTH..MAX_PRODUCER_EVENT_ID_LENGTH) {
            "producerEventId must be a v1- SHA-256 digest"
        }
        require(V1_PRODUCER_EVENT_ID_PATTERN.matches(producerEventId)) {
            "producerEventId must match v1- followed by 64 lowercase hexadecimal characters"
        }
        require(end.isAfter(start)) { "end must be after start" }
        require(briefTextSpanish.length <= MAX_BRIEF_TEXT_LENGTH) { "briefTextSpanish must be brief" }
    }

    enum class Origin { OFFICIAL, FORECAST }

    /** Explicit producer evidence, never inferred by this DTO or by transport. */
    enum class AcceptanceEvidence { NOTIFICATION_ACCEPTED_WITHOUT_BACKLOG }

    private companion object {
        private const val PRODUCER_EVENT_ID_VERSION_PREFIX = "v1-"
        private const val SHA256_HEX_LENGTH = 64
        private const val MIN_PRODUCER_EVENT_ID_LENGTH =
            PRODUCER_EVENT_ID_VERSION_PREFIX.length + SHA256_HEX_LENGTH
        private const val MAX_PRODUCER_EVENT_ID_LENGTH = MIN_PRODUCER_EVENT_ID_LENGTH
        private val V1_PRODUCER_EVENT_ID_PATTERN = Regex(
            "${Regex.escape(PRODUCER_EVENT_ID_VERSION_PREFIX)}[0-9a-f]{$SHA256_HEX_LENGTH}",
        )

        fun severityLabel(severity: AlertaEcSeverity): String = when (severity) {
            AlertaEcSeverity.INFORMATION -> "información"
            AlertaEcSeverity.ADVISORY -> "aviso"
            AlertaEcSeverity.WATCH -> "vigilancia"
            AlertaEcSeverity.WARNING -> "advertencia"
            AlertaEcSeverity.EMERGENCY -> "emergencia"
        }
        const val MAX_BRIEF_TEXT_LENGTH = 80
    }
}
