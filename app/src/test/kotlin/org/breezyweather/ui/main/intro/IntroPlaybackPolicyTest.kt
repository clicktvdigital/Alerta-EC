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

    private fun profile(
        tv: Boolean = true,
        landscape: Boolean = true,
        lowRam: Boolean = false,
        memory: Long = 4_000_000_000L,
        longSide: Int = 1920,
        fhd: Boolean = true,
    ) = IntroPlaybackPolicy.deviceProfile(tv, landscape, lowRam, memory, longSide, fhd)

    @Test
    fun deviceProfilesFollowPowerAndOrientation() {
        profile() shouldBe IntroDeviceProfile.ANDROID_TV
        profile(memory = 1_765_000_000L) shouldBe IntroDeviceProfile.ANDROID_TV
        profile(memory = 1_200_000_000L) shouldBe IntroDeviceProfile.TV_BOX
        profile(lowRam = true) shouldBe IntroDeviceProfile.TV_BOX
        profile(longSide = 1280) shouldBe IntroDeviceProfile.TV_BOX
        profile(fhd = false) shouldBe IntroDeviceProfile.TV_BOX
        profile(tv = false, landscape = true) shouldBe IntroDeviceProfile.TV_BOX
        profile(tv = false, landscape = false) shouldBe IntroDeviceProfile.PHONE
    }

    @Test
    fun resourcesFallBackCleanlyAndTvBoxNeverGetsFullHdVideo() {
        IntroPlaybackPolicy.videoCandidates(IntroDeviceProfile.ANDROID_TV).first() shouldBe "alertaec_intro_fhd_h"
        IntroPlaybackPolicy.videoCandidates(IntroDeviceProfile.TV_BOX).first() shouldBe "alertaec_intro_hd_h"
        ("alertaec_intro_fhd_h" in IntroPlaybackPolicy.videoCandidates(IntroDeviceProfile.TV_BOX)) shouldBe false
        IntroPlaybackPolicy.videoCandidates(IntroDeviceProfile.PHONE) shouldBe listOf("alertaec_cinematic_intro")
        IntroDeviceProfile.entries.forEach {
            IntroPlaybackPolicy.videoCandidates(it).last() shouldBe "alertaec_cinematic_intro"
        }
        IntroPlaybackPolicy.logoCandidates(IntroDeviceProfile.PHONE).first() shouldBe "alertaec_intro_logo_fhd_v"
        IntroPlaybackPolicy.logoCandidates(IntroDeviceProfile.TV_BOX).first() shouldBe "alertaec_intro_logo_fhd_h"

        val available = mapOf("alertaec_cinematic_intro" to 7)
        IntroPlaybackPolicy.firstAvailable(IntroPlaybackPolicy.videoCandidates(IntroDeviceProfile.ANDROID_TV)) {
            available[it] ?: 0
        } shouldBe ("alertaec_cinematic_intro" to 7)
        IntroPlaybackPolicy.firstAvailable(listOf("missing")) { 0 } shouldBe null
    }
}
