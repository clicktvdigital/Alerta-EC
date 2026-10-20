package org.breezyweather.alertaec.support

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Coordina el autosoporte sin solicitar acciones intermedias.
 *
 * La fase REPARANDO se reservara para operaciones seguras verificables.
 * Esta primera version solo diagnostica; no declara reparaciones ficticias.
 */
internal object AutoSupportEngine {

    internal data class Progress(
        val phase: AutoSupportPhase,
        val report: AutoSupportReport? = null,
        val message: String,
    )

    fun run(
        context: Context,
        locationRepository: breezyweather.data.location.LocationRepository,
        weatherRepository: breezyweather.data.weather.WeatherRepository,
    ): Flow<Progress> = flow {
        emit(Progress(AutoSupportPhase.DIAGNOSTICANDO, message = "Comprobando servicios de ALERTA EC"))

        val checks = try {
            AutoSupportDiagnostics.run(context.applicationContext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            listOf(
                AutoSupportCheck(
                    id = "diagnostic_error",
                    description = "Diagnostico general",
                    result = AutoSupportResult.ERROR,
                    detail = "No fue posible completar el diagnostico: ${e.javaClass.simpleName}",
                )
            )
        }

        val weatherChecks = try {
            AutoSupportWeatherDiagnostics.run(
                context = context.applicationContext,
                locationRepository = locationRepository,
                weatherRepository = weatherRepository,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            listOf(
                AutoSupportCheck(
                    id = "weather_diagnostic_error",
                    description = "Diagnostico meteorologico",
                    result = AutoSupportResult.ERROR,
                    detail = "No fue posible consultar los datos meteorologicos: ${e.javaClass.simpleName}",
                )
            )
        }

        val workChecks = try {
            val status = AutoSupportWeatherWorkGuard.inspect(context.applicationContext)
            listOf(
                AutoSupportCheck(
                    id = "weather_update_queue",
                    description = "Estado de actualizaciones meteorologicas",
                    result = AutoSupportResult.CORRECTO,
                    queueBusy = status.busy,
                    detail = if (status.busy) {
                        "WorkManager registra ${status.runningCount} trabajos meteorologicos en ejecucion y ${status.pendingCount} solicitudes manuales pendientes o bloqueadas. No se iniciara una actualizacion duplicada."
                    } else {
                        "No hay actualizaciones manuales pendientes ni trabajos meteorologicos en ejecucion. Las actualizaciones automaticas pueden permanecer programadas para su proximo horario."
                    },
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            listOf(
                AutoSupportCheck(
                    id = "weather_update_queue",
                    description = "Estado de actualizaciones meteorologicas",
                    result = AutoSupportResult.PENDIENTE,
                    detail = "No se pudo comprobar la cola de actualizaciones: ${e.javaClass.simpleName}",
                )
            )
        }

        var repairMessage: String? = null
        var verifiedWeatherChecks = weatherChecks
        val internetValidated = checks.any {
            it.id == "internet" && it.result == AutoSupportResult.CORRECTO
        }
        val weatherQueueState = workChecks
            .firstOrNull { it.id == "weather_update_queue" }
            ?.takeIf { it.result == AutoSupportResult.CORRECTO }
            ?.queueBusy
        val weatherQueueAvailable = weatherQueueState != null
        val weatherQueueBusy = weatherQueueState == true
        if (
            AutoSupportWeatherRepairPolicy.shouldRequestRefresh(weatherChecks) &&
            internetValidated &&
            weatherQueueAvailable &&
            !weatherQueueBusy
        ) {
            emit(
                Progress(
                    phase = AutoSupportPhase.REPARANDO,
                    report = AutoSupportReport(checks + weatherChecks + workChecks),
                    message = "Solicitando una actualizacion meteorologica segura para la ubicacion principal",
                )
            )
            val repairStartedAtMillis = System.currentTimeMillis()
            val request = AutoSupportWeatherRefresh.request(
                context = context.applicationContext,
                locationRepository = locationRepository,
            )
            repairMessage = when (request.result) {
                AutoSupportWeatherRefresh.RequestResult.SOLICITADA -> {
                    val workId = request.workId
                    if (workId == null) {
                        "Se registro una solicitud, pero no hay identificador para seguirla. Reparacion pendiente."
                    } else {
                        emit(
                            Progress(
                                phase = AutoSupportPhase.REPARANDO,
                                report = AutoSupportReport(checks + weatherChecks + workChecks),
                                message = "Esperando el resultado de la actualizacion meteorologica",
                            )
                        )
                        val outcome = AutoSupportWeatherWorkTracker.await(
                            context = context.applicationContext,
                            workId = workId,
                        )
                        val assessment = AutoSupportWeatherRepairOutcome.assess(outcome)
                        if (assessment.shouldRecheckWeather) {
                            emit(
                                Progress(
                                    phase = AutoSupportPhase.REPARANDO,
                                    report = AutoSupportReport(checks + weatherChecks + workChecks),
                                    message = "Comprobando si se recibieron datos meteorologicos nuevos",
                                )
                            )
                            val afterChecks = try {
                                AutoSupportWeatherDiagnostics.run(
                                    context = context.applicationContext,
                                    locationRepository = locationRepository,
                                    weatherRepository = weatherRepository,
                                )
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                listOf(
                                    AutoSupportCheck(
                                        id = "weather_recheck_error",
                                        description = "Verificacion meteorologica posterior",
                                        result = AutoSupportResult.ERROR,
                                        detail = "No fue posible verificar los datos: ${e.javaClass.simpleName}",
                                    )
                                )
                            }
                            if (afterChecks.any { it.id == "weather_recheck_error" }) {
                                verifiedWeatherChecks = weatherChecks + afterChecks
                            } else {
                                verifiedWeatherChecks = AutoSupportWeatherRepairVerifier.compare(
                                    before = weatherChecks,
                                    after = afterChecks,
                                    repairStartedAtMillis = repairStartedAtMillis,
                                )
                            }
                        }
                        if (assessment.shouldRecheckWeather) {
                            val verifiedReport = AutoSupportReport(
                                checks + verifiedWeatherChecks + workChecks
                            )
                            val workFailureWarning = if (
                                outcome == AutoSupportWeatherWorkTracker.Outcome.FALLIDO
                            ) {
                                " El trabajo meteorologico informo un fallo; los resultados pueden ser parciales."
                            } else {
                                ""
                            }
                            when {
                                verifiedReport.repairedCount > 0 ->
                                    "Se verificaron ${verifiedReport.repairedCount} reparaciones meteorologicas mediante nuevas fechas de adquisicion. Quedan ${verifiedReport.pendingCount} problemas pendientes."
                                verifiedReport.pendingCount > 0 ->
                                    "Se comprobo nuevamente el pronostico, pero no se confirmo ninguna reparacion. Quedan ${verifiedReport.pendingCount} problemas pendientes."
                                else ->
                                    "Se comprobo nuevamente el pronostico. No quedan problemas detectados, pero no se confirmo ninguna reparacion nueva."
                            }
                            .plus(workFailureWarning)
                        } else {
                            assessment.message
                        }
                    }
                }
                AutoSupportWeatherRefresh.RequestResult.SOLICITUD_NO_VERIFICADA ->
                    "No se pudo confirmar una solicitud nueva. Reparacion pendiente."
                AutoSupportWeatherRefresh.RequestResult.YA_EXISTE_TRABAJO ->
                    "Ya existe una actualizacion meteorologica activa o pendiente. No se creo otra."
                AutoSupportWeatherRefresh.RequestResult.NO_SOLICITADA ->
                    "WorkManager no acepto una solicitud nueva. Reparacion pendiente."
                AutoSupportWeatherRefresh.RequestResult.SIN_UBICACION ->
                    "No existe una ubicacion principal para actualizar."
                AutoSupportWeatherRefresh.RequestResult.ERROR ->
                    "No fue posible solicitar la actualizacion meteorologica."
            }
        }

        if (
            !internetValidated &&
            AutoSupportWeatherRepairPolicy.shouldRequestRefresh(weatherChecks)
        ) {
            repairMessage = "No se intento actualizar el pronostico porque Android no confirmo una conexion a Internet validada. Los problemas meteorologicos siguen pendientes."
        }

        if (
            internetValidated &&
            AutoSupportWeatherRepairPolicy.shouldRequestRefresh(weatherChecks) &&
            (!weatherQueueAvailable || weatherQueueBusy)
        ) {
            repairMessage = if (weatherQueueBusy) {
                "Ya existe una actualizacion meteorologica pendiente o en ejecucion. No se inicio otra; los datos se comprobaran en una ejecucion posterior del autosoporte."
            } else {
                "No se pudo verificar el estado de WorkManager. Por seguridad no se solicito otra actualizacion."
            }
        }

        val report = AutoSupportReport(checks + verifiedWeatherChecks + workChecks)
        emit(
            Progress(
                phase = AutoSupportPhase.FINALIZADO,
                report = report,
                message = repairMessage ?: if (report.completedWithoutPendingIssues) {
                    "Diagnostico completado sin problemas detectados"
                } else {
                    "Diagnostico completado con ${report.pendingCount} problemas pendientes"
                },
            )
        )
    }.flowOn(Dispatchers.Default)
}
