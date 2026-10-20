package org.breezyweather.alertaec.support

/**
 * Evita iniciar actualizaciones meteorologicas innecesarias.
 * No intenta reparar permisos, GPS ni problemas ajenos al pronostico.
 */
internal object AutoSupportWeatherRepairPolicy {

    private val refreshableIds = setOf(
        "weather_data",
        "weather_current",
        "weather_daily",
        "weather_hourly",
    )

    fun shouldRequestRefresh(checks: List<AutoSupportCheck>): Boolean =
        checks.any { check ->
            check.id in refreshableIds &&
                check.result == AutoSupportResult.PENDIENTE
        }
}
