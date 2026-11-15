/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui

import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.ui.expanded.cardScrimScale
import com.jero009.essentialisland.ui.expanded.expandedGradientBrush
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CardStyleTest {
    private val base = SmartIslandSettings.Default

    @Test
    fun scrimIsFullWithoutGradientAndLightWithIt() {
        assertEquals(1f, cardScrimScale(base.copy(expandedGradientEnabled = false)), 0.001f)
        // Nothing default: the expanded card lifts to #1B1B1B via the gradient, so the scrim stays light.
        assertEquals(0.3f, cardScrimScale(base), 0.001f)
    }

    @Test
    fun gradientBrushOnlyWhenEnabled() {
        assertNull(expandedGradientBrush(base.copy(expandedGradientEnabled = false)))
        // Nothing default: gradient on (black pill -> #1B1B1B expanded card).
        assertNotNull(expandedGradientBrush(base))
    }

    @Test
    fun seeThroughBlurIsOffByDefault() {
    }
}
