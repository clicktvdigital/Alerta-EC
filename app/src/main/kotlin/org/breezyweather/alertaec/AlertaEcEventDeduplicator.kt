package org.breezyweather.alertaec

import android.content.Context
import org.breezyweather.domain.settings.ConfigStore

object AlertaEcEventDeduplicator {

    private const val PREFERENCES = "ALERTA_EC_EVENT_DEDUPLICATION"
    private const val KEY_NOTIFIED_EVENT_IDS = "NOTIFIED_EVENT_IDS"
    private const val MAX_REMEMBERED_EVENTS = 100

    fun filterNewEvents(context: Context, events: List<AlertaEcEvent>): List<AlertaEcEvent> {
        if (events.isEmpty()) return emptyList()

        val config = ConfigStore(context, PREFERENCES)
        val remembered = config.getStringSet(KEY_NOTIFIED_EVENT_IDS, emptySet())
            ?.filterNotNull()
            ?.toMutableSet()
            ?: mutableSetOf()

        return AlertaEcNotificationPolicy.filterNewEvents(events, remembered)
    }

    internal fun notifiedEventIds(context: Context): Set<String> =
        ConfigStore(context, PREFERENCES).getStringSet(KEY_NOTIFIED_EVENT_IDS, emptySet())
            ?.filterNotNull()
            ?.toSet()
            ?: emptySet()

    internal fun isStillUnnotified(eventId: String, notifiedEventIds: Set<String>): Boolean =
        eventId !in notifiedEventIds

    fun markNotified(context: Context, event: AlertaEcEvent) {
        val config = ConfigStore(context, PREFERENCES)
        val remembered = config.getStringSet(KEY_NOTIFIED_EVENT_IDS, emptySet())
            ?.filterNotNull()
            ?.toMutableList()
            ?: mutableListOf()

        remembered.remove(event.id)
        remembered.add(event.id)

        val trimmed = remembered.takeLast(MAX_REMEMBERED_EVENTS).toSet()
        config.edit().putStringSet(KEY_NOTIFIED_EVENT_IDS, trimmed).apply()
    }

    fun clear(context: Context) {
        ConfigStore(context, PREFERENCES).edit().remove(KEY_NOTIFIED_EVENT_IDS).apply()
    }
}
