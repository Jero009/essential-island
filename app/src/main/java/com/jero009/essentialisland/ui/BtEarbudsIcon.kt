/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Two-pod earbuds glyph: rounded heads tilted toward the middle, thick stems flaring outward at the
 * bottom — the V silhouette of TWS earbuds. Tinted like any other vector icon.
 */
val EarbudsGlyph: ImageVector = ImageVector.Builder(
    name = "EarbudsGlyph",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    // left pod head (tilted toward the centre)
    path(fill = SolidColor(Color.Black), stroke = null) {
        moveTo(12.4f, 8.4f)
        arcTo(3.5f, 3.5f, 0f, false, true, 5.4f, 8.4f)
        arcTo(3.5f, 3.5f, 0f, false, true, 12.4f, 8.4f)
        close()
    }
    // right pod head
    path(fill = SolidColor(Color.Black), stroke = null) {
        moveTo(18.6f, 8.4f)
        arcTo(3.5f, 3.5f, 0f, false, true, 11.6f, 8.4f)
        arcTo(3.5f, 3.5f, 0f, false, true, 18.6f, 8.4f)
        close()
    }
    // stems flare outward from under each head, thick with round caps
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 3.4f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(8.9f, 11.2f)
        lineTo(7.2f, 17.8f)
    }
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 3.4f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(15.1f, 11.2f)
        lineTo(16.8f, 17.8f)
    }
}.build()
