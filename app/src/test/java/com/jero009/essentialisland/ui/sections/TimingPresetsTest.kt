/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui

import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.ui.sections.TimingPreset
import com.jero009.essentialisland.ui.sections.TimingValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingPresetsTest {
    @Test
    fun `normal is the shipped default`() {
        val defaults = TimingPreset.from(SmartIslandSettings.Default)
        assertEquals(TimingPreset.Normal, TimingPreset.match(defaults))
    }

    @Test
    fun `every preset value sits inside its slider range`() {
        TimingPreset.entries.forEach { preset ->
            with(preset.values) {
                assertTrue(idleTimeoutMs in SmartIslandSettings.MIN_IDLE_TIMEOUT_MS..SmartIslandSettings.MAX_IDLE_TIMEOUT_MS)
                assertTrue(batteryWarningMs in SmartIslandSettings.MIN_BATTERY_WARNING_MS..SmartIslandSettings.MAX_BATTERY_WARNING_MS)
                assertTrue(popupMs in SmartIslandSettings.MIN_POPUP_MS..SmartIslandSettings.MAX_POPUP_MS)
                assertTrue(secondaryMergeMs in SmartIslandSettings.MIN_SECONDARY_MERGE_MS..SmartIslandSettings.MAX_SECONDARY_MERGE_MS)
                assertTrue(transferExpiryMinutes in 0..SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES)
            }
        }
    }

    @Test
    fun `hand-tuned timings match no preset`() {
        val custom = TimingPreset.Normal.values.copy(idleTimeoutMs = 4_999)
        assertNull(TimingPreset.match(custom))
    }

    @Test
    fun `presets are distinct`() {
        assertTrue(TimingPreset.entries.map { it.values }.toSet().size == TimingPreset.entries.size)
    }
}
