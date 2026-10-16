package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AlertaEcNotificationPolicyTest {
    @Test
    fun `only fresh forecast events with a forecast time are eligible`() {
        val now = 1_800_000_000_000L
        AlertaEcNotificationPolicy.shouldNotify(event("fresh").copy(startTimeMillis = now + 1_000), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(event("recent").copy(startTimeMillis = now - 60 * 60 * 1000L + 1), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(event("stale").copy(startTimeMillis = now - 60 * 60 * 1000L - 1), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(event("missing-time"), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(
            event("observed").copy(state = AlertaEcEventState.OBSERVED, startTimeMillis = now),
            now
        ) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(
            event("official").copy(state = AlertaEcEventState.OFFICIAL_EVENT, startTimeMillis = now),
            now
        ) shouldBe false
    }

    @Test
    fun `state labels preserve forecast and observed classification`() {
        AlertaEcNotificationPolicy.stateLabel(AlertaEcEventState.FORECAST) shouldBe "PRONÓSTICO"
        AlertaEcNotificationPolicy.stateLabel(AlertaEcEventState.OBSERVED) shouldBe "OBSERVADO"
    }

    @Test
    fun `event filtering removes repeated and previously notified ids`() {
        val repeated = event("same")
        val events = listOf(event("old"), repeated, repeated, event("new"))

        AlertaEcNotificationPolicy.filterNewEvents(events, setOf("old")) shouldBe listOf(repeated, event("new"))
    }

    @Test
    fun acquisitionAgeIsInclusiveAndFutureOrUnknownAcquisitionIsRejected() {
        val now = 1_800_000_000_000L
        val valid = event("acquisition").copy(startTimeMillis = now)
        val hour = 3_600_000L
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(updatedAtMillis = now - hour), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(updatedAtMillis = now - hour - 1), now) shouldBe false
        for (acquired in listOf(null, 0L, -1L, Long.MIN_VALUE, now + 1, Long.MAX_VALUE)) {
            AlertaEcNotificationPolicy.shouldNotify(valid.copy(updatedAtMillis = acquired), now) shouldBe false
        }
    }

    @Test
    fun expiredAndReversedIntervalsAreRejectedAtTheExactEnd() {
        val now = 1_800_000_000_000L
        val valid = event("interval").copy(startTimeMillis = now - 30 * 60_000L)
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = now + 1), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = now), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = now - 1), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = valid.startTimeMillis), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = 0), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = Long.MIN_VALUE), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = Long.MAX_VALUE), now) shouldBe false
    }

    @Test
    fun unknownEndHasAConservativeStartAgeLimitWithoutInventingAnEnd() {
        val now = 1_800_000_000_000L
        val valid = event("no-end")
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(startTimeMillis = now - 3_600_000 + 1), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(startTimeMillis = now - 3_600_000), now) shouldBe false
    }

    @Test
    fun futureHorizonIsSixHoursInclusive() {
        val now = 1_800_000_000_000L
        val horizon = 6 * 3_600_000L
        val valid = event("future")
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(startTimeMillis = now + horizon), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(startTimeMillis = now + horizon + 1), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(startTimeMillis = Long.MAX_VALUE), now) shouldBe false
    }

    @Test
    fun invalidStartAndReferenceTimesAreRejected() {
        val now = 1_800_000_000_000L
        for (start in listOf(null, 0L, -1L, Long.MIN_VALUE)) {
            AlertaEcNotificationPolicy.shouldNotify(event("invalid").copy(startTimeMillis = start), now) shouldBe false
        }
        AlertaEcNotificationPolicy.shouldNotify(event("clock").copy(startTimeMillis = now), 0) shouldBe false
    }

    @Test
    fun explicitIntervalsAreBoundedAndCanIncludeAnOngoingForecast() {
        val now = 1_800_000_000_000L
        val valid = event("duration").copy(startTimeMillis = now)
        val duration = 6 * 3_600_000L
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = now + duration), now) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(valid.copy(endTimeMillis = now + duration + 1), now) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(
            valid.copy(startTimeMillis = now - 2 * 3_600_000L, endTimeMillis = now + 1),
            now
        ) shouldBe true
    }

    private fun event(id: String) = AlertaEcEvent(
        id = id,
        phenomenon = AlertaEcPhenomenon.RAIN,
        state = AlertaEcEventState.FORECAST,
        severity = AlertaEcSeverity.ADVISORY,
        headline = "Lluvia prevista",
        updatedAtMillis = 1_800_000_000_000L
    )
}
