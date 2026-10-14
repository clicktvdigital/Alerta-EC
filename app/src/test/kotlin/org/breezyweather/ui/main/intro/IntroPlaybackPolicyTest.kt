package org.breezyweather.ui.main.intro

import android.media.AudioManager
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IntroPlaybackPolicyTest {
    @Test
    fun audioRequiresConsentAndAnAvailableTrack() {
        IntroPlaybackPolicy.shouldPlayAudio(true, 0, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(false, 1, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_SILENT, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_NORMAL, 0) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe true
    }

    @Test
    fun reducedMotionFollowsUserOrSystemPreference() {
        IntroPlaybackPolicy.shouldReduceMotion(false, 1f) shouldBe false
        IntroPlaybackPolicy.shouldReduceMotion(true, 1f) shouldBe true
        IntroPlaybackPolicy.shouldReduceMotion(false, 0f) shouldBe true
    }
}
