package org.breezyweather.ui.map

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InamhiStyleStateTest {
    @Test
    fun duplicateFailuresAndLateSuccessCannotFinishNewAttempt() {
        val state = InamhiStyleState()
        val first = requireNotNull(state.begin())
        assertNull(state.begin())
        assertTrue(state.complete(first))
        assertFalse(state.complete(first))
        val retry = requireNotNull(state.begin())
        assertFalse(state.complete(first))
        assertTrue(state.complete(retry))
        assertTrue(state.exhausted)
        assertNull(state.begin())
    }

    @Test
    fun pauseInvalidatesCallbackWithoutResettingBudget() {
        val state = InamhiStyleState()
        val first = requireNotNull(state.begin())
        state.invalidate()
        assertFalse(state.complete(first))
        assertNotNull(state.begin())
        state.invalidate()
        assertNull(state.begin())
        assertNotNull(InamhiStyleState().begin())
    }
}
