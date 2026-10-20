package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AlertaEcRecommendationsTest {
    @Test
    fun everyPhenomenonHasAdvice() {
        AlertaEcPhenomenon.entries.forEach { phenomenon ->
            AlertaEcRecommendations.forPhenomenon(phenomenon).isNotBlank() shouldBe true
        }
    }

    @Test
    fun rainAdviceMentionsProtection() {
        AlertaEcRecommendations.forPhenomenon(AlertaEcPhenomenon.RAIN)
            .contains("paraguas", ignoreCase = true) shouldBe true
    }

    @Test
    fun tsunamiAdviceRequiresOfficialAlert() {
        AlertaEcRecommendations.forPhenomenon(AlertaEcPhenomenon.TSUNAMI)
            .contains("alerta oficial", ignoreCase = true) shouldBe true
    }
}
