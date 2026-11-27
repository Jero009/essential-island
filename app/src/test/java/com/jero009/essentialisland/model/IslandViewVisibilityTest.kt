/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.model

import com.jero009.essentialisland.data.SmartIslandSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandViewVisibilityTest {
    private fun hidden(
        settings: SmartIslandSettings = SmartIslandSettings(),
        isLocked: Boolean = false,
        isLandscape: Boolean = false,
        visibleCardCount: Int = 0,
        exemptMode: ExemptMode = ExemptMode.None
    ) = isIslandViewHidden(settings, isLocked, isLandscape, visibleCardCount, exemptMode)

    @Test fun portraitAlwaysVisible() {
        assertFalse(hidden())
        assertFalse(hidden(visibleCardCount = 3))
        assertFalse(hidden(exemptMode = ExemptMode.Static))
    }

    @Test fun landscapeShownWhenShowInLandscape() {
        assertFalse(hidden(isLandscape = true))
        assertFalse(hidden(isLandscape = true, visibleCardCount = 2))
    }

    @Test fun landscapeIdlePillStaysHidden() {
        assertTrue(hidden(isLandscape = true, settings = SmartIslandSettings(showInLandscape = false)))
    }

    @Test fun cardsSurfaceIslandInLandscape() {
        val landscapeOff = SmartIslandSettings(showInLandscape = false)
        assertFalse(hidden(isLandscape = true, settings = landscapeOff, visibleCardCount = 1))
        assertFalse(hidden(isLandscape = true, settings = landscapeOff, visibleCardCount = 4))
        // Cards gone: the island hides again.
        assertTrue(hidden(isLandscape = true, settings = landscapeOff, visibleCardCount = 0))
    }

    @Test fun landscapeCardsToggleOffKeepsOldBehavior() {
        val settings = SmartIslandSettings(showInLandscape = false, showNotificationCardsInLandscape = false)
        assertTrue(hidden(isLandscape = true, settings = settings, visibleCardCount = 2))
    }

    @Test fun cardsDoNotOverrideLandscapePillToggleInPortrait() {
        // The cards toggle only relaxes the pure landscape case; portrait is unaffected either way.
        val settings = SmartIslandSettings(showNotificationCardsInLandscape = false)
        assertFalse(hidden(settings = settings, visibleCardCount = 1))
    }

    @Test fun lockScreenHideBeatsCards() {
        val settings = SmartIslandSettings(showOnLockScreen = false)
        assertTrue(hidden(settings = settings, isLocked = true, visibleCardCount = 2))
        assertTrue(hidden(settings = settings, isLocked = true, isLandscape = true, visibleCardCount = 2))
        assertFalse(hidden(settings = settings, isLocked = false))
    }

    @Test fun exemptOffHidesEvenWithCards() {
        assertTrue(hidden(isLandscape = true, visibleCardCount = 2, exemptMode = ExemptMode.Off))
        assertTrue(hidden(visibleCardCount = 2, exemptMode = ExemptMode.Off))
    }

    @Test fun exemptStaticStaysVisibleForItsOwnHandling() {
        // Static is handled downstream (dead pill); it must not be pre-hidden here.
        assertFalse(hidden(exemptMode = ExemptMode.Static, visibleCardCount = 0))
        assertFalse(hidden(isLandscape = true, exemptMode = ExemptMode.Static, visibleCardCount = 1))
    }

    @Test fun defaultsHaveCardsInLandscapeOn() {
        assertTrue(SmartIslandSettings().showNotificationCardsInLandscape)
        assertFalse(hidden(isLandscape = true, settings = SmartIslandSettings(showInLandscape = false), visibleCardCount = 1))
    }
}
