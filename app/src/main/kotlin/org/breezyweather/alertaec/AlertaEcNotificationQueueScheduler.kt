package org.breezyweather.alertaec

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import org.breezyweather.common.extensions.workManager
import java.util.concurrent.TimeUnit

internal object AlertaEcNotificationQueueScheduler {
    private const val WORK_NAME = "alerta_ec_notification_queue"

    fun schedule(context: Context, delaySeconds: Long = 30L, appendToChain: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<AlertaEcNotificationQueueWorker>()
            .setInitialDelay(delaySeconds.coerceAtLeast(0L), TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        context.workManager.enqueueUniqueWork(
            WORK_NAME, if (appendToChain) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP, request
        )
    }
}
