/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.model

/**
 * What the elapsed-time label shows while the user drags the media seek bar: the thumb's
 * position, not the ticking playback position (the same pattern MissingCore/Music uses).
 */
object MediaSeekPreview {

    /** Sentinel fraction for "no drag in progress". */
    const val NONE = Float.NaN

    /**
     * Milliseconds to display for a drag-preview [fraction]; `null` when there is no live
     * preview (no drag, or a stream whose length is unknown — the bar cannot seek there).
     */
    fun elapsedMs(fraction: Float, durationMs: Long?): Long? {
        if (fraction.isNaN()) return null
        if (durationMs == null || durationMs <= 0L) return null
        return (fraction.coerceIn(0f, 1f) * durationMs).toLong()
    }
}
