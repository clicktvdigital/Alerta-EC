package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationPostResultTest {
    @Test
    fun graveExceptionBeforeNotifyReleasesClaim() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event") shouldBe true
        var phase = NotificationPostPhase.NOT_INVOKED

        assertThrows<AssertionError> {
            try {
                postInformationalNotification(
                    canNotifyEvent = { true },
                    poster = { _ -> throw AssertionError("before return") },
                    onPostReturned = { NotificationBookkeepingResult.COMPLETED },
                    onPhaseChanged = { phase = it },
                )
            } finally {
                if (phase == NotificationPostPhase.RETURNED) {
                    registry.retainUncertain("event")
                } else {
                    registry.release("event")
                }
            }
        }

        registry.tryClaim("event") shouldBe true
    }

    @Test
    fun graveExceptionAfterNotifyReturnedRetainsUncertain() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event") shouldBe true
        var phase = NotificationPostPhase.NOT_INVOKED

        assertThrows<AssertionError> {
            try {
                postInformationalNotification(
                    canNotifyEvent = { true },
                    poster = { onNotifyInvoked -> onNotifyInvoked() },
                    onPostReturned = { throw AssertionError("bookkeeping interrupted") },
                    onPhaseChanged = { phase = it },
                )
            } finally {
                if (phase == NotificationPostPhase.RETURNED) {
                    registry.retainUncertain("event")
                } else {
                    registry.release("event")
                }
            }
        }

        registry.state("event") shouldBe AlertaEcEventClaimRegistry.State.UNCERTAIN
    }



    fun graveExceptionDuringPosterRetainsUncertainClaim() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event") shouldBe true
        var phase = NotificationPostPhase.NOT_INVOKED

        assertThrows<AssertionError> {
            try {
                postInformationalNotification(
                    canNotifyEvent = { true },
                    poster = { onNotifyInvoked ->
                        onNotifyInvoked()
                        throw AssertionError("during notify")
                    },
                    onPostReturned = { NotificationBookkeepingResult.COMPLETED },
                    onPhaseChanged = { phase = it },
                )
            } finally {
                if (phase == NotificationPostPhase.NOT_INVOKED) registry.release("event")
                else registry.retainUncertain("event")
            }
        }

        registry.state("event") shouldBe AlertaEcEventClaimRegistry.State.UNCERTAIN
    }


    fun graveExceptionAfterPosterReturnsRetainsUncertainClaim() {
        val registry = AlertaEcEventClaimRegistry()
        registry.tryClaim("event") shouldBe true
        var phase = NotificationPostPhase.NOT_INVOKED

        assertThrows<AssertionError> {
            try {
                postInformationalNotification(
                    canNotifyEvent = { true },
                    poster = { onNotifyInvoked -> onNotifyInvoked() },
                    onPostReturned = { NotificationBookkeepingResult.COMPLETED },
                    onPhaseChanged = {
                        phase = it
                        if (it == NotificationPostPhase.RETURNED) throw AssertionError("after return")
                    },
                )
            } finally {
                if (phase == NotificationPostPhase.NOT_INVOKED) registry.release("event")
                else registry.retainUncertain("event")
            }
        }

        registry.state("event") shouldBe AlertaEcEventClaimRegistry.State.UNCERTAIN
    }

    @Test
    fun revalidationRejectsIdMarkedAfterSelectionBeforeClaim() {
        AlertaEcEventDeduplicator.isStillUnnotified("event", setOf("event")) shouldBe false
        AlertaEcEventDeduplicator.isStillUnnotified("event", emptySet()) shouldBe true
    }

    @Test
    fun blockedDoesNotPublishOrMark() {
        var publications = 0
        var marks = 0

        val result = postInformationalNotification(
            canNotifyEvent = { false },
            poster = { _ -> publications++ },
            onPostReturned = { marks++; NotificationBookkeepingResult.COMPLETED },
        )

        result shouldBe NotificationPostResult.BLOCKED
        publications shouldBe 0
        marks shouldBe 0
    }

    @Test
    fun returnedPublishesOneRequestAndRunsEffectsAfterward() {
        var publications = 0
        val effects = mutableListOf<String>()

        val result = postInformationalNotification(
            canNotifyEvent = { true },
            poster = { _ ->
                publications++
                effects += "published"
            },
            onPostReturned = {
                effects += "markNotified"
                effects += "remove"
                effects += "markShown"
                NotificationBookkeepingResult.COMPLETED
            },
        )

        result shouldBe NotificationPostResult.POST_REQUEST_RETURNED
        publications shouldBe 1
        effects shouldBe listOf("published", "markNotified", "remove", "markShown")
    }

    @Test
    fun securityExceptionLeavesEventUnmarkedAndUnremoved() {
        var publications = 0
        var marks = 0
        var removals = 0

        val result = postInformationalNotification(
            canNotifyEvent = { true },
            poster = { _ ->
                publications++
                throw SecurityException("blocked by platform")
            },
            onPostReturned = {
                marks++
                removals++
                NotificationBookkeepingResult.COMPLETED
            },
        )

        result shouldBe NotificationPostResult.POST_FAILED
        publications shouldBe 1
        marks shouldBe 0
        removals shouldBe 0
    }

    @Test
    fun illegalArgumentExceptionLeavesEventUnmarkedAndUnremoved() {
        var publications = 0
        var marks = 0
        var removals = 0

        val result = postInformationalNotification(
            canNotifyEvent = { true },
            poster = { _ ->
                publications++
                throw IllegalArgumentException("invalid notification")
            },
            onPostReturned = {
                marks++
                removals++
                NotificationBookkeepingResult.COMPLETED
            },
        )

        result shouldBe NotificationPostResult.POST_FAILED
        publications shouldBe 1
        marks shouldBe 0
        removals shouldBe 0
    }

    @Test
    fun blockedDoesNotContinueEvenWhenPending() {
        AlertaEcNotificationQueuePolicy.continuation(
            result = NotificationPostResult.BLOCKED,
            hasPending = true,
            lastShownAtMillis = 0L,
            nowMillis = 1_000_000L,
        ) shouldBe null
    }

    @Test
    fun failedPostDoesNotCreateAnImmediateRetryChain() {
        AlertaEcNotificationQueuePolicy.continuation(
            result = NotificationPostResult.POST_FAILED,
            hasPending = true,
            lastShownAtMillis = 0L,
            nowMillis = 1_000_000L,
        ) shouldBe null
    }

    @Test
    fun bookkeepingFailureAfterReturnedPostDoesNotContinue() {
        var publications = 0
        val result = postInformationalNotification(
            canNotifyEvent = { true },
            poster = { _ -> publications++ },
            onPostReturned = { NotificationBookkeepingResult.FAILED },
        )

        result shouldBe NotificationPostResult.POST_REQUEST_RETURNED_WITH_BOOKKEEPING_FAILURE
        publications shouldBe 1
        AlertaEcNotificationQueuePolicy.continuation(
            result = result,
            hasPending = true,
            lastShownAtMillis = 1_000L,
            nowMillis = 1_000L,
        ) shouldBe null
    }

    @Test
    fun eachBookkeepingEffectFailureStopsLaterEffects() {
        val effectNames = listOf("markNotified", "remove", "markShown")
        effectNames.forEach { failingEffect ->
            val effects = mutableListOf<String>()
            val result = bookkeepInformationalNotification(
                markNotified = {
                    effects += "markNotified"
                    if (failingEffect == "markNotified") throw IllegalStateException()
                },
                remove = {
                    effects += "remove"
                    if (failingEffect == "remove") throw IllegalStateException()
                },
                markShown = {
                    effects += "markShown"
                    if (failingEffect == "markShown") throw IllegalStateException()
                },
            )

            result shouldBe NotificationBookkeepingResult.FAILED
            effects shouldBe effectNames.take(effectNames.indexOf(failingEffect) + 1)
        }
    }

    @Test
    fun returnedPostContinuesOnlyAfterTheInformationInterval() {
        AlertaEcNotificationQueuePolicy.continuation(
            result = NotificationPostResult.POST_REQUEST_RETURNED,
            hasPending = true,
            lastShownAtMillis = 1_000_000L,
            nowMillis = 1_000_000L,
        )?.delayMillis shouldBe AlertaEcNotificationQueuePolicy.INFORMATION_DURATION_MILLIS
        AlertaEcNotificationQueuePolicy.continuation(
            result = NotificationPostResult.POST_REQUEST_RETURNED,
            hasPending = true,
            lastShownAtMillis = 1_000_000L,
            nowMillis = 1_000_000L + AlertaEcNotificationQueuePolicy.INFORMATION_DURATION_MILLIS,
        )?.delayMillis shouldBe 1L
    }

    @Test
    fun dismissalNeverSchedulesWithZeroDelay() {
        AlertaEcNotificationQueuePolicy.continuationAfterDismissal(
            hasPending = true,
            lastShownAtMillis = 1_000L,
            nowMillis = 10_000_000L,
        )?.delayMillis shouldBe AlertaEcNotificationQueuePolicy.DISMISSAL_DELAY_MILLIS
    }

    @Test
    fun dismissalDoesNotReactivateAQueueWithoutShownEvidence() {
        AlertaEcNotificationQueuePolicy.continuationAfterDismissal(
            hasPending = true,
            lastShownAtMillis = 0L,
            nowMillis = 10_000L,
        ) shouldBe null
    }

    @Test
    fun enabledDismissalUsesPositiveThirtySecondMinimumDelay() {
        AlertaEcNotificationQueuePolicy.continuationAfterDismissal(
            hasPending = true,
            lastShownAtMillis = 1_000L,
            nowMillis = 1_000L + AlertaEcNotificationQueuePolicy.INFORMATION_DURATION_MILLIS,
        )?.delayMillis shouldBe AlertaEcNotificationQueuePolicy.DISMISSAL_DELAY_MILLIS
    }
}
