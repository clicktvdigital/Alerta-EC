package org.breezyweather.alertaec

import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ALERTA EC rain risk index (0-100) for the next hours at one place, combining every source the app
 * has, and always explaining why. A guide for the user, not an official warning.
 *
 * Weights: forecast probability 35, forecast amount (3 h) 30, rain measured by EPMAPS gauges 20,
 * INAMHI WRF 3 h 10, community reports nearby up to 15 (capped at 100 overall).
 */
object AlertaEcRainRisk {
    enum class Level(val emoji: String, val label: String, val advice: String) {
        CALM("🟢", "Tranquilo", "Sin señales de lluvia fuerte."),
        WATCH("🟡", "Atento", "Puede llover; mantente pendiente del cielo y de la app."),
        CAUTION("🟠", "Precaución", "Lluvia intensa posible: evita quebradas y zonas bajas."),
        DANGER("🔴", "Peligro", "Lluvia fuerte probable o en curso: no cruces calles inundadas y sigue avisos oficiales."),
    }

    data class Inputs(
        val probabilityPercent: Int? = null,
        val forecastMm3h: Double? = null,
        val rainingWithinMinutes: Int? = null,
        val measuredLastHourMm: Double? = null,
        val inamhi3hMm: Double? = null,
        val communityReports: Int = 0,
    )

    data class Result(val score: Int, val level: Level, val reasons: List<String>)

    fun levelFor(score: Int): Level = when {
        score >= 75 -> Level.DANGER
        score >= 50 -> Level.CAUTION
        score >= 25 -> Level.WATCH
        else -> Level.CALM
    }

    fun evaluate(inputs: Inputs): Result {
        val es = Locale.forLanguageTag("es-EC")
        val reasons = mutableListOf<String>()
        var score = 0.0
        inputs.probabilityPercent?.coerceIn(0, 100)?.let {
            score += it * 0.35
            reasons += "Probabilidad de lluvia: $it %"
        }
        inputs.forecastMm3h?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            score += min(it / 20.0, 1.0) * 30.0
            if (it > 0.0) reasons += String.format(es, "Pronóstico: %.1f mm en 3 h", it)
        }
        inputs.rainingWithinMinutes?.let {
            score += if (it <= 5) 10.0 else 5.0
            reasons += if (it <= 5) "Lluvia ahora o en minutos (minuto a minuto)" else "Lluvia en ~$it min"
        }
        inputs.measuredLastHourMm?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            score += min(it / 10.0, 1.0) * 20.0
            reasons += if (it > 0.0) String.format(es, "Pluviómetro EPMAPS: %.1f mm en la última hora", it)
            else "Pluviómetro EPMAPS: sin lluvia medida"
        }
        inputs.inamhi3hMm?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            score += min(it / 20.0, 1.0) * 10.0
            reasons += String.format(es, "INAMHI WRF: %.1f mm en 3 h", it)
        }
        if (inputs.communityReports > 0) {
            score += min(inputs.communityReports * 5.0, 15.0)
            reasons += "Reportes de la comunidad cerca: ${inputs.communityReports}"
        }
        val total = score.roundToInt().coerceIn(0, 100)
        if (reasons.isEmpty()) reasons += "Sin datos suficientes todavía"
        return Result(total, levelFor(total), reasons)
    }
}
