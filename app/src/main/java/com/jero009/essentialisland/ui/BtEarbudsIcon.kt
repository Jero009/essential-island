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
 * Two-pod earbuds glyph, drawn to match the rounded headphones glyph: a circular head per pod with a
 * short stem below. Tinted like any other vector icon.
 */
val EarbudsGlyph: ImageVector = ImageVector.Builder(
    name = "EarbudsGlyph",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    // left pod head
    path(fill = SolidColor(Color.Black), stroke = null) {
        moveTo(11.3f, 8.6f)
        arcTo(3.0f, 3.0f, 0f, false, true, 5.3f, 8.6f)
        arcTo(3.0f, 3.0f, 0f, false, true, 11.3f, 8.6f)
        close()
    }
    // right pod head
    path(fill = SolidColor(Color.Black), stroke = null) {
        moveTo(18.7f, 8.6f)
        arcTo(3.0f, 3.0f, 0f, false, true, 12.7f, 8.6f)
        arcTo(3.0f, 3.0f, 0f, false, true, 18.7f, 8.6f)
        close()
    }
    // left stem
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.6f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(8.3f, 11.4f)
        lineTo(8.3f, 17.8f)
    }
    // right stem
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.6f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(15.7f, 11.4f)
        lineTo(15.7f, 17.8f)
    }
}.build()
