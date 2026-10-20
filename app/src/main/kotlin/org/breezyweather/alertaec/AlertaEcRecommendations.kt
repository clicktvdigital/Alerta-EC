package org.breezyweather.alertaec

import java.util.Calendar
import java.util.TimeZone

/** Consejos generales; no sustituyen instrucciones oficiales. */
internal object AlertaEcRecommendations {
    private const val RAIN_PROBABILITY_THRESHOLD = 50
    private const val BOOTS_ACCUMULATION_MM = 10.0
    private const val BOOTS_INTENSITY_MM_PER_HOUR = 5.0
    private const val UV_PROTECTIVE_THRESHOLD = 6.0
    private const val NIGHT_COLD_HOUR = 18
    private const val MORNING_COLD_HOUR = 6

    fun forEvent(event: AlertaEcEvent): String {
        val rawBasic = forPhenomenon(event.phenomenon)
        if (event.state != AlertaEcEventState.FORECAST) return rawBasic
        val basic = if (isStrongWind(event) &&
            event.phenomenon == AlertaEcPhenomenon.RAIN
        ) rawBasic.replace("paraguas", "impermeable") else rawBasic
        val tips = mutableListOf<String>()
        val amount = event.precipitationAccumulationMm?.takeIf { it.isFinite() && it >= 0.0 }
        val intensity = event.precipitationMmPerHour?.takeIf { it.isFinite() && it >= 0.0 }
        val probability = event.precipitationProbabilityPercent?.takeIf { it in 0..100 }
        val rainy = event.phenomenon == AlertaEcPhenomenon.RAIN ||
            event.phenomenon == AlertaEcPhenomenon.HEAVY_RAIN ||
            event.phenomenon == AlertaEcPhenomenon.THUNDERSTORM
        val strongWind = isStrongWind(event)
        val rainExpected = rainy && (amount != null && amount > 0.0 ||
            probability != null && probability >= RAIN_PROBABILITY_THRESHOLD)
        if (rainExpected) {
            if (event.phenomenon != AlertaEcPhenomenon.THUNDERSTORM && !strongWind) {
                tips += "Lleva sombrilla o impermeable; usa calzado antideslizante."
            } else {
                tips += "Lleva impermeable y calzado antideslizante; evita la sombrilla por seguridad."
            }
            if ((amount != null && amount >= BOOTS_ACCUMULATION_MM) ||
                (intensity != null && intensity >= BOOTS_INTENSITY_MM_PER_HOUR)
            ) {
                tips += "Considera botas antideslizantes por la cantidad o intensidad prevista."
            }
        }
        val temp = event.feelsLikeCelsius?.takeIf { it.isFinite() }
            ?: event.temperatureCelsius?.takeIf { it.isFinite() }
        val localHour = localHour(event)
        when {
            temp != null && temp <= 12.0 -> tips += "Usa chompa y ropa abrigada."
            temp != null && temp <= 15.0 && localHour != null &&
                (localHour >= NIGHT_COLD_HOUR || localHour < MORNING_COLD_HOUR) ->
                tips += "Lleva una chompa para la noche o la madrugada."
            temp != null && temp >= 28.0 -> tips += "Lleva agua y ropa ligera; busca sombra."
        }
        val uv = event.uvIndex?.takeIf { it.isFinite() && it >= 0.0 }
        if (uv != null && uv >= UV_PROTECTIVE_THRESHOLD && isDaytime(localHour)) {
            tips += "Usa sombrero y protector solar durante las horas de sol."
        }
        if (strongWind) {
            tips += "Aléjate de objetos inestables y evita usar sombrilla con ráfagas intensas."
        }
        val normalized = basic.lowercase()
        val uniqueTips = tips.filterNot { tip ->
            val text = tip.lowercase()
            normalized.contains(text) ||
                (text.contains("chompa") && listOf("chompa", "ropa por capas").any(normalized::contains)) ||
                (text.contains("sombrero") && listOf("sombrero", "gorra", "ropa ligera").any(normalized::contains)) ||
                (text.startsWith("lleva sombrilla") && normalized.contains("paraguas")) ||
                (text.contains("ráfagas") && listOf("ráfagas", "objetos que puedan caer").any(normalized::contains))
        }
        return (listOf(basic) + uniqueTips).joinToString(" ")
    }

