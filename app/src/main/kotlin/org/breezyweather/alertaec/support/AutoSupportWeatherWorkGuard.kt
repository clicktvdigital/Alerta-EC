package org.breezyweather.alertaec.support

import android.content.Context
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.breezyweather.common.extensions.workManager

/**
 * Protege el autosoporte contra trabajos meteorologicos duplicados.
 * Solo consulta WorkManager; no inicia, cancela ni modifica trabajos.
 */
internal object AutoSupportWeatherWorkGuard {

    private const val WEATHER_TAG = "WeatherUpdate"
    private const val MANUAL_TAG = "WeatherUpdate-manual"

    internal data class ManualWork(
        val id: String,
        val state: WorkInfo.State,
    )

    suspend fun manualWorks(context: Context): List<ManualWork> =
        withContext(Dispatchers.IO) {
            try {
                context.applicationContext.workManager
                    .getWorkInfosByTag(MANUAL_TAG)
                    .get()
                    .map { ManualWork(it.id.toString(), it.state) }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw kotlinx.coroutines.CancellationException(
                    "Consulta de trabajos manuales interrumpida"
                ).also { it.initCause(e) }
            }
        }

    internal data class Status(
        val busy: Boolean,
        val pendingCount: Int,
        val runningCount: Int,
    )

    suspend fun inspect(context: Context): Status = withContext(Dispatchers.IO) {
        val works = try {
            context.applicationContext.workManager
                .getWorkInfosByTag(WEATHER_TAG)
                .get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw kotlinx.coroutines.CancellationException(
                "Consulta de actualizaciones meteorologicas interrumpida"
            ).also { it.initCause(e) }
        }

        Status(
            busy = works.any { work ->
                AutoSupportWeatherWorkPolicy.isBusy(
                    AutoSupportWeatherWorkPolicy.State.valueOf(work.state.name),
                    MANUAL_TAG in work.tags,
                )
            },
            pendingCount = works.count { work ->
                AutoSupportWeatherWorkPolicy.isPending(
                    AutoSupportWeatherWorkPolicy.State.valueOf(work.state.name),
                    MANUAL_TAG in work.tags,
                )
            },
            runningCount = works.count {
                it.state == WorkInfo.State.RUNNING
            },
        )
    }
}
