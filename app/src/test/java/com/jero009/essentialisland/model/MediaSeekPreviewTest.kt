/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaSeekPreviewTest {

    @Test
    fun noDragMeansNoPreview() {
        assertNull(MediaSeekPreview.elapsedMs(MediaSeekPreview.NONE, 200_000L))
    }

    @Test
    fun streamsWithoutLengthNeverPreview() {
        // The bar cannot seek on a live stream, so a thumb fraction must not show a time.
        assertNull(MediaSeekPreview.elapsedMs(0.5f, null))
        assertNull(MediaSeekPreview.elapsedMs(0.5f, 0L))
        assertNull(MediaSeekPreview.elapsedMs(0.5f, -1L))
    }

    @Test
    fun previewFollowsTheThumbFraction() {
        assertEquals(0L, MediaSeekPreview.elapsedMs(0f, 200_000L))
        assertEquals(100_000L, MediaSeekPreview.elapsedMs(0.5f, 200_000L))
        assertEquals(200_000L, MediaSeekPreview.elapsedMs(1f, 200_000L))
    }

    @Test
    fun fractionsAreClampedBeforeScaling() {
        assertEquals(0L, MediaSeekPreview.elapsedMs(-0.4f, 200_000L))
        assertEquals(200_000L, MediaSeekPreview.elapsedMs(1.4f, 200_000L))
    }
}
