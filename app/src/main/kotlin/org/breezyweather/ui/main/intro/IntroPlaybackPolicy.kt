package org.breezyweather.ui.main.intro

internal object IntroPlaybackPolicy {
    fun shouldReduceMotion(userEnabled: Boolean, animatorDurationScale: Float): Boolean =
        userEnabled || animatorDurationScale == 0f

    fun shouldPlayAudio(userEnabled: Boolean, rawResourceId: Int, ringerMode: Int, musicVolume: Int): Boolean =
        userEnabled && rawResourceId != 0 && ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL && musicVolume > 0
}
