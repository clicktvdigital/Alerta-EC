package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class AlertaEcEventIdentityCompatibilityTest {
    private val alias = "forecast-legacy-sensitive-value"
    private val v2Id = "aec-v2-forecast-current-revision"
    private val differentV2Id = "aec-v2-forecast-different-revision"
    private val valid = AlertaEcEventIdentityV2.Result.Valid(v2Id)
    private val invalid = AlertaEcEventIdentityV2.Result.Invalid("datos inválidos")

    private fun decide(
        aliasV1: String = alias,
        result: AlertaEcEventIdentityV2.Result = valid,
        severity: AlertaEcSeverity = AlertaEcSeverity.ADVISORY,
        v1Mark: Boolean = false,
        history: Set<String> = emptySet(),
    ) = AlertaEcEventIdentityCompatibility.decide(aliasV1, result, severity, v1Mark, history)

    @Test
    fun completeMatrixKeepsIdentificationAndPolicyDeterministic() {
        AlertaEcSeverity.values().forEach { severity ->
            listOf(valid, invalid).forEach { result ->
                listOf(alias, "").forEach { aliasV1 ->
                    listOf(false, true).forEach { v1Mark ->
                        listOf(emptySet(), setOf(differentV2Id), setOf(v2Id)).forEach { history ->
                            val decision = decide(aliasV1, result, severity, v1Mark, history)
                            val invalidEvidence =
                                (result is AlertaEcEventIdentityV2.Result.Invalid && aliasV1.isEmpty()) ||
                                    (result is AlertaEcEventIdentityV2.Result.Invalid && history.isNotEmpty()) ||
                                    (v1Mark && aliasV1.isEmpty())
                            if (invalidEvidence) {
                                decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.INVALID_EVIDENCE
                            } else if (result is AlertaEcEventIdentityV2.Result.Invalid) {
                                decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V1_ONLY
                            } else if (history.contains(v2Id)) {
                                decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_EXACT
                            } else if (v1Mark) {
                                decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_UNRESOLVED_LEGACY
                            } else {
                                decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_NEW
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun validV2WithExactCurrentIdAlwaysSuppressesRegardlessOfOtherV2Ids() {
        val decision = decide(history = setOf(differentV2Id, v2Id), v1Mark = true)
        decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_EXACT
        decision.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.SUPPRESS_EXACT_V2
        decision.temporalDuplicateStatus shouldBe AlertaEcEventIdentityCompatibility.TemporalDuplicateStatus.EXACT_V2_HISTORY_ONLY
        decision.v2HistoryContainsCurrentId shouldBe true
    }

    @Test
    fun differentV2IdIsNotAnExactMatch() {
        val decision = decide(history = setOf(differentV2Id))
        decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_NEW
        decision.v2HistoryContainsCurrentId shouldBe false
    }

    @Test
    fun legacyMarkWithoutV2MatchHasSeveritySpecificAmbiguityPolicy() {
        val expected = mapOf(
            AlertaEcSeverity.INFORMATION to AlertaEcEventIdentityCompatibility.NotificationPolicy.TEMPORAL_AMBIGUITY_INFORMATION,
            AlertaEcSeverity.ADVISORY to AlertaEcEventIdentityCompatibility.NotificationPolicy.TEMPORAL_AMBIGUITY_ADVISORY,
            AlertaEcSeverity.WATCH to AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1,
            AlertaEcSeverity.WARNING to AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1,
            AlertaEcSeverity.EMERGENCY to AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1,
        )
        expected.forEach { (severity, policy) ->
            val decision = decide(severity = severity, v1Mark = true)
            decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_UNRESOLVED_LEGACY
            decision.notificationPolicy shouldBe policy
            decision.temporalDuplicateStatus shouldBe AlertaEcEventIdentityCompatibility.TemporalDuplicateStatus.V1_HISTORY_AMBIGUOUS
        }
    }

    @Test
    fun emptyAliasWithDeclaredV1MarkIsInvalidEvenWhenV2Matches() {
        AlertaEcSeverity.values().forEach { severity ->
            val decision = decide(aliasV1 = "", severity = severity, v1Mark = true, history = setOf(v2Id))
            decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.INVALID_EVIDENCE
            decision.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.DO_NOT_PUBLISH_INVALID_EVIDENCE
            decision.temporalDuplicateStatus shouldBe AlertaEcEventIdentityCompatibility.TemporalDuplicateStatus.INVALID_EVIDENCE
        }
    }

    @Test
    fun whitespaceOnlyV1AliasKeepsCurrentNonEmptyAliasBehaviorAndStaysOutOfDecisionString() {
        val whitespaceAlias = "   "
        val decision = decide(aliasV1 = whitespaceAlias, result = invalid, v1Mark = true)

        decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V1_ONLY
        decision.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.V1_COMPATIBILITY_ONLY
        decision.toString().contains(whitespaceAlias) shouldBe false
    }

    @Test
    fun notificationPolicyMatrixUsesExactIdsAndRejectsInvalidEvidence() {
        val base = AlertaEcEventIdentityV2.ForecastInput(
            source = "Fuente matriz",
            location = AlertaEcEventIdentityV2.LocationInput(-0.18, -78.46),
            phenomenon = AlertaEcPhenomenon.RAIN,
            startTimeMillis = 1_700_000_000_000L,
            severity = AlertaEcSeverity.ADVISORY,
        )
        val currentId = (AlertaEcEventIdentityV2.forecast(base) as AlertaEcEventIdentityV2.Result.Valid).id
        val otherId = (AlertaEcEventIdentityV2.forecast(base.copy(source = "Otra fuente matriz"))
            as AlertaEcEventIdentityV2.Result.Valid).id

        val exact = decide(
            result = AlertaEcEventIdentityV2.Result.Valid(currentId),
            severity = AlertaEcSeverity.EMERGENCY,
            v1Mark = true,
            history = setOf(otherId, currentId),
        )
        exact.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.SUPPRESS_EXACT_V2
        exact.v2HistoryContainsCurrentId shouldBe true

        val ambiguousPolicies = mapOf(
            AlertaEcSeverity.INFORMATION to AlertaEcEventIdentityCompatibility.NotificationPolicy.TEMPORAL_AMBIGUITY_INFORMATION,
            AlertaEcSeverity.ADVISORY to AlertaEcEventIdentityCompatibility.NotificationPolicy.TEMPORAL_AMBIGUITY_ADVISORY,
            AlertaEcSeverity.WATCH to AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1,
            AlertaEcSeverity.WARNING to AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1,
            AlertaEcSeverity.EMERGENCY to AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1,
        )
        ambiguousPolicies.forEach { (severity, policy) ->
            val ambiguous = decide(
                result = AlertaEcEventIdentityV2.Result.Valid(currentId),
                severity = severity,
                v1Mark = true,
                history = setOf(otherId),
            )
            ambiguous.notificationPolicy shouldBe policy
            if (severity == AlertaEcSeverity.WATCH ||
                severity == AlertaEcSeverity.WARNING ||
                severity == AlertaEcSeverity.EMERGENCY
            ) {
                ambiguous.notificationPolicy shouldBe
                    AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1
            }
        }

        val invalidEvidence = decide(
            result = invalid,
            aliasV1 = "",
            v1Mark = true,
            history = setOf(currentId),
        )
        invalidEvidence.notificationPolicy shouldBe
            AlertaEcEventIdentityCompatibility.NotificationPolicy.DO_NOT_PUBLISH_INVALID_EVIDENCE
        invalidEvidence.temporalDuplicateStatus shouldBe
            AlertaEcEventIdentityCompatibility.TemporalDuplicateStatus.INVALID_EVIDENCE
    }

    @Test
    fun invalidV2NeverAcceptsHistoricalV2AsExact() {
        val decision = decide(result = invalid, v1Mark = false, history = setOf(v2Id))
        decision.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.INVALID_EVIDENCE
        decision.identification shouldNotBe AlertaEcEventIdentityCompatibility.Identification.V2_EXACT
        decision.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.DO_NOT_PUBLISH_INVALID_EVIDENCE
        decision.v2HistoryContainsCurrentId shouldBe false
    }

    @Test
    fun invalidV2KeepsV1OnlyOnlyWithCoherentEvidence() {
        decide(result = invalid, aliasV1 = alias, v1Mark = true).identification shouldBe
            AlertaEcEventIdentityCompatibility.Identification.V1_ONLY
        decide(result = invalid, aliasV1 = alias, v1Mark = false).identification shouldBe
            AlertaEcEventIdentityCompatibility.Identification.V1_ONLY
        decide(result = invalid, aliasV1 = "", v1Mark = false).identification shouldBe
            AlertaEcEventIdentityCompatibility.Identification.INVALID_EVIDENCE
        decide(result = invalid, aliasV1 = "", v1Mark = true).identification shouldBe
            AlertaEcEventIdentityCompatibility.Identification.INVALID_EVIDENCE
    }

    @Test
    fun advisoryToWarningKeepsLegacyAmbiguityAndDoesNotSuppressPriority() {
        val advisory = decide(severity = AlertaEcSeverity.ADVISORY, v1Mark = true)
        val warning = decide(severity = AlertaEcSeverity.WARNING, v1Mark = true)
        advisory.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_UNRESOLVED_LEGACY
        warning.identification shouldBe AlertaEcEventIdentityCompatibility.Identification.V2_UNRESOLVED_LEGACY
        advisory.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.TEMPORAL_AMBIGUITY_ADVISORY
        warning.notificationPolicy shouldBe AlertaEcEventIdentityCompatibility.NotificationPolicy.NO_SUPPRESS_PRIORITY_FROM_V1
    }

    @Test
    fun sourceAndLocationChangesAreNotCollapsedWhenV2IdsDiffer() {
        val sourceChange = AlertaEcEventIdentityV2.Result.Valid("aec-v2-source-change")
        val locationChange = AlertaEcEventIdentityV2.Result.Valid("aec-v2-location-change")
        decide(result = sourceChange, history = setOf(v2Id)).v2HistoryContainsCurrentId shouldBe false
        decide(result = locationChange, history = setOf(v2Id)).v2HistoryContainsCurrentId shouldBe false
    }

    @Test
    fun actualSourceAndLocationChangesHaveDifferentV2HistoryResults() {
        val base = AlertaEcEventIdentityV2.ForecastInput(
            source = "Fuente A",
            location = AlertaEcEventIdentityV2.LocationInput(-0.18, -78.46),
            phenomenon = AlertaEcPhenomenon.RAIN,
            startTimeMillis = 1_700_000_000_000L,
            severity = AlertaEcSeverity.ADVISORY,
        )
        val sourceChange = base.copy(source = "Fuente B")
        val locationChange = base.copy(
            location = AlertaEcEventIdentityV2.LocationInput(-0.95, -90.96),
        )
        val baseId = AlertaEcEventIdentityV2.forecast(base) as AlertaEcEventIdentityV2.Result.Valid
        val sourceId = AlertaEcEventIdentityV2.forecast(sourceChange) as AlertaEcEventIdentityV2.Result.Valid
        val locationId = AlertaEcEventIdentityV2.forecast(locationChange) as AlertaEcEventIdentityV2.Result.Valid
        decide(result = sourceId, history = setOf(baseId.id)).v2HistoryContainsCurrentId shouldBe false
        decide(result = locationId, history = setOf(baseId.id)).v2HistoryContainsCurrentId shouldBe false
    }

    @Test
    fun decisionToStringDoesNotExposeV1Alias() {
        decide(aliasV1 = alias, v1Mark = true).toString().contains(alias) shouldBe false
    }

    @Test
    fun sameRevisionUsesActualIdEquality() {
        decide(result = AlertaEcEventIdentityV2.Result.Valid(v2Id), history = setOf(v2Id)).identification shouldBe
            AlertaEcEventIdentityCompatibility.Identification.V2_EXACT
        decide(result = AlertaEcEventIdentityV2.Result.Valid("$v2Id-extra"), history = setOf(v2Id)).identification shouldBe
            AlertaEcEventIdentityCompatibility.Identification.V2_NEW
    }
}
