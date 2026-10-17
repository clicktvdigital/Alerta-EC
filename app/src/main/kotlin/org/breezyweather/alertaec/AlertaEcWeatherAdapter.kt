package org.breezyweather.alertaec

import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.Hourly
import breezyweather.domain.weather.model.Precipitation
import kotlin.math.roundToInt
import breezyweather.domain.weather.reference.AlertSeverity
import breezyweather.domain.weather.reference.WeatherCode

object AlertaEcWeatherAdapter {

    fun fromLocation(location: Location, nowMillis: Long = System.currentTimeMillis()): List<AlertaEcEvent> {
        val weather = location.weather ?: return emptyList()
        val events = mutableListOf<AlertaEcEvent>()

        weather.currentAlertList.forEach { alert ->
            events += fromOfficialAlert(alert, location)
        }

        // Minutely rain remains owned by Notifications.checkAndSendPrecipitation.
        // Keep this Alerta EC route inactive to prevent duplicate notifications.

        // Use the eligibility clock; an hour starting exactly one hour ago is no longer eligible.
        weather.hourlyForecast.firstOrNull {
            it.date.time > nowMillis - 60 * 60 * 1000L &&
                it.date.time - nowMillis < 24 * 60 * 60 * 1000L
        }?.let { hour ->
            fromHourlyForecast(hour, location)?.let { events += it }
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

    private fun fromHourlyForecast(hour: Hourly, location: Location): AlertaEcEvent? {
        val precipitationMm = hour.precipitation?.total?.inMillimeters?.takeIf { it.isFinite() && it >= 0 }
        val probability = hour.precipitationProbability?.total?.inPercent?.takeIf { it.isFinite() && it in 0.0..100.0 }?.roundToInt()
        val windKmh = hour.wind?.speed?.inKilometersPerHour?.takeIf { it.isFinite() && it >= 0 }
        val gustKmh = hour.wind?.gusts?.inKilometersPerHour?.takeIf { it.isFinite() && it >= 0 }
        val codePhenomenon = hour.weatherCode?.let(::phenomenonFromWeatherCode)

        val phenomenon = when {
            codePhenomenon == AlertaEcPhenomenon.THUNDERSTORM -> codePhenomenon
            precipitationMm != null && precipitationMm >= Precipitation.PRECIPITATION_HOURLY_HEAVY ->
                AlertaEcPhenomenon.HEAVY_RAIN
            codePhenomenon != null -> codePhenomenon
            else -> return null
        }

        val severity = when {
            precipitationMm != null && precipitationMm >= Precipitation.PRECIPITATION_HOURLY_RAINSTORM ->
                AlertaEcSeverity.WARNING
            precipitationMm != null && precipitationMm >= Precipitation.PRECIPITATION_HOURLY_HEAVY ->
                AlertaEcSeverity.WATCH
            else -> AlertaEcSeverity.ADVISORY
        }

        val headline = when (phenomenon) {
            AlertaEcPhenomenon.HEAVY_RAIN -> "Lluvia fuerte prevista"
            AlertaEcPhenomenon.RAIN -> "Lluvia prevista"
            else -> hour.weatherText ?: phenomenon.spokenName
        }

        return AlertaEcEvent(
            id = "forecast-${location.formattedId}-${phenomenon.name}-${hour.date.time}",
            phenomenon = phenomenon,
            state = AlertaEcEventState.FORECAST,
            severity = severity,
            headline = headline,
            locationName = locationName(location),
            latitude = location.latitude,
            longitude = location.longitude,
            sourceName = location.forecastSource,
            startTimeMillis = hour.date.time,
            updatedAtMillis = hour.acquiredAt?.time,
            precipitationAccumulationMm = precipitationMm,
            precipitationProbabilityPercent = probability,
            temperatureCelsius = hour.temperature?.temperature?.inCelsius,
            feelsLikeCelsius = hour.temperature?.feelsLikeTemperature?.inCelsius,
            windSpeedKmh = windKmh,
            windGustKmh = gustKmh,
            windDirectionDegrees = hour.wind?.degree?.takeIf { it.isFinite() && it in 0.0..360.0 }?.roundToInt(),
            humidityPercent = hour.relativeHumidity?.inPercent?.takeIf { it.isFinite() && it in 0.0..100.0 }?.roundToInt(),
            pressureHpa = hour.pressure?.inHectopascals,
            visibilityKm = hour.visibility?.inKilometers
        )
    }

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

    internal fun phenomenonFromText(text: String): AlertaEcPhenomenon {
        val value = text.lowercase(java.util.Locale.ROOT)
        return when {
            "tsunami" in value -> AlertaEcPhenomenon.TSUNAMI
            "sismo" in value || "terremoto" in value -> AlertaEcPhenomenon.EARTHQUAKE
            "volc" in value || "ceniza" in value -> AlertaEcPhenomenon.VOLCANIC_ACTIVITY
            "desliz" in value -> AlertaEcPhenomenon.LANDSLIDE
            "inund" in value -> AlertaEcPhenomenon.FLOOD
            "granizo" in value -> AlertaEcPhenomenon.HAIL
            "tormenta eléctrica" in value || "tormenta electrica" in value ||
                "trueno" in value || "thunder" in value -> AlertaEcPhenomenon.THUNDERSTORM
            "viento fuerte" in value || "strong wind" in value || "high wind" in value -> AlertaEcPhenomenon.STRONG_WIND
            "niebla" in value -> AlertaEcPhenomenon.FOG
            "incendio" in value -> AlertaEcPhenomenon.WILDFIRE
            "lluvia" in value || "rain" in value -> AlertaEcPhenomenon.RAIN
            else -> AlertaEcPhenomenon.OTHER
        }
    }

    private fun locationName(location: Location): String =
        location.geographicName.takeIf { it.isNotBlank() }
            ?: location.admin1?.takeIf { it.isNotBlank() }
            ?: location.country
}
