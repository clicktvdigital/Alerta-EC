package org.breezyweather.alertaec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlertaEcRainRiskTest {
    @Test
    fun noDataIsCalmAndSaysWhy() {
        val result = AlertaEcRainRisk.evaluate(AlertaEcRainRisk.Inputs())
        assertEquals(0, result.score)
        assertEquals(AlertaEcRainRisk.Level.CALM, result.level)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test
    fun allSourcesAtMaximumIsDangerCappedAt100() {
        val result = AlertaEcRainRisk.evaluate(
            AlertaEcRainRisk.Inputs(
                probabilityPercent = 100,
                forecastMm3h = 40.0,
                rainingWithinMinutes = 0,
                measuredLastHourMm = 25.0,
                inamhi3hMm = 30.0,
                communityReports = 6,
            )
        )
        assertEquals(100, result.score)
        assertEquals(AlertaEcRainRisk.Level.DANGER, result.level)
        assertEquals(6, result.reasons.size)
    }

    @Test
    fun levelsFollowTheirThresholds() {
        assertEquals(AlertaEcRainRisk.Level.CALM, AlertaEcRainRisk.levelFor(24))
        assertEquals(AlertaEcRainRisk.Level.WATCH, AlertaEcRainRisk.levelFor(25))
        assertEquals(AlertaEcRainRisk.Level.CAUTION, AlertaEcRainRisk.levelFor(50))
        assertEquals(AlertaEcRainRisk.Level.DANGER, AlertaEcRainRisk.levelFor(75))
    }

    @Test
    fun moderateProbabilityAloneIsWatch() {
        val result = AlertaEcRainRisk.evaluate(AlertaEcRainRisk.Inputs(probabilityPercent = 80))
        assertEquals(28, result.score)
        assertEquals(AlertaEcRainRisk.Level.WATCH, result.level)
    }
}
