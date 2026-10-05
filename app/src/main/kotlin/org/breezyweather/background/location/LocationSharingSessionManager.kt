package org.breezyweather.background.location

import android.content.Context
import java.util.UUID

enum class LocationSharingMode {
    SOS,
    TRUSTED_CONTACTS,
}

data class LocationSharingSession(
    val sessionId: String,
    val mode: LocationSharingMode,
    val startedAt: Long,
    val expiresAt: Long,
    val isActive: Boolean,
    val latestLocation: ActiveLocationSnapshot?,
) {
    val remainingMillis: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)

    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt
}

object LocationSharingSessionManager {

    private const val PREFS_NAME = "alerta_ec_location_sharing"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_MODE = "mode"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_ACTIVE = "active"

    const val DEFAULT_DURATION_MILLIS = 60L * 60L * 1000L
    const val MAX_DURATION_MILLIS = 24L * 60L * 60L * 1000L

    fun start(
        context: Context,
        mode: LocationSharingMode,
        durationMillis: Long = DEFAULT_DURATION_MILLIS,
    ): LocationSharingSession {
        val duration = durationMillis.coerceIn(60_000L, MAX_DURATION_MILLIS)
        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_MODE, mode.name)
            .putLong(KEY_STARTED_AT, now)
            .putLong(KEY_EXPIRES_AT, now + duration)
            .putBoolean(KEY_ACTIVE, true)
            .apply()

        ActiveLocationController.start(context)
        return get(context)!!
    }

    fun stop(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, false)
            .apply()
        ActiveLocationController.stop(context)
    }

    fun get(context: Context): LocationSharingSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionId = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val mode = prefs.getString(KEY_MODE, null)
            ?.let { runCatching { LocationSharingMode.valueOf(it) }.getOrNull() }
            ?: return null
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        var active = prefs.getBoolean(KEY_ACTIVE, false)

        if (active && expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
            active = false
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
            ActiveLocationController.stop(context)
        }

        return LocationSharingSession(
            sessionId = sessionId,
            mode = mode,
            startedAt = startedAt,
            expiresAt = expiresAt,
            isActive = active,
            latestLocation = ActiveLocationController.getLatest(context),
        )
    }
}
