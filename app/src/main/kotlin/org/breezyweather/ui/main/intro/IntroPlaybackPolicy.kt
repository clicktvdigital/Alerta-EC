package org.breezyweather.ui.main.intro

/** Perfil de reproducción de la intro según el equipo. */
internal enum class IntroDeviceProfile { ANDROID_TV, TV_BOX, PHONE }

internal object IntroPlaybackPolicy {
    /** Por debajo de esto un televisor se trata como TV Box: video HD para no saturarlo. */
    const val FHD_MIN_TOTAL_MEMORY_BYTES = 2_500_000_000L

    /**
     * - Celular: retrato sin modo televisión.
     * - TV Box: horizontal sin modo televisión (muchos box se anuncian como tableta) o televisor con poca
     *   memoria, pantalla menor a Full HD o sin decodificador H.264 1080p.
     * - Android TV: televisor (Leanback) con recursos para Full HD.
     */
    fun deviceProfile(
        isTelevision: Boolean,
        isLandscape: Boolean,
        lowRamDevice: Boolean,
        totalMemoryBytes: Long,
        displayLongSidePx: Int,
        decoderSupportsFhd: Boolean,
    ): IntroDeviceProfile = when {
        !isTelevision && !isLandscape -> IntroDeviceProfile.PHONE
        !isTelevision -> IntroDeviceProfile.TV_BOX
        lowRamDevice ||
            (totalMemoryBytes > 0 && totalMemoryBytes < FHD_MIN_TOTAL_MEMORY_BYTES) ||
            displayLongSidePx in 1 until 1920 ||
            !decoderSupportsFhd -> IntroDeviceProfile.TV_BOX
        else -> IntroDeviceProfile.ANDROID_TV
    }

    /** Logo inicial y final Full HD; luego los recursos anteriores como respaldo. */
    fun logoCandidates(profile: IntroDeviceProfile): List<String> = when (profile) {
        IntroDeviceProfile.PHONE -> listOf("alertaec_intro_logo_fhd_v", "alerta_ec_intro_logo")
        else -> listOf("alertaec_intro_logo_fhd_h", "tv_banner")
    }

    /**
     * Video por perfil, en orden de preferencia. El último siempre existe (vertical 720×1280),
     * así que falta un archivo nunca deja la intro sin video. TV Box nunca recibe 1080p.
     */
    fun videoCandidates(profile: IntroDeviceProfile): List<String> = when (profile) {
        IntroDeviceProfile.ANDROID_TV -> listOf("alertaec_intro_fhd_h", "alertaec_intro_hd_h", "alertaec_cinematic_intro")
        IntroDeviceProfile.TV_BOX -> listOf("alertaec_intro_hd_h", "alertaec_cinematic_intro")
        IntroDeviceProfile.PHONE -> listOf("alertaec_cinematic_intro")
    }

    /** Primer recurso existente; [resolve] devuelve 0 si no existe. */
    fun firstAvailable(names: List<String>, resolve: (String) -> Int): Pair<String, Int>? =
        names.firstNotNullOfOrNull { name -> resolve(name).takeIf { it != 0 }?.let { name to it } }

    fun shouldReduceMotion(
        userEnabled: Boolean,
        animatorDurationScale: Float,
        systemAnimationsEnabled: Boolean = true,
    ): Boolean = userEnabled || !animatorDurationScale.isFinite() || animatorDurationScale <= 0f || !systemAnimationsEnabled

    fun shouldPlayAudio(userEnabled: Boolean, rawResourceId: Int, ringerMode: Int, musicVolume: Int): Boolean =
        userEnabled && rawResourceId != 0 && ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL && musicVolume > 0
}
