package org.breezyweather.alertaec

import android.content.Context
import org.breezyweather.domain.settings.ConfigStore

internal object AlertaEcNotificationQueueStore {
    private const val NAME = "ALERTA_EC_NOTIFICATION_QUEUE"
    private const val KEY_PENDING = "pending_ids"
    private const val KEY_LAST_SHOWN = "last_shown_at"
    private const val KEY_LOCATION = "location_id"

    @Synchronized fun saveLocation(context: Context, formattedId: String) {
        val config = ConfigStore(context, NAME)
        if (config.getString(KEY_LOCATION, null) == formattedId) return
        config.edit().putString(KEY_LOCATION, formattedId)
            .putString(KEY_PENDING, "")
            .putLong(KEY_LAST_SHOWN, 0L)
            .apply()
    }

    fun locationId(context: Context): String? =
        ConfigStore(context, NAME).getString(KEY_LOCATION, null)


    @Synchronized fun pending(context: Context): List<String> =
        ConfigStore(context, NAME).getString(KEY_PENDING, "")
            ?.split("\n")?.filter { it.isNotBlank() }?.distinct() ?: emptyList()

    @Synchronized fun save(context: Context, ids: List<String>) {
        ConfigStore(context, NAME).edit()
            .putString(KEY_PENDING, ids.distinct().take(AlertaEcNotificationQueuePolicy.MAX_PENDING_IDS).joinToString("\n"))
            .apply()
    }

    @Synchronized fun enqueue(context: Context, eventIds: List<String>) {
        save(context, pending(context) + eventIds)
    }

    @Synchronized fun nextValid(context: Context, events: List<AlertaEcEvent>, nowMillis: Long): AlertaEcEvent? {
        val valid = AlertaEcNotificationQueuePolicy.informational(events, nowMillis).associateBy { it.id }
        val ids = AlertaEcNotificationQueuePolicy.nextValidPendingIds(
            pendingIds = pending(context),
            validEventIds = valid.keys,
            notifiedEventIds = AlertaEcEventDeduplicator.notifiedEventIds(context),
        )
        save(context, ids)
        return ids.firstNotNullOfOrNull { valid[it] }
    }

    @Synchronized fun remove(context: Context, eventId: String) {
        save(context, pending(context).filterNot { it == eventId })
    }

    fun lastShownAt(context: Context): Long =
        ConfigStore(context, NAME).getLong(KEY_LAST_SHOWN, 0L)

    fun markShown(context: Context, timestamp: Long) {
        ConfigStore(context, NAME).edit()
            .putLong(KEY_LAST_SHOWN, timestamp)
            .apply()
    }
}
