package org.breezyweather.ui.map

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InamhiRequestStateTest {
    @Test
    fun coalescesPendingRefreshesIntoOneRequest() {
        val state = InamhiRequestState()
        val first = requireNotNull(state.begin())
        repeat(5) { assertNull(state.begin()) }
        assertTrue(state.finish(first))
        val second = requireNotNull(state.begin())
        assertFalse(state.finish(second))
        assertFalse(state.loading)
    }

    @Test
    fun invalidatedResponseCannotFinishOrOverwriteNewRequest() {
        val state = InamhiRequestState()
        val old = requireNotNull(state.begin())
        state.invalidate() // Cambio de preferencias, pausa o destrucción de la vista.
        val current = requireNotNull(state.begin())
        assertFalse(state.isCurrent(old))
        assertFalse(state.finish(old))
        assertTrue(state.loading)
        assertTrue(state.isCurrent(current))
        assertFalse(state.finish(current))
    }

    @Test
    fun destroyedViewDropsPendingRefreshAndRejectsRepeatedCompletion() {
        val state = InamhiRequestState()
        val old = requireNotNull(state.begin())
        state.begin()
        state.invalidate()
        assertFalse(state.finish(old))
        val recreated = requireNotNull(state.begin())
        assertFalse(state.finish(recreated))
        assertFalse(state.isCurrent(recreated))
        assertFalse(state.finish(recreated))
    }
}
