/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU General Public License v3 - see LICENSE
 */

package com.jero009.essentialisland.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Nothing OS 5 segmented control, shared by every single-choice segment group in the app.
 *
 * Measured grammar: recessed tile container, the selected pill one grey step up (#141414 /
 * #303030 dark), greyscale text — never coloured, active state is an inversion step, not a hue.
 * The selected pill is ONE thumb that glides between the segments (~200 ms, standard mechanical
 * ease, transform only) per the nothing-os-design spec's sliding-thumb rule — the fill never
 * flips between two static backgrounds.
 */
@Composable
fun <T> NothingSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val segmentWidth = maxWidth / options.size
            val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
            val thumbX by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                ),
                label = "segmentThumb"
            )
            // The gliding thumb, drawn behind all segments.
            Box(
                modifier = Modifier
                    .offset(x = thumbX)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.19f))
            )
            Row(modifier = Modifier.matchParentSize()) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelected(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
