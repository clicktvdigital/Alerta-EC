package org.breezyweather.alertaec.support

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import breezyweather.data.location.LocationRepository
import org.breezyweather.background.weather.WeatherUpdateJob

/**
 * Solicita una actualizacion meteorologica solo cuando la consulta previa
 * no detecta trabajos pendientes o en ejecucion.
 *
 * La consulta y el encolado no son atomicos: una actualizacion automatica
 * podria aparecer entre ambas operaciones. No garantiza exclusion total.
 */
internal object AutoSupportWeatherRefresh {

    internal enum class RequestResult {
        SOLICITADA,
        SOLICITUD_NO_VERIFICADA,
        YA_EXISTE_TRABAJO,
        NO_SOLICITADA,
        SIN_UBICACION,
        ERROR,
    }

    internal data class RequestOutcome(
        val result: RequestResult,
        val workId: String? = null,
    )

    suspend fun request(
        context: Context,
        locationRepository: LocationRepository,
    ): RequestOutcome = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val location = locationRepository.getFirstLocation(withParameters = false)
                ?: return@withContext RequestOutcome(RequestResult.SIN_UBICACION)
            val status = AutoSupportWeatherWorkGuard.inspect(appContext)

            if (status.busy) {
                RequestOutcome(RequestResult.YA_EXISTE_TRABAJO)
            } else {
                val proposedId = WeatherUpdateJob.startNowTracked(
                    appContext,
                    location = location,
                )
                if (proposedId == null) {
                    RequestOutcome(RequestResult.NO_SOLICITADA)
                } else {
                    var accepted = false
                    var attempts = 0
                    while (!accepted && attempts < 6) {
                        accepted = AutoSupportWeatherWorkGuard.manualWorks(appContext)
                            .any { it.id == proposedId }
                        attempts++
                        if (!accepted && attempts < 6) {
                            delay(500)
                        }
                    }
                    if (accepted) {
                        RequestOutcome(RequestResult.SOLICITADA, proposedId)
                    } else {
                        val existingManualWork = AutoSupportWeatherWorkGuard
                            .manualWorks(appContext)
                            .any { work ->
                                work.id != proposedId &&
                                    AutoSupportWeatherWorkPolicy.isBusy(
                                        AutoSupportWeatherWorkPolicy.State.valueOf(work.state.name),
                                        manual = true,
                                    )
                            }
                        if (existingManualWork) {
                            RequestOutcome(RequestResult.YA_EXISTE_TRABAJO)
                        } else {
                            RequestOutcome(RequestResult.SOLICITUD_NO_VERIFICADA)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RequestOutcome(RequestResult.ERROR)
        }
    }
}
