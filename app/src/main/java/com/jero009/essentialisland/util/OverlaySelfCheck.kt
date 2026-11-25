/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

/**
 * Decision table for the overlay's bound-but-windowless self-check.
 *
 * After Android kills the process (for example when a runtime permission is revoked), the system
 * re-binds the accessibility service — but the re-added pill window can still fail silently on
 * some OEM builds, or the system may not re-bind the service at all. The service runs this check
 * after every session start: while the island is switched on and the service is bound, a missing
 * pill window is always a fault, because every intentional hide (lock screen, landscape, exempt
 * apps) only flips the view's visibility and never removes the window.
 */
object OverlaySelfCheck {
    /** First verification that the window actually appeared after a session start. */
    const val FIRST_CHECK_MS = 2_000L

    /** Re-add attempts that follow the doubling backoff before switching to slow retries. */
    const val MAX_FAST_ATTEMPTS = 3

    /** Steady retry cadence once the fast backoff is exhausted — one cheap main-thread tick. */
    const val SLOW_RETRY_MS = 30_000L

    fun windowLost(systemConnected: Boolean, masterEnabled: Boolean, windowAttached: Boolean): Boolean =
        systemConnected && masterEnabled && !windowAttached

    /** Retry delay for the nth failed re-add (0-based): 2s, 4s, 8s, then every 30s. */
    fun retryDelayMs(attempt: Int): Long =
        if (attempt < MAX_FAST_ATTEMPTS) FIRST_CHECK_MS shl attempt else SLOW_RETRY_MS
}
