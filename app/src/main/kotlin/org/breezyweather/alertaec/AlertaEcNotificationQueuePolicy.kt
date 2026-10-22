package org.breezyweather.alertaec

internal object AlertaEcNotificationQueuePolicy {
    const val INFORMATION_DURATION_MILLIS = 30_000L
    const val MAX_PENDING_IDS = 100
    const val DISMISSAL_DELAY_MILLIS = 30_000L

    internal data class Continuation(val delayMillis: Long)

    fun nextEligibleAt(lastShownAtMillis: Long): Long =
        lastShownAtMillis + INFORMATION_DURATION_MILLIS

    fun canShowNext(lastShownAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis >= nextEligibleAt(lastShownAtMillis)

    /**
     * BLOCKED and POST_FAILED have no automatic continuation: the event stays
     * pending for a later ordinary weather update.
     */
    fun continuation(
        result: NotificationPostResult?,
        hasPending: Boolean,
        lastShownAtMillis: Long,
        nowMillis: Long,
    ): Continuation? {
        if (
            !hasPending ||
            result == NotificationPostResult.BLOCKED ||
            result == NotificationPostResult.POST_FAILED ||
            result == NotificationPostResult.POST_REQUEST_RETURNED_WITH_BOOKKEEPING_FAILURE
        ) {
            return null
        }
        return Continuation((nextEligibleAt(lastShownAtMillis) - nowMillis).coerceAtLeast(1L))
    }

    fun continuationAfterDismissal(
        hasPending: Boolean,
        lastShownAtMillis: Long,
        nowMillis: Long,
    ): Continuation? {
        // Sin una marca persistida de una publicación completada no hay evidencia
        // suficiente para reactivar la cola desde un receiver de dismissal.
        if (!hasPending || lastShownAtMillis <= 0L) return null
        val eligibleDelay = (nextEligibleAt(lastShownAtMillis) - nowMillis).coerceAtLeast(0L)
        return Continuation(maxOf(eligibleDelay, DISMISSAL_DELAY_MILLIS))
    }

    fun isPriority(event: AlertaEcEvent): Boolean =
        event.state == AlertaEcEventState.OFFICIAL_EVENT ||
            event.severity == AlertaEcSeverity.WATCH ||
            event.severity == AlertaEcSeverity.WARNING ||
            event.severity == AlertaEcSeverity.EMERGENCY

    fun informational(events: List<AlertaEcEvent>, nowMillis: Long): List<AlertaEcEvent> =
        events.filter { !isPriority(it) && AlertaEcNotificationPolicy.shouldNotify(it, nowMillis) }
            .distinctBy { it.id }

    fun priority(events: List<AlertaEcEvent>): List<AlertaEcEvent> =
        events.filter(::isPriority).distinctBy { it.id }

    /** Limpia sin reordenar: solo elimina IDs no válidos o ya notificados. */
    fun nextValidPendingIds(
        pendingIds: List<String>,
        validEventIds: Set<String>,
        notifiedEventIds: Set<String>,
    ): List<String> = pendingIds.asSequence()
        .filter { it in validEventIds && it !in notifiedEventIds }
        .distinct()
        .take(MAX_PENDING_IDS)
        .toList()
}
