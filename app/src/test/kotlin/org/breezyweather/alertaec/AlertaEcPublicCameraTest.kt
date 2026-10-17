package org.breezyweather.alertaec

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertaEcPublicCameraTest {

    private fun camera(
        authorized: Boolean,
        enabled: Boolean = true,
        type: AlertaEcCameraType = AlertaEcCameraType.LIVE_VIDEO,
        mediaUrl: String? = "https://example.org/live.m3u8"
    ) = AlertaEcPublicCamera(
        id = "camera-test",
        name = "Camara de prueba",
        provider = "Proveedor de prueba",
        province = "Pichincha",
        locationName = "Quito",
        latitude = null,
        longitude = null,
        sourcePageUrl = "https://example.org",
        mediaUrl = mediaUrl,
        type = type,
        lastImageAtMillis = null,
        embeddingAuthorized = authorized,
        enabled = enabled
    )

    @Test
    fun authorizedStreamCanPlay() {
        assertTrue(camera(authorized = true).canPlayInsideApp)
    }

    @Test
    fun unauthorizedStreamCannotPlay() {
        assertFalse(camera(authorized = false).canPlayInsideApp)
    }

    @Test
    fun disabledCameraCannotPlay() {
        assertFalse(camera(authorized = true, enabled = false).canPlayInsideApp)
    }

    @Test
    fun externalLinksCannotPlayInsideApp() {
        assertFalse(
            camera(
                authorized = true,
                type = AlertaEcCameraType.OFFICIAL_EXTERNAL_LINK
            ).canPlayInsideApp
        )
    }

    @Test
    fun cameraWithoutMediaCannotPlay() {
        assertFalse(camera(authorized = true, mediaUrl = null).canPlayInsideApp)
    }
}