    private fun isStrongWind(event: AlertaEcEvent): Boolean {
        val gust = event.windGustKmh?.takeIf { it.isFinite() && it >= 0.0 }
        val wind = event.windSpeedKmh?.takeIf { it.isFinite() && it >= 0.0 }
        return (gust != null && gust >= 50.0) || (wind != null && wind >= 40.0)
    }

    private fun localHour(event: AlertaEcEvent): Int? {
        val millis = event.startTimeMillis?.takeIf { it > 0L } ?: return null
        val zoneId = event.timeZoneId?.takeIf { it.isNotBlank() } ?: return null
        val zone = TimeZone.getTimeZone(zoneId)
        if (zoneId != zone.id && zoneId != "UTC") return null
        return Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
    }

    private fun isDaytime(hour: Int?): Boolean = hour != null && hour in 7..17

    fun forPhenomenon(phenomenon: AlertaEcPhenomenon): String = when (phenomenon) {
        AlertaEcPhenomenon.RAIN -> "Lleva paraguas o poncho de aguas, chompa impermeable y calzado antideslizante. Consulta el pronóstico actualizado."
        AlertaEcPhenomenon.HEAVY_RAIN -> "Usa poncho de aguas, impermeable y calzado antideslizante. Protege documentos y teléfono; no cruces zonas inundadas ni te acerques a quebradas."
        AlertaEcPhenomenon.THUNDERSTORM -> "Busca refugio en un edificio seguro. Evita árboles aislados, espacios abiertos y cuerpos de agua; no uses paraguas como protección contra rayos."
        AlertaEcPhenomenon.HAIL -> "Refúgiate bajo techo, evita desplazamientos y protege tu cabeza si estás afuera. Al conducir, detente en un lugar seguro."
        AlertaEcPhenomenon.STRONG_WIND -> "Usa ropa ajustada y evita paraguas durante ráfagas. Aléjate de árboles, cables y objetos que puedan caer."
        AlertaEcPhenomenon.FLOOD -> "No cruces calles, ríos ni quebradas inundadas. Sigue indicaciones oficiales y prepara documentos, medicinas esenciales, agua y teléfono cargado si debes evacuar."
        AlertaEcPhenomenon.LANDSLIDE -> "Aléjate de taludes y laderas inestables. Identifica rutas seguras y sigue las instrucciones oficiales de evacuación."
        AlertaEcPhenomenon.EARTHQUAKE -> "Durante el movimiento, agáchate, cúbrete y sujétate. Después, aléjate de estructuras dañadas y sigue indicaciones oficiales."
        AlertaEcPhenomenon.VOLCANIC_ACTIVITY -> "Consulta avisos oficiales. Ante caída de ceniza, permanece bajo techo; si debes salir, usa mascarilla bien ajustada, gafas protectoras y ropa que cubra la piel."
        AlertaEcPhenomenon.WILDFIRE -> "Evita el humo, cierra ventanas si es seguro y sigue indicaciones oficiales. Prepara documentos, medicinas y agua ante una posible evacuación."
        AlertaEcPhenomenon.FOG -> "Si conduces, reduce la velocidad, usa luces bajas y aumenta la distancia. Lleva ropa abrigada si hace frío."
        AlertaEcPhenomenon.SNOW -> "Usa chompa abrigada, ropa por capas, guantes y calzado cerrado antideslizante. Consulta el estado de las vías."
        AlertaEcPhenomenon.EXTREME_HEAT -> "Lleva agua, gorra o sombrero, ropa ligera y bloqueador solar. Busca sombra y evita esfuerzos en las horas de mayor calor."
        AlertaEcPhenomenon.EXTREME_COLD -> "Usa chompa abrigada, ropa por capas, gorro y calzado cerrado. Protege especialmente a niños y adultos mayores."
        AlertaEcPhenomenon.TROPICAL_CYCLONE -> "Consulta avisos oficiales, evita zonas costeras expuestas y prepara agua, medicinas, documentos y teléfono cargado si se indica evacuación."
        AlertaEcPhenomenon.TSUNAMI -> "Ante alerta oficial de tsunami o señales naturales de peligro en la costa, evacúa inmediatamente hacia zonas altas siguiendo rutas seguras; no esperes a ver la ola."
        AlertaEcPhenomenon.OTHER -> "Verifica la fuente y vigencia de la información. Sigue las recomendaciones de las autoridades competentes."
    }
}
