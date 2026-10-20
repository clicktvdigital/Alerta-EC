package org.breezyweather.alertaec.support

import breezyweather.data.location.LocationRepository
import breezyweather.data.weather.WeatherRepository
import org.breezyweather.domain.settings.SettingsManager
import java.util.concurrent.TimeUnit

/**
 * Diagnostico local de la primera ubicacion configurada.
 * No confunde un intento de refresco con la adquisicion de datos.
 */
internal object AutoSupportWeatherDiagnostics {

    suspend fun run(
        context: android.content.Context,
        locationRepository: LocationRepository,
        weatherRepository: WeatherRepository,
    ): List<AutoSupportCheck> {
        val location = locationRepository.getFirstLocation(withParameters = false)
            ?: return listOf(
                AutoSupportCheck(
                    id = "weather_location",
                    description = "Ubicacion meteorologica principal",
                    result = AutoSupportResult.PENDIENTE,
                    detail = "No hay una ubicacion principal configurada para comprobar.",
                )
            )

        val weather = weatherRepository.getWeatherByLocationId(location.formattedId)
            ?: return listOf(
                AutoSupportCheck(
                    id = "weather_data",
                    description = "Datos meteorologicos de la ubicacion principal",
                    result = AutoSupportResult.PENDIENTE,
                    detail = "No existen datos meteorologicos guardados para la ubicacion principal.",
                )
            )

        val now = System.currentTimeMillis()
        val configuredInterval = SettingsManager.getInstance(
            context,
        ).updateInterval.interval?.inWholeMilliseconds
        val hourlyAcquisition = weather.hourlyForecast
            .asSequence()
            .filter { AutoSupportWeatherContent.hasUsefulHourlyRecord(it, now) }
            .mapNotNull { it.acquiredAt?.time }
            .maxOrNull()

        val (hasCurrent, hasDaily, hasHourly) =
            AutoSupportWeatherContent.summary(weather, now)

        return listOf(
            contentAwareFreshness(
                "weather_current",
                "Condiciones actuales",
                weather.base.currentUpdateTime?.time,
                now,
                configuredInterval,
                hasCurrent,
            ),
            contentAwareFreshness(
                "weather_daily",
                "Pronostico diario",
                weather.base.forecastUpdateTime?.time,
                now,
                configuredInterval,
                hasDaily,
            ),
            contentAwareFreshness(
                "weather_hourly",
                "Pronostico horario",
                hourlyAcquisition,
                now,
                configuredInterval,
                hasHourly,
            ),
        )
    }

    private fun contentAwareFreshness(
        id: String,
        description: String,
        acquiredAt: Long?,
        now: Long,
        configuredInterval: Long?,
        hasContent: Boolean,
    ): AutoSupportCheck {
        val checked = freshness(id, description, acquiredAt, now, configuredInterval)
        return if (hasContent) {
            checked
        } else {
            checked.copy(
                result = AutoSupportResult.PENDIENTE,
                detail = checked.detail +
                    " No se encontraron datos meteorologicos utiles para esta seccion.",
            )
        }
    }

    private fun freshness(
        id: String,
        description: String,
        acquiredAt: Long?,
        now: Long,
        configuredInterval: Long?,
    ): AutoSupportCheck {
        val age = acquiredAt?.let { now - it }
        val ageMinutes = age?.let { TimeUnit.MILLISECONDS.toMinutes(it) }
        val hasValidDate = age != null && age >= 0
        val withinInterval = hasValidDate && configuredInterval != null && age < configuredInterval
        val noAutomaticInterval = hasValidDate && configuredInterval == null
        val detail = when {
            acquiredAt == null -> "No consta fecha de adquisicion verificable."
            !hasValidDate -> "La fecha de adquisicion es futura o invalida."
            configuredInterval == null -> "Actualizaciones automaticas desactivadas. Se confirma una fecha de adquisicion valida, pero no la vigencia de los datos. Ultima adquisicion hace aproximadamente ${ageMinutes} minutos; no se evalua caducidad automatica."
            withinInterval -> "Ultima adquisicion hace aproximadamente ${ageMinutes} minutos; dentro del intervalo automatico configurado."
            else -> "Ultima adquisicion hace aproximadamente ${ageMinutes} minutos; supera el intervalo automatico configurado. No confirma un fallo del proveedor."
        }
        return AutoSupportCheck(
            id = id,
            description = description,
            result = if (withinInterval || noAutomaticInterval) AutoSupportResult.CORRECTO else AutoSupportResult.PENDIENTE,
            detail = detail,
            acquiredAtMillis = acquiredAt,
        )
    }
}
