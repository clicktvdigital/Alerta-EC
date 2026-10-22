package org.breezyweather.alertaec.accessibility

import io.kotest.matchers.shouldBe
import org.breezyweather.alertaec.AlertaEcEvent
import org.breezyweather.alertaec.AlertaEcEventState
import org.breezyweather.alertaec.AlertaEcPhenomenon
import org.breezyweather.alertaec.AlertaEcSeverity
import java.time.Instant
import org.junit.jupiter.api.Test

class AlertVoicePolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun `new accepted active warning is allowed`() {
        AlertVoicePolicy.mayReadAutomatically(event(), now, state()) shouldBe true
    }

    @Test
    fun `informational and advisory alerts are not read automatically`() {
        AlertVoicePolicy.mayReadAutomatically(event(AlertaEcSeverity.INFORMATION), now, state()) shouldBe false
        AlertVoicePolicy.mayReadAutomatically(event(AlertaEcSeverity.ADVISORY), now, state()) shouldBe false
    }

    @Test
    fun `repeated queued or unaccepted alerts are rejected`() {
        AlertVoicePolicy.mayReadAutomatically(event(), now, state(isNewEvent = false)) shouldBe false
        AlertVoicePolicy.mayReadAutomatically(event(), now, state(hasBacklog = true)) shouldBe false
        AlertVoicePolicy.mayReadAutomatically(event(), now, state(notificationAccepted = false)) shouldBe false
    }

    @Test
    fun `expired missing and not-yet-started events are rejected`() {
        AlertVoicePolicy.mayReadAutomatically(event(endTimeMillis = now), now, state()) shouldBe false
        AlertVoicePolicy.mayReadAutomatically(event(endTimeMillis = null), now, state()) shouldBe false
        AlertVoicePolicy.mayReadAutomatically(event(startTimeMillis = now + 1_000), now, state()) shouldBe false
    }

    @Test
    fun `foreground dnd and volume gates are required`() {
        listOf(state(activityResumed = false), state(activityHasFocus = false), state(dndAllowsAlerts = false), state(notificationVolume = 0)).forEach {
            AlertVoicePolicy.mayReadAutomatically(event(), now, it) shouldBe false
        }
    }



    @Test
    fun `accepted event gate requires watch and a currently active interval`() {
        val acceptedNow = Instant.ofEpochMilli(now)
        AlertVoicePolicy.isEligibleForAutomaticVoice(acceptedEvent(), acceptedNow) shouldBe true
        AlertVoicePolicy.isEligibleForAutomaticVoice(acceptedEvent(AlertaEcSeverity.ADVISORY), acceptedNow) shouldBe false
        AlertVoicePolicy.isEligibleForAutomaticVoice(acceptedEvent(end = acceptedNow), acceptedNow) shouldBe false
        AlertVoicePolicy.isEligibleForAutomaticVoice(acceptedEvent(start = Instant.EPOCH), acceptedNow) shouldBe false
    }

    private fun acceptedEvent(
        severity: AlertaEcSeverity = AlertaEcSeverity.WARNING,
        start: Instant = Instant.ofEpochMilli(now - 1_000),
        end: Instant = Instant.ofEpochMilli(now + 60_000),
    ) = AcceptedAlertEvent(
        producerEventId = "v1-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        origin = AcceptedAlertEvent.Origin.FORECAST,
        phenomenon = AlertaEcPhenomenon.THUNDERSTORM,
        severity = severity,
        start = start,
        end = end,
        acceptanceEvidence = AcceptedAlertEvent.AcceptanceEvidence.NOTIFICATION_ACCEPTED_WITHOUT_BACKLOG,
    )

    private fun event(severity: AlertaEcSeverity = AlertaEcSeverity.WARNING, startTimeMillis: Long = now - 1_000, endTimeMillis: Long? = now + 60_000) = AlertaEcEvent(
        id = "voice-event", phenomenon = AlertaEcPhenomenon.THUNDERSTORM, state = AlertaEcEventState.FORECAST,
        severity = severity, headline = "Tormenta", startTimeMillis = startTimeMillis, endTimeMillis = endTimeMillis
    )

    private fun state(isNewEvent: Boolean = true, notificationAccepted: Boolean = true, hasBacklog: Boolean = false,
        activityResumed: Boolean = true, activityHasFocus: Boolean = true, pushEnabled: Boolean = true,
        soundEnabled: Boolean = true, voiceEnabled: Boolean = true, dndAllowsAlerts: Boolean = true,
        notificationVolume: Int = 5, deviceUnlocked: Boolean = true, audioModeNormal: Boolean = true,
        musicInactive: Boolean = true, audioFocusAvailable: Boolean = true, readingInProgress: Boolean = false) =
        AlertVoicePolicy.AutomaticReadState(isNewEvent, notificationAccepted, hasBacklog, activityResumed, activityHasFocus,
            pushEnabled, soundEnabled, voiceEnabled, dndAllowsAlerts, notificationVolume, deviceUnlocked,
            audioModeNormal, musicInactive, audioFocusAvailable, readingInProgress)
}
