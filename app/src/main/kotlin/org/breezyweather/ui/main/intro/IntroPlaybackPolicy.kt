package org.breezyweather.ui.main.intro

internal object IntroPlaybackPolicy {
    fun shouldReduceMotion(
        userEnabled: Boolean,
        animatorDurationScale: Float,
        systemAnimationsEnabled: Boolean = true,
    ): Boolean = userEnabled || !animatorDurationScale.isFinite() || animatorDurationScale <= 0f || !systemAnimationsEnabled

    fun shouldPlayAudio(userEnabled: Boolean, rawResourceId: Int, ringerMode: Int, musicVolume: Int): Boolean =
        userEnabled && rawResourceId != 0 && ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL && musicVolume > 0

    /** Minimum RAM to play the Full HD TV intro; below it (1 GB TV boxes) the 720p version is used. */
    const val FULL_HD_MIN_RAM_BYTES = 1_536L * 1024 * 1024

    /**
     * Intro video per device profile: Android TV in Full HD (1920x1080), low-RAM TV boxes in HD (720p),
     * phones in vertical. Unknown RAM (0) counts as low, so a box never gets the heavier stream.
     */
    fun videoProfile(tvVisuals: Boolean, totalMemBytes: Long, isLowRamDevice: Boolean): IntroVideoProfile = when {
        !tvVisuals -> IntroVideoProfile.PHONE_VERTICAL
        isLowRamDevice || totalMemBytes < FULL_HD_MIN_RAM_BYTES -> IntroVideoProfile.TV_BOX_HD
        else -> IntroVideoProfile.TV_FULL_HD
    }
}

internal enum class IntroVideoProfile {
    /** Android TV with enough RAM: 1920x1080. */
    TV_FULL_HD,

    /** TV box with about 1 GB of RAM: 1280x720. */
    TV_BOX_HD,

    /** Phone, portrait video. */
    PHONE_VERTICAL,
}
