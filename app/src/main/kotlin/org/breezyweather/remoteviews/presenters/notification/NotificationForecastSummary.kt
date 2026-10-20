package org.breezyweather.remoteviews.presenters.notification

import android.content.Context
import breezyweather.domain.location.model.Location
import org.breezyweather.alertaec.AlertaEcNotificationText
import org.breezyweather.common.extensions.formatMeasure
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.unit.formatting.UnitWidth
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** All forecast values on the compact line belong to the same hourly interval. */
internal class NotificationForecastSummary(context: Context, location: Location) {
    private val now = System.currentTimeMillis()
    private val hour = location.weather?.hourlyForecast?.firstOrNull {
        it.date.time > now - 3_600_000L && it.date.time < now + 86_400_000L
    }
    private val settings = SettingsManager.getInstance(context)
    private val temperature = if (settings.isWidgetNotificationUsingFeelsLike) {
        hour?.temperature?.feelsLikeTemperature ?: hour?.temperature?.temperature
    } else {
        hour?.temperature?.temperature
    }
    val place = location.geographicName.takeIf { it.isNotBlank() }
        ?: listOf(location.admin1, location.country).firstOrNull { !it.isNullOrBlank() }
        ?: "Ubicación no disponible"
    val phenomenon = NotificationWeatherText.nonBlank(hour?.weatherText) ?: "Pronóstico horario"
    val temperatureText = temperature?.takeIf { it.inCelsius.isFinite() }?.formatMeasure(
        context, settings.getTemperatureUnit(context), unitWidth = UnitWidth.NARROW
    )?.let { "Temp. $it" } ?: "Temp. —"
    val interval = hour?.let { AlertaEcNotificationText.hourlyPeriod(it.date.time, now, location.timeZone) }
        ?: "Hora no disponible"
    val values = listOf(
        AlertaEcNotificationText.probabilityPercent(hour?.precipitationProbability?.total?.inPercent),
        AlertaEcNotificationText.accumulationMm(hour?.precipitation?.total?.inMillimeters)
    ).joinToString(" · ")
    val compact = "$temperatureText · $values"
    /** Compatibility text for existing widget consumers; it includes the new hourly metrics. */
    val metrics = "$interval · $compact"
    val details = buildList {
        add("$phenomenon · $place")
        add("$interval · $compact")
        add("Precipitación: acumulación del intervalo horario.")
        if (settings.isWidgetNotificationUsingFeelsLike) add("Temperatura mostrada: sensación térmica si está disponible.")
        hour?.wind?.speed?.inKilometersPerHour?.let { add(String.format("Viento: %.1f km/h", it)) }
        hour?.wind?.gusts?.inKilometersPerHour?.let { add(String.format("Ráfagas: %.1f km/h", it)) }
        hour?.wind?.degree?.takeIf { it.isFinite() && it >= 0 }?.let { add("Dirección del viento: $it°") }
        hour?.relativeHumidity?.inPercent?.let { add("Humedad: ${it.roundToInt()}%") }
        hour?.pressure?.inHectopascals?.let { add(String.format("Presión: %.0f hPa", it)) }
        hour?.visibility?.inKilometers?.let { add(String.format("Visibilidad: %.1f km", it)) }
        add("Fuente: ${location.forecastSource}")
        add(hour?.acquiredAt?.time?.takeIf { it in 1..now }?.let {
            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).apply {
                timeZone = location.timeZone
            }.format(Date(it))
            "Recibido: $date"
        } ?: "Recepción desconocida")
    }.joinToString("\n")
}
