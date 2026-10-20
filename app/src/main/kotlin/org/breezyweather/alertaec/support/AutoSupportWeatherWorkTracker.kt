package org.breezyweather.alertaec.support

import android.content.Context
import androidx.work.WorkInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.breezyweather.common.extensions.workManager
import java.util.UUID

/**
 * Observa un trabajo concreto de WorkManager sin modificarlo.
 * SUCCEEDED solo significa que termino el worker; no prueba
 * que el proveedor meteorologico haya entregado datos nuevos.
 */
internal object AutoSupportWeatherWorkTracker {

    internal enum class Outcome {
        TERMINADO,
        FALLIDO,
        CANCELADO,
        NO_ENCONTRADO,
        TIEMPO_AGOTADO,
        ERROR,
    }

    suspend fun await(
        context: Context,
        workId: String,
        maxChecks: Int = 60,
    ): Outcome = withContext(Dispatchers.IO) {
        try {
            val id = UUID.fromString(workId)
            var checks = 0
            var outcome: Outcome? = null

            while (outcome == null && checks < maxChecks) {
                val work = context.applicationContext.workManager.getWorkInfoById(id).get()
                outcome = when (work?.state) {
                    WorkInfo.State.SUCCEEDED -> Outcome.TERMINADO
                    WorkInfo.State.FAILED -> Outcome.FALLIDO
                    WorkInfo.State.CANCELLED -> Outcome.CANCELADO
                    null -> Outcome.NO_ENCONTRADO
                    else -> null
                }
                checks++
                if (outcome == null && checks < maxChecks) {
                    delay(1000)
                }
            }

            outcome ?: Outcome.TIEMPO_AGOTADO
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("Seguimiento meteorologico interrumpido").also {
                it.initCause(e)
            }
        } catch (e: Exception) {
            Outcome.ERROR
        }
    }
}
