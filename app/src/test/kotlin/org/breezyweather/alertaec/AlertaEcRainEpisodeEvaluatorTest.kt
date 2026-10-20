package org.breezyweather.alertaec

import breezyweather.domain.weather.model.Minutely
import org.breezyweather.unit.precipitation.millimeters
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date

class AlertaEcRainEpisodeEvaluatorTest {
    private val now = 1_800_000_000_000L

    private fun sample(offsetMinutes: Long, mm: Double): Minutely = Minutely(
        Date(now + offsetMinutes * 60_000L),
        15,
        mm.millimeters
    )

    @Test fun missingZeroAndNegativeDataAreNotAlerts() {
        AlertaEcRainEpisodeEvaluator.evaluate(emptyList(), now).state shouldBe AlertaEcRainEpisodeState.NO_DATA
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(0, 0.0), sample(15, 0.0)), now).state shouldBe AlertaEcRainEpisodeState.NO_RAIN
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(0, -1.0), sample(15, 0.0)), now).state shouldBe AlertaEcRainEpisodeState.NO_DATA
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(Minutely(Date(now), 15, null), sample(15, 1.0)), now).state shouldBe AlertaEcRainEpisodeState.IMMINENT
    }

    @Test fun fifteenMinuteForecastIsImminentWithoutInventingFiveMinutes() {
        val decision = AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(0, 0.0), sample(15, 1.0)), now)
        decision.state shouldBe AlertaEcRainEpisodeState.IMMINENT
        decision.windowMinutes shouldBe 15
    }

    @Test fun activePauseAndResumptionAreInferredFromConsecutiveSamples() {
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-15, 1.0), sample(0, 1.0), sample(15, 0.0)), now).state shouldBe AlertaEcRainEpisodeState.PAUSE
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-30, 1.0), sample(-15, 0.0), sample(0, 1.0)), now).state shouldBe AlertaEcRainEpisodeState.RESUMED
    }

    @Test fun recentlyEndedForecastIsMarkedFinished() {
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-30, 1.0), sample(-15, 0.0)), now).state shouldBe AlertaEcRainEpisodeState.FINISHED
    }

    @Test fun pauseToResumeAcrossAnUpdateGetsANewTransition() {
        val paused = AlertaEcRainEpisodeEvaluator.evaluate(
            listOf(sample(-15, 1.0), sample(0, 0.0), sample(15, 1.0)), now
        )
        val resumed = AlertaEcRainEpisodeEvaluator.evaluate(
            listOf(sample(-15, 0.0), sample(0, 1.0), sample(15, 1.0)), now
        )
        AlertaEcRainEpisodeEvaluator.transitionKeyAfter(paused.state, resumed)!!.startsWith("RESUMED:") shouldBe true
    }

    @Test fun staleAndOutOfOrderDataAreRejected() {
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-45, 1.0), sample(-30, 1.0)), now).state shouldBe AlertaEcRainEpisodeState.NO_DATA
        AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(15, 1.0), sample(0, 1.0)), now).state shouldBe AlertaEcRainEpisodeState.NO_DATA
    }
    @Test fun continuousRainKeepsStableEpisodeIdentityAcrossTimestampShifts() {
        val first = AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(0, 1.0), sample(15, 1.0)), now)
        val second = AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(15, 1.0), sample(30, 1.0)), now + 15 * 60_000L)
        val episode = AlertaEcRainEpisodeEvaluator.stableEpisodeKey(null, null, first)
        val continued = AlertaEcRainEpisodeEvaluator.stableEpisodeKey(first.state, episode, second)
        continued shouldBe episode
        AlertaEcRainEpisodeEvaluator.stableNotificationTransitionKey(first.state, continued, episode, second) shouldBe episode
    }

    @Test fun pausedEpisodeCreatesOneResumeTransitionAndNewEpisodeAfterFinish() {
        val paused = AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-15, 1.0), sample(0, 0.0), sample(15, 1.0)), now)
        val resumed = AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-15, 0.0), sample(0, 1.0), sample(15, 1.0)), now)
        val episode = "EPISODE:rain"
        val resumeKey = AlertaEcRainEpisodeEvaluator.stableNotificationTransitionKey(
            paused.state, episode, episode, resumed
        )
        resumeKey shouldBe "RESUMED:$episode"
        val finished = AlertaEcRainEpisodeEvaluator.evaluate(listOf(sample(-30, 1.0), sample(-15, 0.0)), now)
        AlertaEcRainEpisodeEvaluator.stableEpisodeKey(finished.state, episode, finished) shouldBe "EPISODE:${finished.transitionKey}"
    }

}
