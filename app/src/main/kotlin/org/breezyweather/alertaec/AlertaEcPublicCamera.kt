package org.breezyweather.alertaec

enum class AlertaEcCameraType {
    LIVE_VIDEO,
    REFRESHING_IMAGE,
    OFFICIAL_EXTERNAL_LINK
}

data class AlertaEcPublicCamera(
    val id: String,
    val name: String,
    val provider: String,
    val province: String,
    val locationName: String,
    val latitude: Double?,
    val longitude: Double?,
    val sourcePageUrl: String,
    val mediaUrl: String?,
    val type: AlertaEcCameraType,
    val lastImageAtMillis: Long?,
    val embeddingAuthorized: Boolean,
    val enabled: Boolean = true
) {
    val canPlayInsideApp: Boolean
        get() = enabled && embeddingAuthorized &&
            !mediaUrl.isNullOrBlank() &&
            type != AlertaEcCameraType.OFFICIAL_EXTERNAL_LINK
}
