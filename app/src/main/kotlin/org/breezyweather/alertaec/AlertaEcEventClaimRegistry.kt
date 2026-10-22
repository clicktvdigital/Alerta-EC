package org.breezyweather.alertaec

/**
 * Registro de single-flight por proceso. El lock solo protege la transición
 * del estado del claim; nunca se conserva durante trabajo de publicación.
 */
internal open class AlertaEcEventClaimRegistry {
    internal enum class State {
        CLAIMED,
        UNCERTAIN,
    }

    private val lock = Any()
    private val claims = mutableMapOf<String, State>()

    fun tryClaim(eventId: String): Boolean = synchronized(lock) {
        if (eventId in claims) return@synchronized false
        claims[eventId] = State.CLAIMED
        true
    }

    fun complete(eventId: String) {
        synchronized(lock) { claims.remove(eventId) }
    }

    fun release(eventId: String) {
        synchronized(lock) { claims.remove(eventId) }
    }

    fun retainUncertain(eventId: String) {
        synchronized(lock) {
            if (eventId in claims) claims[eventId] = State.UNCERTAIN
        }
    }

    fun resolve(eventId: String, result: NotificationPostResult) {
        when (result) {
            NotificationPostResult.BLOCKED,
            NotificationPostResult.POST_FAILED -> release(eventId)
            NotificationPostResult.POST_REQUEST_RETURNED -> complete(eventId)
            NotificationPostResult.POST_REQUEST_RETURNED_WITH_BOOKKEEPING_FAILURE ->
                retainUncertain(eventId)
        }
    }

    fun state(eventId: String): State? = synchronized(lock) { claims[eventId] }
}

/** Singleton de claims para todas las rutas de publicación del proceso. */
internal object AlertaEcEventClaims : AlertaEcEventClaimRegistry()
