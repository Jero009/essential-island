/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The empty/idle pill must stay completely rigid under swipes (no lean, no stretch, no content slide, no
 * arm swell) — Jernej, after v1.12.x: "the island is bouncy when empty … make it fixed". A pill with
 * content, or the idle-widget pill, keeps its swipe feedback.
 */
class SwipeFeelTest {

    @Test
    fun anEmptyPillWithNoWidgetIsFrozen() {
        assertTrue(SwipeFeel.collapsedPillFrozen(hasCards = false, widgetOnPill = false))
    }

    @Test
    fun aPillWithCardsKeepsItsSwipeFeedback() {
        assertFalse(SwipeFeel.collapsedPillFrozen(hasCards = true, widgetOnPill = false))
    }

    @Test
    fun anIdleWidgetPillKeepsItsSwipeFeedback() {
        assertFalse(SwipeFeel.collapsedPillFrozen(hasCards = false, widgetOnPill = true))
        assertFalse(SwipeFeel.collapsedPillFrozen(hasCards = true, widgetOnPill = true))
    }
}