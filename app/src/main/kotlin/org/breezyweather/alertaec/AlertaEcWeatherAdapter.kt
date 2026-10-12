package org.breezyweather.alertaec

import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.Minutely
import breezyweather.domain.weather.reference.AlertSeverity
import breezyweather.domain.weather.reference.WeatherCode

object AlertaEcWeatherAdapter {

    fun fromLocation(location: Location): List<AlertaEcEvent> {
        val weather = location.weather ?: return emptyList()
        val events = mutableListOf<AlertaEcEvent>()

        weather.currentAlertList.forEach { alert ->
            events += fromOfficialAlert(alert, location)
        }

        weather.minutelyForecast.firstOrNull {
            (it.dbz ?: 0) > 0
        }?.let { minute ->
            events += fromMinutelyRain(minute, location)
        }

        weather.nextHourlyForecast.firstOrNull()?.weatherCode?.let { code ->
            phenomenonFromWeatherCode(code)?.let { phenomenon ->
                if (phenomenon != AlertaEcPhenomenon.RAIN) {
                    val hour = weather.nextHourlyForecast.first()
                    events += AlertaEcEvent(
                        id = "forecast-${location.formattedId}-${phenomenon.name}-${hour.date.time}",
                        phenomenon = phenomenon,
                        state = AlertaEcEventState.FORECAST,
                        severity = AlertaEcSeverity.ADVISORY,
                        headline = hour.weatherText ?: phenomenon.spokenName,
                        locationName = locationName(location),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        sourceName = location.forecastSource,
                        startTimeMillis = hour.date.time
                    )
                }
            }
        }

        return events.distinctBy { it.id }
    }

    private fun fromOfficialAlert(alert: Alert, location: Location) = AlertaEcEvent(
        id = "official-${alert.alertId}",
        phenomenon = phenomenonFromText("${alert.headline.orEmpty()} ${alert.description.orEmpty()}"),
        state = AlertaEcEventState.OFFICIAL_EVENT,
        severity = severityFromAlert(alert.severity),
        headline = alert.headline ?: "Alerta oficial",
        locationName = locationName(location),
        latitude = location.latitude,
        longitude = location.longitude,
        sourceName = alert.source ?: location.alertSource,
        description = alert.description,
        startTimeMillis = alert.startDate?.time,
        endTimeMillis = alert.endDate?.time,
        details = alert.instruction?.let { mapOf("instruction" to it) } ?: emptyMap()
    )

    private fun fromMinutelyRain(minute: Minutely, location: Location) = AlertaEcEvent(
        id = "rain-${location.formattedId}-${minute.date.time}",
        phenomenon = AlertaEcPhenomenon.RAIN,
        state = AlertaEcEventState.FORECAST,
        severity = AlertaEcSeverity.ADVISORY,
        headline = "Lluvia prevista",
        locationName = locationName(location),
        latitude = location.latitude,
        longitude = location.longitude,
        sourceName = location.minutelySource ?: location.forecastSource,
        startTimeMillis = minute.date.time,
        endTimeMillis = minute.endingDate.time,
        precipitationMmPerHour = minute.precipitationIntensity?.inMillimeters
    )

    private fun severityFromAlert(severity: AlertSeverity) = when (severity) {
        AlertSeverity.EXTREME -> AlertaEcSeverity.EMERGENCY
        AlertSeverity.SEVERE -> AlertaEcSeverity.WARNING
        AlertSeverity.MODERATE -> AlertaEcSeverity.WATCH
        AlertSeverity.MINOR -> AlertaEcSeverity.ADVISORY
        AlertSeverity.UNKNOWN -> AlertaEcSeverity.INFORMATION
    }

    private fun phenomenonFromWeatherCode(code: WeatherCode): AlertaEcPhenomenon? = when (code) {
        WeatherCode.RAIN -> AlertaEcPhenomenon.RAIN
        WeatherCode.SNOW -> AlertaEcPhenomenon.SNOW
        WeatherCode.WIND -> AlertaEcPhenomenon.STRONG_WIND
        WeatherCode.FOG -> AlertaEcPhenomenon.FOG
        WeatherCode.HAIL -> AlertaEcPhenomenon.HAIL
        WeatherCode.THUNDER, WeatherCode.THUNDERSTORM -> AlertaEcPhenomenon.THUNDERSTORM
        else -> null
    }

    private fun phenomenonFromText(text: String): AlertaEcPhenomenon {
        val value = text.lowercase()
        return when {
            "tsunami" in value -> AlertaEcPhenomenon.TSUNAMI
            "sismo" in value || "terremoto" in value -> AlertaEcPhenomenon.EARTHQUAKE
            "volc" in value || "ceniza" in value -> AlertaEcPhenomenon.VOLCANIC_ACTIVITY
            "desliz" in value -> AlertaEcPhenomenon.LANDSLIDE
            "inund" in value -> AlertaEcPhenomenon.FLOOD
            "granizo" in value -> AlertaEcPhenomenon.HAIL
            "tormenta" in value || "trueno" in value -> AlertaEcPhenomenon.THUNDERSTORM
            "viento" in value -> AlertaEcPhenomenon.STRONG_WIND
            "niebla" in value -> AlertaEcPhenomenon.FOG
            "incendio" in value -> AlertaEcPhenomenon.WILDFIRE
            "lluvia" in value -> AlertaEcPhenomenon.RAIN
            else -> AlertaEcPhenomenon.OTHER
        }
    }

    private fun locationName(location: Location): String =
        location.customName?.takeIf { it.isNotBlank() }
            ?: location.district?.takeIf { it.isNotBlank() }
            ?: location.city.takeIf { it.isNotBlank() }
            ?: location.admin1
            ?: location.country
}
