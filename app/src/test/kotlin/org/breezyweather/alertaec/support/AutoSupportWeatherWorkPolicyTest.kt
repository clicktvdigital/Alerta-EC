package org.breezyweather.alertaec.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutoSupportWeatherWorkPolicyTest {

    @Test
    fun allStatesForPeriodicAndManualWork() {
        val states = AutoSupportWeatherWorkPolicy.State.entries
        for (manual in listOf(false, true)) {
            for (state in states) {
                val expectedBusy = state == AutoSupportWeatherWorkPolicy.State.RUNNING ||
                    (manual && (
                        state == AutoSupportWeatherWorkPolicy.State.ENQUEUED ||
                            state == AutoSupportWeatherWorkPolicy.State.BLOCKED
                        ))
                val expectedPending = manual && (
                    state == AutoSupportWeatherWorkPolicy.State.ENQUEUED ||
                        state == AutoSupportWeatherWorkPolicy.State.BLOCKED
                    )

                assertEquals(
                    expectedBusy,
                    AutoSupportWeatherWorkPolicy.isBusy(state, manual),
                    "busy: state=$state manual=$manual",
                )
                assertEquals(
                    expectedPending,
                    AutoSupportWeatherWorkPolicy.isPending(state, manual),
                    "pending: state=$state manual=$manual",
                )
            }
        }
    }

    @Test
    fun periodicEnqueuedDoesNotBlockRepair() {
        assertEquals(
            false,
            AutoSupportWeatherWorkPolicy.isBusy(
                AutoSupportWeatherWorkPolicy.State.ENQUEUED,
                false,
            ),
        )
    }

    @Test
    fun manualEnqueuedBlocksDuplicateRepair() {
        assertEquals(
            true,
            AutoSupportWeatherWorkPolicy.isBusy(
                AutoSupportWeatherWorkPolicy.State.ENQUEUED,
                true,
            ),
        )
    }

    @Test
    fun periodicRunningBlocksConcurrentRepair() {
        assertEquals(
            true,
            AutoSupportWeatherWorkPolicy.isBusy(
                AutoSupportWeatherWorkPolicy.State.RUNNING,
                false,
            ),
        )
    }
}
