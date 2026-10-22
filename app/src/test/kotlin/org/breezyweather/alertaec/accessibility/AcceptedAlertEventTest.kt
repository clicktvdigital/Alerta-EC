package org.breezyweather.alertaec.accessibility

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.Instant
import org.breezyweather.alertaec.AlertaEcPhenomenon
import org.breezyweather.alertaec.AlertaEcSeverity
import org.junit.jupiter.api.Test

class AcceptedAlertEventTest {
    private val start = Instant.parse("2026-10-21T15:00:00Z")
    private val end = Instant.parse("2026-10-21T16:00:00Z")

    @Test
    fun `identity is stable and namespaced by origin`() {
        val official = event(origin = AcceptedAlertEvent.Origin.OFFICIAL)
        val forecast = event(origin = AcceptedAlertEvent.Origin.FORECAST)
        official.id shouldBe "OFFICIAL:$VALID_PRODUCER_EVENT_ID"
        forecast.id shouldBe "FORECAST:$VALID_PRODUCER_EVENT_ID"
        event(origin = AcceptedAlertEvent.Origin.OFFICIAL).id shouldBe official.id
    }

    @Test
    fun `producer event id rejects non versioned unsafe or invalid length values`() {
        listOf(
            "",
            "v1-0123456789abcdef",
            "v1-00000000000000000000000000000000000000000000000000000000000000000",
            "producer-event",
            "Quito",
            "-0.1807,-78.4678",
            "https://example.com/alert/1",
            "v1-000000000000000000000000000000000000000000000000000000000000000 ",
            "v1-00000000000000000000000000000000\u00000000000000000000000000000000000",
            "V1-0000000000000000000000000000000000000000000000000000000000000000",
            "v2-0000000000000000000000000000000000000000000000000000000000000000",
            "v1-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        ).forEach { invalidId ->
            shouldThrow<IllegalArgumentException> { event(producerEventId = invalidId) }
        }
    }

    @Test
    fun `producer event id accepts the versioned opaque digest format`() {
        event(producerEventId = VALID_PRODUCER_EVENT_ID).producerEventId shouldBe VALID_PRODUCER_EVENT_ID
        event(producerEventId = VALID_PRODUCER_EVENT_ID_2).producerEventId shouldBe VALID_PRODUCER_EVENT_ID_2
    }

    @Test
    fun `required fields reject invalid temporal payloads`() {
        shouldThrow<IllegalArgumentException> { event(end = start) }
    }

    @Test
    fun `evidence explicitly states accepted without backlog`() {
        event().acceptanceEvidence shouldBe AcceptedAlertEvent.AcceptanceEvidence.NOTIFICATION_ACCEPTED_WITHOUT_BACKLOG
    }

    @Test
    fun `offer without a collector is not available to a future subscriber`() = runTest {
        val bus = AcceptedAlertEventBus()
        bus.tryEmit(event()) shouldBe true
        bus.events.replayCache.shouldBeEmpty()

        val received = mutableListOf<AcceptedAlertEvent>()
        val collector = launch {
            bus.events.collect { received += it }
        }
        runCurrent()
        received shouldBe emptyList()
        collector.cancel()
    }


    @Test
    fun `active collector receives an accepted offer`() = runTest {
        val bus = AcceptedAlertEventBus()
        val collectorReady = CompletableDeferred<Unit>()
        val received = mutableListOf<AcceptedAlertEvent>()
        val collector = launch {
            bus.events
                .onSubscription { collectorReady.complete(Unit) }
                .collect { received += it }
        }

        collectorReady.await()
        val expected = event()
        bus.tryEmit(expected) shouldBe true
        runCurrent()

        received shouldBe listOf(expected)
        collector.cancel()
    }


    @Test
    fun `burst keeps the newest buffered event when collector is busy`() = runTest {
        val bus = AcceptedAlertEventBus()
        val collectorReady = CompletableDeferred<Unit>()
        val firstReceived = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val received = mutableListOf<AcceptedAlertEvent>()
        val collector = launch {
            bus.events
                .onSubscription { collectorReady.complete(Unit) }
                .collect {
                    received += it
                    firstReceived.complete(Unit)
                    releaseCollector.await()
                }
        }

        collectorReady.await()
        val first = event(producerEventId = VALID_PRODUCER_EVENT_ID)
        val second = event(producerEventId = VALID_PRODUCER_EVENT_ID_2)
        val third = event(producerEventId = VALID_PRODUCER_EVENT_ID_3)
        bus.tryEmit(first) shouldBe true
        firstReceived.await()
        bus.tryEmit(second) shouldBe true
        bus.tryEmit(third) shouldBe true

        releaseCollector.complete(Unit)
        runCurrent()

        received shouldBe listOf(first, third)
        collector.cancel()
    }


    @Test
    fun `text is generated from allowed values and remains brief`() {
        val accepted = event()
        accepted.briefTextSpanish shouldBe "Alerta advertencia de tormenta eléctrica."
        (accepted.briefTextSpanish.length <= 80) shouldBe true
        accepted.briefTextSpanish.contains("http") shouldBe false
    }

    private fun event(
        producerEventId: String = VALID_PRODUCER_EVENT_ID,
        origin: AcceptedAlertEvent.Origin = AcceptedAlertEvent.Origin.FORECAST,
        end: Instant = this.end,
    ) = AcceptedAlertEvent(
        producerEventId = producerEventId,
        origin = origin,
        phenomenon = AlertaEcPhenomenon.THUNDERSTORM,
        severity = AlertaEcSeverity.WARNING,
        start = start,
        end = end,
        acceptanceEvidence = AcceptedAlertEvent.AcceptanceEvidence.NOTIFICATION_ACCEPTED_WITHOUT_BACKLOG,
    )

    private companion object {
        private const val VALID_PRODUCER_EVENT_ID =
            "v1-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val VALID_PRODUCER_EVENT_ID_2 =
            "v1-1123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val VALID_PRODUCER_EVENT_ID_3 =
            "v1-2123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
