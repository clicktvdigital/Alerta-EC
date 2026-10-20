package org.breezyweather.ui.map

/** Solo se utiliza en el hilo principal. Una respuesta invalidada nunca libera otra consulta. */
internal class InamhiRequestState {
    private var generation = 0L
    var loading = false
        private set
    private var pending = false

    fun begin(): Long? {
        if (loading) {
            pending = true
            return null
        }
        loading = true
        pending = false
        return ++generation
    }

    fun isCurrent(token: Long): Boolean = loading && token == generation

    fun finish(token: Long): Boolean {
        if (!isCurrent(token)) return false
        loading = false
        return pending.also { pending = false }
    }

    fun invalidate() {
        generation++
        loading = false
        pending = false
    }
}
