package org.breezyweather.alertaec

/**
 * Pure V1/V2 compatibility decision layer.
 *
 * The V1 alias is accepted only as internal compatibility input. It is not
 * retained by [Decision], so data-class rendering, logs, and exceptions cannot
 * expose its potentially sensitive contents. This object does not persist
 * marks, deduplicate events, or publish notifications.
 *
 * API contract: V2 history is supplied as exact IDs. A V2 exact match exists
 * only when a [AlertaEcEventIdentityV2.Result.Valid.id] is a member of that
 * set; a boolean assertion is deliberately not accepted.
 */
internal object AlertaEcEventIdentityCompatibility {
    enum class Identification {
        V1_ONLY,
        V2_EXACT,
        V2_UNRESOLVED_LEGACY,
        V2_NEW,
        INVALID_EVIDENCE,
    }

    /** Publication policy is separate from identity and temporal ambiguity. */
    enum class NotificationPolicy {
        V1_COMPATIBILITY_ONLY,
        SUPPRESS_EXACT_V2,
        NO_SUPPRESS_PRIORITY_FROM_V1,
        TEMPORAL_AMBIGUITY_INFORMATION,
        TEMPORAL_AMBIGUITY_ADVISORY,
        ELIGIBLE_AS_NEW_V2,
        DO_NOT_PUBLISH_INVALID_EVIDENCE,
    }

    /** Temporal meaning of historical evidence, independent of posting. */
    enum class TemporalDuplicateStatus {
        NOT_APPLICABLE,
        EXACT_V2_HISTORY_ONLY,
        V1_HISTORY_AMBIGUOUS,
        NO_HISTORY,
        INVALID_EVIDENCE,
    }

    /** The V1 alias is intentionally absent from this safe decision value. */
    data class Decision(
        val severity: AlertaEcSeverity,
        val v2: AlertaEcEventIdentityV2.Result,
        val v1MarkExists: Boolean,
        val v2HistoryContainsCurrentId: Boolean,
        val identification: Identification,
        val notificationPolicy: NotificationPolicy,
        val temporalDuplicateStatus: TemporalDuplicateStatus,
    )

    /** Decides policy from typed severity and exact historical V2 IDs; never posts. */
    fun decide(
        aliasV1: String,
        v2: AlertaEcEventIdentityV2.Result,
        severity: AlertaEcSeverity,
        v1MarkExists: Boolean,
        historicalV2Ids: Set<String>,
    ): Decision {
        val aliasV1Exists = aliasV1.isNotEmpty()
        val currentV2Id = (v2 as? AlertaEcEventIdentityV2.Result.Valid)?.id
        val v2HistoryContainsCurrentId = currentV2Id != null && currentV2Id in historicalV2Ids
        val contradictoryV1Evidence = v1MarkExists && !aliasV1Exists
        val contradictoryV2Evidence = v2 is AlertaEcEventIdentityV2.Result.Invalid && historicalV2Ids.isNotEmpty()

        val identification = when {
            contradictoryV1Evidence || contradictoryV2Evidence -> Identification.INVALID_EVIDENCE
            v2 is AlertaEcEventIdentityV2.Result.Invalid && aliasV1Exists -> Identification.V1_ONLY
            v2 is AlertaEcEventIdentityV2.Result.Invalid -> Identification.INVALID_EVIDENCE
            v2HistoryContainsCurrentId -> Identification.V2_EXACT
            v1MarkExists -> Identification.V2_UNRESOLVED_LEGACY
            else -> Identification.V2_NEW
        }
        val notificationPolicy = when (identification) {
            Identification.V1_ONLY -> NotificationPolicy.V1_COMPATIBILITY_ONLY
            Identification.V2_EXACT -> NotificationPolicy.SUPPRESS_EXACT_V2
            Identification.V2_UNRESOLVED_LEGACY -> when (severity) {
                AlertaEcSeverity.WATCH, AlertaEcSeverity.WARNING, AlertaEcSeverity.EMERGENCY ->
                    NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1
                AlertaEcSeverity.INFORMATION -> NotificationPolicy.TEMPORAL_AMBIGUITY_INFORMATION
                AlertaEcSeverity.ADVISORY -> NotificationPolicy.TEMPORAL_AMBIGUITY_ADVISORY
            }
            Identification.V2_NEW -> NotificationPolicy.ELIGIBLE_AS_NEW_V2
            Identification.INVALID_EVIDENCE -> NotificationPolicy.DO_NOT_PUBLISH_INVALID_EVIDENCE
        }
        val temporalDuplicateStatus = when (identification) {
            Identification.V1_ONLY -> TemporalDuplicateStatus.NOT_APPLICABLE
            Identification.V2_EXACT -> TemporalDuplicateStatus.EXACT_V2_HISTORY_ONLY
            Identification.V2_UNRESOLVED_LEGACY -> TemporalDuplicateStatus.V1_HISTORY_AMBIGUOUS
            Identification.V2_NEW -> TemporalDuplicateStatus.NO_HISTORY
            Identification.INVALID_EVIDENCE -> TemporalDuplicateStatus.INVALID_EVIDENCE
        }
        return Decision(
            severity = severity,
            v2 = v2,
            v1MarkExists = v1MarkExists,
            v2HistoryContainsCurrentId = v2HistoryContainsCurrentId,
            identification = identification,
            notificationPolicy = notificationPolicy,
            temporalDuplicateStatus = temporalDuplicateStatus,
        )
    }
}
