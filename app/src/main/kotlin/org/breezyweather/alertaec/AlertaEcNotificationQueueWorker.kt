package org.breezyweather.alertaec

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import breezyweather.data.location.LocationRepository
import breezyweather.data.weather.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertaEcNotificationQueueWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val id = AlertaEcNotificationQueueStore.locationId(context)
            ?: return Result.success()
        val location = locationRepository.getLocation(id, withParameters = false)
            ?: return Result.success()
        val weather = try {
            weatherRepository.getWeatherByLocationId(
                id, withDaily = false, withHourly = true,
                withMinutely = false, withAlerts = false, withNormals = false
            )
        } catch (error: Exception) {
            return if (runAttemptCount < 10) Result.retry() else Result.success()
        }
        if (AlertaEcNotificationQueueStore.locationId(context) != id) return Result.success()
        if (weather == null) {
            return if (runAttemptCount < 10) Result.retry() else Result.success()
        }
        AlertaEcNotifier.checkAndNotify(context, location.copy(weather = weather), fromQueueWorker = true)
        if (AlertaEcNotificationQueueStore.pending(context).isNotEmpty()) {
            val nowMillis = System.currentTimeMillis()
            val remainingMillis = (AlertaEcNotificationQueuePolicy.nextEligibleAt(
                AlertaEcNotificationQueueStore.lastShownAt(context)
            ) - nowMillis).coerceAtLeast(0L)
            AlertaEcNotificationQueueScheduler.schedule(context, (remainingMillis + 999L) / 1000L, appendToChain = true)
        }
        return Result.success()
    }
}
