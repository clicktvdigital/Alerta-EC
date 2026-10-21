package org.breezyweather.ui.emergency

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.breezyweather.alertaec.info.OfficialInfoCachedItem
import org.breezyweather.alertaec.info.OfficialInfoCategory
import org.breezyweather.alertaec.info.OfficialInfoFreshness
import org.breezyweather.alertaec.info.OfficialInfoItem
import org.breezyweather.alertaec.info.OfficialInfoSelectionReadState
import org.breezyweather.alertaec.info.OfficialInfoSelectionResult
import org.breezyweather.alertaec.info.OfficialInfoSelector
import org.breezyweather.alertaec.info.OfficialInfoVerification
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EmergencyCenterViewModelTest {

    @AfterEach
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialEmptySelectionPublishesEmptyStateAndUsesInjectedClock() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        every { selector.select(1_000L) } returns selection(OfficialInfoSelectionReadState.EMPTY)

        val viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { 1_000L },
            readDispatcher = dispatcher,
        )
        assertEquals(true, viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(EmergencyCenterUiState(readState = OfficialInfoSelectionReadState.EMPTY, isLoading = false), viewModel.uiState.value)
        verify(exactly = 1) { selector.select(1_000L) }
    }

    @Test
    fun loadedSelectionPublishesCurrentAndHistory() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        val current = cachedItem("current", OfficialInfoFreshness.ACTIVE)
        val history = cachedItem("history", OfficialInfoFreshness.EXPIRED)
        every { selector.select(2_000L) } returns selection(
            readState = OfficialInfoSelectionReadState.LOADED,
            current = listOf(current),
            history = listOf(history),
        )

        val viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { 2_000L },
            readDispatcher = dispatcher,
        )
        assertEquals(true, viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(listOf(current), viewModel.uiState.value.current)
        assertEquals(listOf(history), viewModel.uiState.value.history)
        assertEquals(OfficialInfoSelectionReadState.LOADED, viewModel.uiState.value.readState)
    }

    @Test
    fun readErrorSelectionPublishesReadErrorWithoutEntries() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        every { selector.select(3_000L) } returns selection(OfficialInfoSelectionReadState.READ_ERROR)

        val viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { 3_000L },
            readDispatcher = dispatcher,
        )
        assertEquals(true, viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(emptyList<OfficialInfoCachedItem>(), viewModel.uiState.value.current)
        assertEquals(emptyList<OfficialInfoCachedItem>(), viewModel.uiState.value.history)
        assertEquals(OfficialInfoSelectionReadState.READ_ERROR, viewModel.uiState.value.readState)
    }

    @Test
    fun reloadReadsAgainWithUpdatedInjectedClock() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        var now = 4_000L
        val first = cachedItem("first", OfficialInfoFreshness.ACTIVE)
        val second = cachedItem("second", OfficialInfoFreshness.ACTIVE)
        every { selector.select(any()) } answers {
            when (val nowMillis = firstArg<Long>()) {
                4_000L -> selection(OfficialInfoSelectionReadState.LOADED, current = listOf(first))
                5_000L -> selection(OfficialInfoSelectionReadState.LOADED, current = listOf(second))
                else -> error("unexpected clock value: $nowMillis")
            }
        }

        val viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { now },
            readDispatcher = dispatcher,
        )
        assertEquals(true, viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(listOf(first), viewModel.uiState.value.current)
        assertEquals(emptyList<OfficialInfoCachedItem>(), viewModel.uiState.value.history)
        assertEquals(OfficialInfoSelectionReadState.LOADED, viewModel.uiState.value.readState)

        now = 5_000L
        viewModel.reload()
        assertEquals(true, viewModel.uiState.value.isLoading)
        assertEquals(listOf(first), viewModel.uiState.value.current)
        assertEquals(emptyList<OfficialInfoCachedItem>(), viewModel.uiState.value.history)
        assertEquals(OfficialInfoSelectionReadState.LOADED, viewModel.uiState.value.readState)
        advanceUntilIdle()

        assertEquals(listOf(second), viewModel.uiState.value.current)
        verify(exactly = 1) { selector.select(4_000L) }
        verify(exactly = 1) { selector.select(5_000L) }
    }

    @Test
    fun unexpectedReadFailurePublishesReadError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        every { selector.select(6_000L) } throws IllegalStateException("unexpected read failure")

        val viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { 6_000L },
            readDispatcher = dispatcher,
        )
        assertEquals(true, viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        assertEquals(EmergencyCenterUiState(readState = OfficialInfoSelectionReadState.READ_ERROR, isLoading = false), viewModel.uiState.value)
    }

    @Test
    fun reloadCancelsInitialReadBeforeSynchronousSelectionStarts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        every { selector.select(7_000L) } returns selection(
            OfficialInfoSelectionReadState.LOADED,
            current = listOf(cachedItem("reloaded", OfficialInfoFreshness.ACTIVE)),
        )

        val viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { 7_000L },
            readDispatcher = dispatcher,
        )
        // select es síncrona: la cancelación cooperativa de una llamada ya ejecutándose no está garantizada.
        verify(exactly = 0) { selector.select(7_000L) }
        viewModel.reload()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.current.size)
        verify(exactly = 1) { selector.select(7_000L) }
    }

    @Test
    fun cancelledReadCannotPublishObsoleteResultAfterNewReload() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val selector = mockk<OfficialInfoSelector>()
        val first = cachedItem("first", OfficialInfoFreshness.ACTIVE)
        val stale = cachedItem("stale", OfficialInfoFreshness.ACTIVE)
        val fresh = cachedItem("fresh", OfficialInfoFreshness.ACTIVE)
        var selectionCount = 0
        var viewModel: EmergencyCenterViewModel? = null
        every { selector.select(8_000L) } answers {
            when (++selectionCount) {
                1 -> selection(OfficialInfoSelectionReadState.LOADED, current = listOf(first))
                2 -> {
                    viewModel!!.reload()
                    selection(OfficialInfoSelectionReadState.LOADED, current = listOf(stale))
                }
                3 -> selection(OfficialInfoSelectionReadState.LOADED, current = listOf(fresh))
                else -> error("unexpected selection count: $selectionCount")
            }
        }

        viewModel = EmergencyCenterViewModel(
            selector = selector,
            clock = EmergencyCenterClock { 8_000L },
            readDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertEquals(listOf(first), viewModel!!.uiState.value.current)

        viewModel!!.reload()
        assertEquals(true, viewModel!!.uiState.value.isLoading)
        assertEquals(listOf(first), viewModel!!.uiState.value.current)
        advanceUntilIdle()

        assertEquals(listOf(fresh), viewModel!!.uiState.value.current)
        assertEquals(false, viewModel!!.uiState.value.isLoading)
        verify(exactly = 3) { selector.select(8_000L) }
    }

    private fun selection(
        readState: OfficialInfoSelectionReadState,
        current: List<OfficialInfoCachedItem> = emptyList(),
        history: List<OfficialInfoCachedItem> = emptyList(),
    ) = OfficialInfoSelectionResult(current, history, readState)

    private fun cachedItem(
        bulletinId: String,
        freshness: OfficialInfoFreshness,
    ) = OfficialInfoCachedItem(
        item = OfficialInfoItem(
            sourceId = "synthetic",
            bulletinId = bulletinId,
            category = OfficialInfoCategory.ALERT,
            title = "Aviso $bulletinId",
            summary = "Resumen sintético",
            body = "Contenido sintético",
            country = "Ecuador",
            province = "Pichincha",
            canton = "Quito",
            region = "Sierra",
            publishedAtMillis = 1_000L,
            updatedAtMillis = 1_000L,
            fetchedAtMillis = 1_100L,
            expiresAtMillis = 10_000L,
            verification = OfficialInfoVerification.OFFICIAL,
            sourceUrl = "https://example.test/$bulletinId",
        ),
        freshness = freshness,
    )
}
