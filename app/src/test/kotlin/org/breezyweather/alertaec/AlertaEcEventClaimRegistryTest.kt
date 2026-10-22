package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test

class AlertaEcEventClaimRegistryTest {
    @Test
    fun twoConcurrentCallersOnlyOneClaimsSameId() {
        val registry = AlertaEcEventClaimRegistry()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val results = (1..2).map {
            executor.submit<Boolean> {
                start.await()
                registry.tryClaim("same")
            }
        }
        start.countDown()
        results.map { it.get(2, TimeUnit.SECONDS) }.count { it } shouldBe 1
        executor.shutdownNow()
    }

    @Test
    fun releaseBeforeNotifyAllowsAClaimAgain() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event") shouldBe true
        registry.release("event")
        registry.tryClaim("event") shouldBe true
    }

    @Test
    fun completeAfterSuccessRemovesClaim() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event")
        registry.complete("event")
        registry.state("event") shouldBe null
        registry.tryClaim("event") shouldBe true
    }

    @Test
    fun bookkeepingFailureRetainsUncertainState() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event")
        registry.retainUncertain("event")
        registry.state("event") shouldBe AlertaEcEventClaimRegistry.State.UNCERTAIN
        registry.tryClaim("event") shouldBe false
    }

    @Test
    fun blockedAndFailedReleaseClaim() {
        listOf(NotificationPostResult.BLOCKED, NotificationPostResult.POST_FAILED).forEach { result ->
            val registry = AlertaEcEventClaimRegistry()
            registry.tryClaim("event")
            registry.resolve("event", result)
            registry.tryClaim("event") shouldBe true
        }
    }

    @Test
    fun differentIdsCanBeClaimedSimultaneously() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("first") shouldBe true
        registry.tryClaim("second") shouldBe true
    }
}
