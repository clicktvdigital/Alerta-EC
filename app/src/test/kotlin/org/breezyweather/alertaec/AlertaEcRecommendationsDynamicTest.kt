package org.breezyweather.alertaec

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AlertaEcRecommendationsDynamicTest {
    private fun event(p: AlertaEcPhenomenon, temp: Double? = null,
        mm: Double? = null, probability: Int? = null, gust: Double? = null) =
        AlertaEcEvent(id = "test", phenomenon = p,
            state = AlertaEcEventState.FORECAST,
            severity = AlertaEcSeverity.ADVISORY, headline = "Prueba",
            temperatureCelsius = temp, precipitationAccumulationMm = mm,
            precipitationProbabilityPercent = probability, windGustKmh = gust)

    @Test fun rain() {
        assertTrue(AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.RAIN, mm = 2.0)).contains("paraguas"))
    }
    @Test fun cold() {
        assertTrue(AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.FOG, temp = 8.0)).contains("chompa"))
    }
    @Test fun heat() {
        assertTrue(AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.FOG, temp = 31.0)).contains("agua"))
    }
    @Test fun dry() {
        assertFalse(AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.FOG, probability = 10)).contains("paraguas"))
    }
    @Test fun wind() {
        assertTrue(AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.FOG, gust = 65.0)).contains("ráfagas"))
    }

    @Test fun avoidsDuplicateRainAdvice() {
        val advice = AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.RAIN, mm = 2.0))
        assertFalse(advice.contains("Lleva paraguas o poncho de aguas y calzado antideslizante."))
    }
    @Test fun avoidsDuplicateColdAdvice() {
        val advice = AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.SNOW, temp = 5.0))
        assertFalse(advice.contains("Usa chompa y ropa abrigada."))
    }
    @Test fun preservesSafetyAdvice() {
        val advice = AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.THUNDERSTORM, mm = 5.0))
        assertTrue(advice.contains("Busca refugio"))
    }

}
