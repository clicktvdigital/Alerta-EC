package org.breezyweather.alertaec.support

/**
 * Reglas puras para distinguir trabajos meteorologicos activos de
 * actualizaciones periodicas que solo esperan su proximo horario.
 */
internal object AutoSupportWeatherWorkPolicy {

    internal enum class State {
        ENQUEUED,
        RUNNING,
        BLOCKED,
        SUCCEEDED,
        FAILED,
        CANCELLED,
    }

    fun isBusy(state: State, manual: Boolean): Boolean =
        state == State.RUNNING ||
            (manual && (state == State.ENQUEUED || state == State.BLOCKED))

    fun isPending(state: State, manual: Boolean): Boolean =
        manual && (state == State.ENQUEUED || state == State.BLOCKED)
}
