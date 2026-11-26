/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU General Public License v3 - see LICENSE
 */

package com.jero009.essentialisland.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jero009.essentialisland.ui.components.NothingSegmented

/** Which orientation profile the Home and Position tabs are editing. */
enum class OrientationTab(val label: String) {
    Portrait("Portrait"),
    Landscape("Landscape")
}

/** Segmented Portrait / Landscape switch shown at the top of the Home and Position tabs. */
@Composable
fun OrientationTabs(
    selected: OrientationTab,
    onSelected: (OrientationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // The shared Nothing OS 5 segment control: recessed container, greyscale inversion, and a
    // selected pill that glides between the segments (~200 ms mechanical ease) instead of flipping.
    NothingSegmented(
        options = OrientationTab.entries.toList(),
        selected = selected,
        label = { it.label },
        onSelected = onSelected,
        modifier = modifier
    )
}
