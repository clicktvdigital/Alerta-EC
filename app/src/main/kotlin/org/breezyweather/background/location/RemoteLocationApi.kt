package org.breezyweather.background.location

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class RemoteLocationUpdate(
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val measuredAt: Long,
    val expiresAt: Long,
)

@Serializable
data class RemoteLocationSessionResponse(
    val sessionId: String,
    val shareToken: String,
    val expiresAt: Long,
)

@Serializable
data class RemoteLocationStatusResponse(
    val accepted: Boolean,
)

interface RemoteLocationApi {

    @POST("v1/location/sessions")
    suspend fun createSession(
        @Body update: RemoteLocationUpdate,
    ): RemoteLocationSessionResponse

    @POST("v1/location/sessions/{sessionId}/location")
    suspend fun updateLocation(
        @Path("sessionId") sessionId: String,
        @Body update: RemoteLocationUpdate,
    ): RemoteLocationStatusResponse

    @DELETE("v1/location/sessions/{sessionId}")
    suspend fun revokeSession(
        @Path("sessionId") sessionId: String,
    ): RemoteLocationStatusResponse
}
