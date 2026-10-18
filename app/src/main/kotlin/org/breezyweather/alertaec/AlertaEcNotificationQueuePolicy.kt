package org.breezyweather.alertaec

internal object AlertaEcNotificationQueuePolicy {
    const val INFORMATION_DURATION_MILLIS = 30_000L

    fun nextEligibleAt(lastShownAtMillis: Long): Long =
        lastShownAtMillis + INFORMATION_DURATION_MILLIS

    fun canShowNext(lastShownAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis >= nextEligibleAt(lastShownAtMillis)

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
}
