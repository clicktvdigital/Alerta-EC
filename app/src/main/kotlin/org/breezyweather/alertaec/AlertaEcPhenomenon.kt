package org.breezyweather.alertaec

enum class AlertaEcPhenomenon(val spokenName: String, val soundKey: String) {
    RAIN("lluvia", "rain"),
    HEAVY_RAIN("lluvia fuerte", "heavy_rain"),
    THUNDERSTORM("tormenta eléctrica", "thunderstorm"),
    HAIL("granizo", "hail"),
    STRONG_WIND("viento fuerte", "strong_wind"),
    FLOOD("inundación", "flood"),
    LANDSLIDE("deslizamiento", "landslide"),
    EARTHQUAKE("sismo", "earthquake"),
    VOLCANIC_ACTIVITY("actividad volcánica", "volcano"),
    WILDFIRE("incendio", "wildfire"),
    FOG("niebla", "fog"),
    SNOW("nieve", "snow"),
    EXTREME_HEAT("calor extremo", "extreme_heat"),
    EXTREME_COLD("frío extremo", "extreme_cold"),
    TROPICAL_CYCLONE("ciclón tropical", "tropical_cyclone"),
    TSUNAMI("tsunami", "tsunami"),
    OTHER("fenómeno", "general_alert"),
}

enum class AlertaEcEventState {
    FORECAST,
    OBSERVED,
    OFFICIAL_EVENT,
}

enum class AlertaEcSeverity {
    INFORMATION,
    ADVISORY,
    WATCH,
    WARNING,
    EMERGENCY,
}
