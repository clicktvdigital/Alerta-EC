package org.breezyweather.background.location

import android.content.Context

sealed interface RemoteLocationSyncResult {
    data class SessionCreated(
        val sessionId: String,
        val shareToken: String,
        val expiresAt: Long,
    ) : RemoteLocationSyncResult

    data object Updated : RemoteLocationSyncResult
    data object Revoked : RemoteLocationSyncResult

    data class Failed(
        val error: Throwable,
    ) : RemoteLocationSyncResult
}

class RemoteLocationSynchronizer(
    private val api: RemoteLocationApi,
) {

    suspend fun createSession(
        context: Context,
        session: LocationSharingSession,
    ): RemoteLocationSyncResult {
        val location = session.latestLocation ?: ActiveLocationController.getLatest(context)
            ?: return RemoteLocationSyncResult.Failed(
                IllegalStateException("No location available")
            )

        return runCatching {
            api.createSession(location.toRemoteUpdate(session))
        }.fold(
            onSuccess = {
                RemoteLocationSyncResult.SessionCreated(
                    sessionId = it.sessionId,
                    shareToken = it.shareToken,
                    expiresAt = it.expiresAt,
                )
            },
            onFailure = { RemoteLocationSyncResult.Failed(it) },
        )
    }

    suspend fun update(
        context: Context,
        session: LocationSharingSession,
    ): RemoteLocationSyncResult {
        if (!session.isActive || session.isExpired) {
            return RemoteLocationSyncResult.Failed(
                IllegalStateException("Location sharing session is not active")
            )
        }

        val location = ActiveLocationController.getLatest(context)
            ?: return RemoteLocationSyncResult.Failed(
                IllegalStateException("No location available")
            )

        return runCatching {
            api.updateLocation(
                sessionId = session.sessionId,
                update = location.toRemoteUpdate(session),
            )
        }.fold(
            onSuccess = {
                if (it.accepted) {
                    RemoteLocationSyncResult.Updated
                } else {
                    RemoteLocationSyncResult.Failed(
                        IllegalStateException("Remote update was rejected")
                    )
                }
            },
            onFailure = { RemoteLocationSyncResult.Failed(it) },
        )
    }

    suspend fun revoke(sessionId: String): RemoteLocationSyncResult {
        return runCatching {
            api.revokeSession(sessionId)
        }.fold(
            onSuccess = {
                if (it.accepted) {
                    RemoteLocationSyncResult.Revoked
                } else {
                    RemoteLocationSyncResult.Failed(
                        IllegalStateException("Remote revocation was rejected")
                    )
                }
            },
            onFailure = { RemoteLocationSyncResult.Failed(it) },
        )
    }

    private fun ActiveLocationSnapshot.toRemoteUpdate(
        session: LocationSharingSession,
    ) = RemoteLocationUpdate(
        sessionId = session.sessionId,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracy = accuracy,
        measuredAt = timestamp,
        expiresAt = session.expiresAt,
    )
}
