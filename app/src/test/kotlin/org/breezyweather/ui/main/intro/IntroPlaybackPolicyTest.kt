package org.breezyweather.ui.main.intro

import android.content.SharedPreferences
import android.media.AudioManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class IntroPlaybackPolicyTest {
    private fun preferencesWithSound(savedValue: Boolean?): SharedPreferences = mockk<SharedPreferences>().also {
        every { it.getBoolean("sound_enabled", any()) } answers { savedValue ?: secondArg<Boolean>() }
    }

    @Test
    fun freshInstallationDoesNotAuthorizeAudioOrPersistADefault() {
        val preferences = preferencesWithSound(null)
        val enabled = AlertaEcIntroController.isSoundEnabled(preferences)
        enabled shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(enabled, 1, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe false
        verify(exactly = 0) { preferences.edit() }
    }

    @Test
    fun previouslyEnabledSoundRemainsEnabledWithoutRewritingPreferences() {
        val preferences = preferencesWithSound(true)
        val enabled = AlertaEcIntroController.isSoundEnabled(preferences)
        enabled shouldBe true
        IntroPlaybackPolicy.shouldPlayAudio(enabled, 1, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe true
        verify(exactly = 0) { preferences.edit() }
    }

    @Test
    fun previouslyDisabledSoundRemainsDisabledWithoutRewritingPreferences() {
        val preferences = preferencesWithSound(false)
        AlertaEcIntroController.isSoundEnabled(preferences) shouldBe false
        verify(exactly = 0) { preferences.edit() }
    }

    @Test
    fun audioRequiresConsentAndAnAvailableTrack() {
        IntroPlaybackPolicy.shouldPlayAudio(true, 0, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(false, 1, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_SILENT, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_VIBRATE, 5) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_NORMAL, 0) shouldBe false
        IntroPlaybackPolicy.shouldPlayAudio(true, 1, AudioManager.RINGER_MODE_NORMAL, 5) shouldBe true
    }

    @Test
    fun reducedMotionFollowsUserOrSystemPreference() {
        IntroPlaybackPolicy.shouldReduceMotion(false, 1f) shouldBe false
        IntroPlaybackPolicy.shouldReduceMotion(true, 1f) shouldBe true
        IntroPlaybackPolicy.shouldReduceMotion(false, 0f) shouldBe true
        IntroPlaybackPolicy.shouldReduceMotion(false, 1f, systemAnimationsEnabled = false) shouldBe true
        IntroPlaybackPolicy.shouldReduceMotion(false, Float.NaN) shouldBe true
    }

    @Test
    fun `video profile follows device type and RAM`() {
        val gib = 1024L * 1024 * 1024
        // Android TV of the test bench: ~1.64 GiB -> Full HD.
        IntroPlaybackPolicy.videoProfile(true, (1.64 * gib).toLong(), false) shouldBe IntroVideoProfile.TV_FULL_HD
        // 1 GB TV box -> 720p.
        IntroPlaybackPolicy.videoProfile(true, 1 * gib, false) shouldBe IntroVideoProfile.TV_BOX_HD
        // Declared low-RAM device -> 720p even with more memory reported.
        IntroPlaybackPolicy.videoProfile(true, 4 * gib, true) shouldBe IntroVideoProfile.TV_BOX_HD
        // Unknown memory never gets the heavier stream.
        IntroPlaybackPolicy.videoProfile(true, 0, false) shouldBe IntroVideoProfile.TV_BOX_HD
        // Phones always use the portrait video.
        IntroPlaybackPolicy.videoProfile(false, 8 * gib, false) shouldBe IntroVideoProfile.PHONE_VERTICAL
    }
}
