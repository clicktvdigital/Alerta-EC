package org.breezyweather.alertaec

import android.content.Context
import org.breezyweather.domain.settings.ConfigStore

object AlertaEcEventDeduplicator {

    private const val PREFERENCES = "ALERTA_EC_EVENT_DEDUPLICATION"
    private const val KEY_NOTIFIED_EVENT_IDS = "NOTIFIED_EVENT_IDS"
    private const val KEY_NOTIFIED_HISTORY_V2 = "NOTIFIED_HISTORY_V2"
    private const val MAX_REMEMBERED_EVENTS = 300
    private const val RETENTION_MILLIS = 48 * 60 * 60 * 1000L

    private fun loadOrderedEntries(config: ConfigStore, nowMillis: Long): MutableList<Pair<String, Long>> {
        val raw = config.getString(KEY_NOTIFIED_HISTORY_V2, null)
        val entries = mutableListOf<Pair<String, Long>>()
        if (!raw.isNullOrBlank()) {
            raw.lineSequence().forEach { line ->
                val sep = line.lastIndexOf("|")
                if (sep > 0) {
                    val id = line.substring(0, sep)
                    val ts = line.substring(sep + 1).toLongOrNull() ?: nowMillis
                    if (nowMillis - ts <= RETENTION_MILLIS) {
                        entries.add(id to ts)
                    }
                }
            }
        } else {
            // Migracion unica desde el StringSet heredado
            config.getStringSet(KEY_NOTIFIED_EVENT_IDS, emptySet())
                ?.filterNotNull()
                ?.forEach { id -> entries.add(id to nowMillis) }
        }
        return entries
    }

    private fun saveOrderedEntries(config: ConfigStore, entries: List<Pair<String, Long>>) {
        val trimmed = entries.takeLast(MAX_REMEMBERED_EVENTS)
        val serialized = trimmed.joinToString("\n") { "${it.first}|${it.second}" }
        config.edit()
            .putString(KEY_NOTIFIED_HISTORY_V2, serialized)
            .remove(KEY_NOTIFIED_EVENT_IDS)
            .apply()
    }

    fun filterNewEvents(context: Context, events: List<AlertaEcEvent>): List<AlertaEcEvent> {
        if (events.isEmpty()) return emptyList()
        val remembered = notifiedEventIds(context)
        return AlertaEcNotificationPolicy.filterNewEvents(events, remembered)
    }

    internal fun notifiedEventIds(context: Context): Set<String> {
        val config = ConfigStore(context, PREFERENCES)
        val nowMillis = System.currentTimeMillis()
        return loadOrderedEntries(config, nowMillis).map { it.first }.toSet()
    }

    internal fun isStillUnnotified(eventId: String, notifiedEventIds: Set<String>): Boolean =
        eventId !in notifiedEventIds

    fun markNotified(context: Context, event: AlertaEcEvent) {
        val config = ConfigStore(context, PREFERENCES)
        val nowMillis = System.currentTimeMillis()
        val entries = loadOrderedEntries(config, nowMillis)
        entries.removeAll { it.first == event.id }
        entries.add(event.id to nowMillis)
        saveOrderedEntries(config, entries)
    }

    fun clear(context: Context) {
        ConfigStore(context, PREFERENCES).edit()
            .remove(KEY_NOTIFIED_EVENT_IDS)
            .remove(KEY_NOTIFIED_HISTORY_V2)
            .apply()
    }
}
