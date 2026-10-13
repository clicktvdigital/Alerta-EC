package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.Test

class AlertaEcNotificationPolicyTest {
    @Test
    fun `only forecast events with an active source are eligible`() {
        AlertaEcNotificationPolicy.shouldNotify(AlertaEcEventState.FORECAST) shouldBe true
        AlertaEcNotificationPolicy.shouldNotify(AlertaEcEventState.OBSERVED) shouldBe false
        AlertaEcNotificationPolicy.shouldNotify(AlertaEcEventState.OFFICIAL_EVENT) shouldBe false
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

    private fun event(id: String) = AlertaEcEvent(
        id = id,
        phenomenon = AlertaEcPhenomenon.RAIN,
        state = AlertaEcEventState.FORECAST,
        severity = AlertaEcSeverity.ADVISORY,
        headline = "Lluvia prevista"
    )
}
