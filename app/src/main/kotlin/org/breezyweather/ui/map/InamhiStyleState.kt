package org.breezyweather.ui.map

/** Presupuesto por vista; eventos duplicados o tardíos no consumen otro intento. */
internal class InamhiStyleState {
    private var generation = 0L
    private var active: Long? = null
    var attempts = 0
        private set
    val exhausted: Boolean get() = attempts >= 2

    fun begin(): Long? {
        if (active != null || exhausted) return null
        attempts++
        return (++generation).also { active = it }
    }

    fun complete(token: Long): Boolean {
        if (active != token) return false
        active = null
        return true
    }

    fun invalidate() {
        active = null
        generation++
    }
}
