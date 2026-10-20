package org.breezyweather.alertaec

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AlertaEcRecommendationsDynamicTest {
    private fun event(p: AlertaEcPhenomenon, temp: Double? = null,
        mm: Double? = null, intensity: Double? = null, probability: Int? = null,
        gust: Double? = null, tempFeels: Double? = null, uv: Double? = null,
        hourMillis: Long? = null, zone: String? = null) =
        AlertaEcEvent(id = "test", phenomenon = p,
            state = AlertaEcEventState.FORECAST,
            severity = AlertaEcSeverity.ADVISORY, headline = "Prueba",
            temperatureCelsius = temp, feelsLikeCelsius = tempFeels,
            precipitationAccumulationMm = mm, precipitationMmPerHour = intensity,
            precipitationProbabilityPercent = probability, windGustKmh = gust,
            uvIndex = uv, startTimeMillis = hourMillis, timeZoneId = zone)

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
    @Test fun probabilityAloneDoesNotRecommendBoots() {
        val advice = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.RAIN, probability = 80)
        )
        assertTrue(advice.contains("sombrilla") || advice.contains("paraguas"))
        assertFalse(advice.contains("botas antideslizantes"))
    }

    @Test fun intenseRainRecommendsBoots() {
        val advice = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.RAIN, mm = 12.0, intensity = 6.0)
        )
        assertTrue(advice.contains("botas antideslizantes"))
    }

    @Test fun highUvRecommendsSunProtectionOnlyDuringLocalDay() {
        val day = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.OTHER, uv = 7.0, hourMillis = 12L * 60L * 60L * 1000L, zone = "UTC")
        )
        val night = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.OTHER, uv = 7.0, hourMillis = 22L * 60L * 60L * 1000L, zone = "UTC")
        )
        assertTrue(day.contains("protector solar"))
        assertFalse(night.contains("protector solar"))
    }

    @Test fun invalidTimezoneDoesNotProduceUvAdvice() {
        val advice = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.OTHER, uv = 9.0, hourMillis = 12L * 60L * 60L * 1000L, zone = "Not/AZone")
        )
        assertFalse(advice.contains("protector solar"))
    }

    @Test fun uvAndRainCombineWithoutDuplicateAdvice() {
        val advice = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.RAIN, mm = 2.0, uv = 8.0, hourMillis = 12L * 60L * 60L * 1000L, zone = "UTC")
        )
        assertTrue(advice.contains("protector solar"))
        assertTrue(advice.contains("paraguas"))
        assertEquals(1, advice.split("protector solar").size - 1)
    }

    @Test fun coldNightUsesLocalTimezoneAndFeelsLike() {
        val advice = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.FOG, temp = 20.0, tempFeels = 14.0,
                hourMillis = 22L * 60L * 60L * 1000L, zone = "UTC")
        )
        assertTrue(advice.contains("chompa para la noche"))
    }

    @Test fun missingValuesDoNotInventClothingAdvice() {
        val advice = AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.FOG))
        assertFalse(advice.contains("protector solar"))
        assertFalse(advice.contains("chompa para la noche"))
        assertFalse(advice.contains("botas antideslizantes"))
    }

    @Test fun strongWindAvoidsSombrilla() {
        val advice = AlertaEcRecommendations.forEvent(
            event(AlertaEcPhenomenon.RAIN, mm = 2.0, gust = 60.0)
        )
        assertFalse(advice.contains("Lleva sombrilla o impermeable"))
        assertTrue(advice.contains("evita la sombrilla"))
    }

    @Test fun preservesSafetyAdvice() {
        val advice = AlertaEcRecommendations.forEvent(event(AlertaEcPhenomenon.THUNDERSTORM, mm = 5.0))
        assertTrue(advice.contains("Busca refugio"))
    }

}
