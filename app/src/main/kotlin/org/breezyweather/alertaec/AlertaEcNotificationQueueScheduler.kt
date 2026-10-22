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
            .setInitialDelay(delaySeconds.coerceAtLeast(1L), TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        context.workManager.enqueueUniqueWork(
            WORK_NAME, if (appendToChain) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP, request
        )
    }

    fun scheduleContinuation(
        context: Context,
        result: NotificationPostResult?,
        appendToChain: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val decision = AlertaEcNotificationQueuePolicy.continuation(
            result = result,
            hasPending = AlertaEcNotificationQueueStore.pending(context).isNotEmpty(),
            lastShownAtMillis = AlertaEcNotificationQueueStore.lastShownAt(context),
            nowMillis = nowMillis,
        ) ?: return
        schedule(context, decision.delayMillis.ceilSeconds(), appendToChain)
    }

    fun scheduleAfterDismissal(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val decision = AlertaEcNotificationQueuePolicy.continuationAfterDismissal(
            hasPending = AlertaEcNotificationQueueStore.pending(context).isNotEmpty(),
            lastShownAtMillis = AlertaEcNotificationQueueStore.lastShownAt(context),
            nowMillis = nowMillis,
        ) ?: return
        schedule(context, decision.delayMillis.ceilSeconds(), appendToChain = true)
    }

    private fun Long.ceilSeconds(): Long = (this + 999L) / 1000L
}
