package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AlertaEcNotificationQueuePolicyTest {
    @Test
    fun nextValidPendingIdsExcludesNotifiedIdsAndPreservesFifo() {
        AlertaEcNotificationQueuePolicy.nextValidPendingIds(
            pendingIds = listOf("old", "already", "first", "first", "last"),
            validEventIds = setOf("already", "first", "last"),
            notifiedEventIds = setOf("already"),
        ) shouldBe listOf("first", "last")
    }

    @Test
    fun nextValidPendingIdsKeepsOnlyFirstHundredWithoutReordering() {
        val ids = (1..101).map { "id-$it" }
        AlertaEcNotificationQueuePolicy.nextValidPendingIds(
            pendingIds = ids,
            validEventIds = ids.toSet(),
            notifiedEventIds = emptySet(),
        ) shouldBe ids.take(100)
    }
}
