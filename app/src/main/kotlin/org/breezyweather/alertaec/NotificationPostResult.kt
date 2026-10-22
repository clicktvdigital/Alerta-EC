package org.breezyweather.alertaec

/**
 * Resultado de la solicitud de publicación de una notificación.
 *
 * `POST_REQUEST_RETURNED` solo significa que la llamada a `notify` retornó sin
 * excepción; no garantiza visualización, sonido ni entrega al usuario.
 * `POST_FAILED` conserva el comportamiento normal de reintento/estado, pero
 * una excepción de notify no demuestra ausencia absoluta de efectos Android.
 */
internal enum class NotificationPostResult {
    BLOCKED,
    POST_REQUEST_RETURNED,
    /** notify retornó, pero el bookkeeping posterior quedó incompleto; se reconsidera en una actualización normal. */
    POST_REQUEST_RETURNED_WITH_BOOKKEEPING_FAILURE,
    POST_FAILED,
}

internal enum class NotificationBookkeepingResult {
    COMPLETED,
    FAILED,
}

/** Fase observable para que el caller pueda cerrar correctamente un claim ante errores graves. */
internal enum class NotificationPostPhase {
    NOT_INVOKED,
    INVOKED,
    RETURNED,
}

/**
 * Ejecuta el preflight y, si está permitido, una solicitud de publicación.
 * Las excepciones de runtime dejan el evento pendiente y no ejecutan [onPostReturned].
 * [poster] debe invocar el callback recibido justo antes de iniciar la llamada
 * real a notify; así la fase no se adelanta a la construcción de la notificación.
 */
internal fun postInformationalNotification(
    canNotifyEvent: () -> Boolean,
    poster: (onNotifyInvoked: () -> Unit) -> Unit,
    onPostReturned: () -> NotificationBookkeepingResult,
    onPhaseChanged: (NotificationPostPhase) -> Unit = {},
): NotificationPostResult {
    val allowed = try {
        canNotifyEvent()
    } catch (_: SecurityException) {
        return NotificationPostResult.POST_FAILED
    } catch (_: IllegalArgumentException) {
        return NotificationPostResult.POST_FAILED
    } catch (_: RuntimeException) {
        return NotificationPostResult.POST_FAILED
    }

    if (!allowed) return NotificationPostResult.BLOCKED

    val result = try {
        poster { onPhaseChanged(NotificationPostPhase.INVOKED) }
        onPhaseChanged(NotificationPostPhase.RETURNED)
        NotificationPostResult.POST_REQUEST_RETURNED
    } catch (_: SecurityException) {
        NotificationPostResult.POST_FAILED
    } catch (_: IllegalArgumentException) {
        NotificationPostResult.POST_FAILED
    } catch (_: RuntimeException) {
        NotificationPostResult.POST_FAILED
    }

    if (result != NotificationPostResult.POST_REQUEST_RETURNED) return result

    return try {
        when (onPostReturned()) {
            NotificationBookkeepingResult.COMPLETED -> result
            NotificationBookkeepingResult.FAILED ->
                NotificationPostResult.POST_REQUEST_RETURNED_WITH_BOOKKEEPING_FAILURE
        }
    } catch (_: RuntimeException) {
        NotificationPostResult.POST_REQUEST_RETURNED_WITH_BOOKKEEPING_FAILURE
    }
}

/**
 * Ejecuta el bookkeeping en orden y se detiene en el primer efecto que falla.
 * No intenta reconstruir ni inventar el estado de los efectos posteriores.
 */
internal fun bookkeepInformationalNotification(
    markNotified: () -> Unit,
    remove: () -> Unit,
    markShown: () -> Unit,
): NotificationBookkeepingResult {
    return try {
        markNotified()
        remove()
        markShown()
        NotificationBookkeepingResult.COMPLETED
    } catch (_: RuntimeException) {
        NotificationBookkeepingResult.FAILED
    }
}
