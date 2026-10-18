package org.breezyweather.alertaec

/** Consejos generales; no sustituyen instrucciones oficiales. */
internal object AlertaEcRecommendations {
    fun forEvent(event: AlertaEcEvent): String {
        val basic = forPhenomenon(event.phenomenon)
        if (event.state != AlertaEcEventState.FORECAST) return basic
        val tips = mutableListOf<String>()
        val mm = event.precipitationAccumulationMm?.takeIf { it.isFinite() && it >= 0.0 }
        val probability = event.precipitationProbabilityPercent?.takeIf { it in 0..100 }
        val rainy = event.phenomenon == AlertaEcPhenomenon.RAIN ||
            event.phenomenon == AlertaEcPhenomenon.HEAVY_RAIN ||
            event.phenomenon == AlertaEcPhenomenon.THUNDERSTORM
        if (rainy && ((mm != null && mm >= 0.2) ||
                (probability != null && probability >= 50))) {
            tips += "Lleva paraguas o poncho de aguas y calzado antideslizante."
        }
        val temp = event.feelsLikeCelsius?.takeIf { it.isFinite() }
            ?: event.temperatureCelsius?.takeIf { it.isFinite() }
        when {
            temp != null && temp <= 12.0 -> tips += "Usa chompa y ropa abrigada."
            temp != null && temp >= 28.0 -> tips += "Lleva agua, sombrero y ropa ligera; busca sombra."
        }
        val gust = event.windGustKmh?.takeIf { it.isFinite() && it >= 0.0 }
        val wind = event.windSpeedKmh?.takeIf { it.isFinite() && it >= 0.0 }
        if ((gust != null && gust >= 50.0) || (wind != null && wind >= 40.0)) {
            tips += "Aléjate de objetos inestables y evita usar paraguas con ráfagas intensas."
        }
        val normalized = basic.lowercase()
        val uniqueTips = tips.filterNot { tip ->
            val text = tip.lowercase()
            when {
                text.contains("paraguas") && listOf("paraguas", "poncho", "impermeable").any { normalized.contains(it) } -> true
                text.contains("chompa") && listOf("chompa", "ropa abrigada", "ropa por capas").any { normalized.contains(it) } -> true
                text.contains("sombrero") && listOf("sombrero", "gorra", "ropa ligera").any { normalized.contains(it) } -> true
                text.contains("ráfagas") && listOf("ráfagas", "objetos que puedan caer").any { normalized.contains(it) } -> true
                else -> normalized.contains(text)
            }
        }
        return (listOf(basic) + uniqueTips).joinToString(" ")
    }

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
