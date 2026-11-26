/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jero009.essentialisland.R
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.data.SmartIslandSettingsRepository
import com.jero009.essentialisland.ui.search.searchAnchor
import com.jero009.essentialisland.ui.components.NothingSegmented
import com.jero009.essentialisland.util.GestureSensitivity
import kotlinx.coroutines.launch

/** Low / Normal / High selector for how easily swipes fire. Sits at the top of the Gestures screen. */
@Composable
fun GestureSensitivityRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val selected = GestureSensitivity.fromId(settings.gestureSensitivity).id
    val options = listOf(
        GestureSensitivity.ID_LOW to stringResource(R.string.gesture_sensitivity_low),
        GestureSensitivity.ID_NORMAL to stringResource(R.string.gesture_sensitivity_normal),
        GestureSensitivity.ID_HIGH to stringResource(R.string.gesture_sensitivity_high)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchAnchor("gestures.sensitivity")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.gesture_sensitivity_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.gesture_sensitivity_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Shared Nothing OS 5 segment control: greyscale inversion + a gliding selected pill.
        NothingSegmented(
            options = options.map { it.first },
            selected = selected,
            label = { id -> options.first { it.first == id }.second },
            onSelected = { id ->
                scope.launch { repository.setGestureSensitivity(id) }
            }
        )
    }
}
