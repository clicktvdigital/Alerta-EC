package org.breezyweather.domain.settings

import android.content.Context
import android.content.SharedPreferences
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class CurrentLocationStoreTest {
    private fun store(values: MutableMap<String, Any> = mutableMapOf()): CurrentLocationStore {
        val context = mockk<Context>()
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { preferences.getString(any(), any()) } answers { values[firstArg()] as? String ?: secondArg() }
        every { preferences.getFloat(any(), any()) } answers { values[firstArg()] as? Float ?: secondArg() }
        every { preferences.getLong(any(), any()) } answers { values[firstArg()] as? Long ?: secondArg() }
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            val value = secondArg<String?>()
            if (value == null) values.remove(firstArg<String>()) else values[firstArg()] = value
            editor
        }
        every { editor.putFloat(any(), any()) } answers {
            values[firstArg()] = secondArg<Float>()
            editor
        }
        every { editor.putLong(any(), any()) } answers {
            values[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.clear() } answers {
            values.clear()
            editor
        }
        every { editor.apply() } returns Unit
        return CurrentLocationStore(context)
    }

    @Test
    fun preservesOriginalPointAndMeasurementInsteadOfRefreshTime() {
        val store = store()
        store.updateCurrentLocation(-78.1234567890123, -0.1234567890123, 8.5f, 2850.25, 1700000000123, "gps")
        store.lastKnownLongitude shouldBe -78.1234567890123
        store.lastKnownLatitude shouldBe -0.1234567890123
        store.accuracyMeters shouldBe 8.5f
        store.altitudeMeters shouldBe 2850.25
        store.measuredAtMillis shouldBe 1700000000123L
        store.provider shouldBe "gps"
        store.isUsable shouldBe true
    }

    @Test
    fun acceptsLegacyFloatsWithoutInventingMeasurementMetadata() {
        val store = store(mutableMapOf(
            "last_known_latitude" to -0.1f,
            "last_known_longitude" to -78f,
            "last_successful_refresh" to 1700000000000L
        ))
        store.lastKnownLatitude shouldBe (-0.1f).toDouble()
        store.isUsable shouldBe true
        store.measuredAtMillis shouldBe null
        store.accuracyMeters shouldBe null
    }

    @Test
    fun missingMetadataRetainsOldReadingAndClearRemovesPosition() {
        val store = store()
        store.updateCurrentLocation(1.0, 2.0, 5f, 100.0, 1700000000000, "gps")
        val refreshBeforeRejectedUpdate = store.lastSuccessfulRefresh
        store.updateCurrentLocation(3.0, 4.0)

        store.lastKnownLatitude shouldBe 1.0
        store.lastKnownLongitude shouldBe 2.0
        store.accuracyMeters shouldBe 5f
        store.altitudeMeters shouldBe 100.0
        store.measuredAtMillis shouldBe 1700000000000L
        store.provider shouldBe "gps"
        store.lastSuccessfulRefresh shouldBe refreshBeforeRejectedUpdate
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) { store.updateCurrentLocation(Double.NaN, 0.0) }

        store.clearCurrentLocation()
        store.isUsable shouldBe false
        store.lastKnownLongitude shouldBe 0.0
        store.accuracyMeters shouldBe null
        store.altitudeMeters shouldBe null
        store.measuredAtMillis shouldBe null
        store.provider shouldBe null
    }

    @Test
    fun newerTimestampReplacesOlderReading() {
        val store = store()
        store.updateCurrentLocation(1.0, 2.0, 5f, 100.0, 1700000000000, "gps")
        store.updateCurrentLocation(3.0, 4.0, 8f, 120.0, 1700000001000, "network")

        store.lastKnownLatitude shouldBe 3.0
        store.lastKnownLongitude shouldBe 4.0
        store.accuracyMeters shouldBe 8f
        store.altitudeMeters shouldBe 120.0
        store.measuredAtMillis shouldBe 1700000001000L
        store.provider shouldBe "network"
    }
}
