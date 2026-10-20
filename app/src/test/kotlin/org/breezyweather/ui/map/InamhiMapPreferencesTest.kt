package org.breezyweather.ui.map

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InamhiMapPreferencesTest {
    private val context = mockk<Context>()
    private val prefs = mockk<SharedPreferences>()
    private val legacy = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    init {
        every { context.getSharedPreferences("alerta_ec_inamhi_map", Context.MODE_PRIVATE) } returns prefs
        every { context.getSharedPreferences("alerta_ec_map_layers", Context.MODE_PRIVATE) } returns legacy
        every { legacy.contains(any()) } returns false
        every { prefs.getBoolean(any(), true) } returns true
    }

    @Test
    fun missingPreferencesEnableAllLayersWithoutWriting() {
        val selection = InamhiMapPreferences.read(context)
        InamhiLayers.all.forEach { assertTrue(selection.enabled(it)) }
        verify(exactly = 0) { prefs.edit() }
    }

    @Test
    fun savedDisabledPreferencesAreRespected() {
        every { prefs.getBoolean(any(), true) } returns false
        val selection = InamhiMapPreferences.read(context)
        InamhiLayers.all.forEach { assertFalse(selection.enabled(it)) }
        verify(exactly = 0) { prefs.edit() }
    }

    @Test
    fun legacyValueCannotOverwriteCurrentChoice() {
        every { legacy.contains("inamhi_rain_3h_enabled") } returns true
        every { prefs.contains("rain_visible") } returns true
        every { prefs.getBoolean("rain_visible", true) } returns false
        every { legacy.edit() } returns editor
        every { editor.remove(any()) } returns editor
        assertFalse(InamhiMapPreferences.read(context).rain3h)
        verify(exactly = 0) { prefs.edit() }
        verify { editor.remove("inamhi_rain_3h_enabled") }
    }
}
