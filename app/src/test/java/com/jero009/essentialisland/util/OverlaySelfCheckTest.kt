/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlaySelfCheckTest {

    @Test
    fun `window is lost only when bound, enabled and detached`() {
        assertTrue(OverlaySelfCheck.windowLost(systemConnected = true, masterEnabled = true, windowAttached = false))
        assertFalse(OverlaySelfCheck.windowLost(systemConnected = false, masterEnabled = true, windowAttached = false))
        assertFalse(OverlaySelfCheck.windowLost(systemConnected = true, masterEnabled = false, windowAttached = false))
        assertFalse(OverlaySelfCheck.windowLost(systemConnected = true, masterEnabled = true, windowAttached = true))
    }

    @Test
    fun `retry delay doubles then settles on the slow cadence`() {
        assertEquals(OverlaySelfCheck.FIRST_CHECK_MS, OverlaySelfCheck.retryDelayMs(0))
        assertEquals(OverlaySelfCheck.FIRST_CHECK_MS * 2, OverlaySelfCheck.retryDelayMs(1))
        assertEquals(OverlaySelfCheck.FIRST_CHECK_MS * 4, OverlaySelfCheck.retryDelayMs(2))
        assertEquals(OverlaySelfCheck.SLOW_RETRY_MS, OverlaySelfCheck.retryDelayMs(OverlaySelfCheck.MAX_FAST_ATTEMPTS))
        assertEquals(OverlaySelfCheck.SLOW_RETRY_MS, OverlaySelfCheck.retryDelayMs(100))
    }
}
