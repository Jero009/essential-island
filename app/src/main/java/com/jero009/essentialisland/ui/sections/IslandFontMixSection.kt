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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.model.FontMix
import com.jero009.essentialisland.model.FontSlot
import com.jero009.essentialisland.ui.IslandFontOptions
import com.jero009.essentialisland.ui.islandFontFamily
import com.jero009.essentialisland.util.CustomFonts

/** The two font roles the island exposes; each maps onto the underlying FontMix slots (model/FontMix.kt). */
private enum class FontRole(val title: String, val note: String) {
    TEXT("Text font", "Everything the island reads: titles' body, notifications, widgets"),
    TITLE("Title font", "Headlines and emphasis inside the island")
}

/**
 * Simple font settings: exactly two pickers — Text font and Title font. Both apply to all text
 * (the underlying Light/Regular slots share the text font; the Medium slot is the title font).
 */
@Composable
internal fun IslandFontMixSection(
    settings: SmartIslandSettings,
    onChange: (enabled: Boolean, light: String, regular: String, medium: String) -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<FontRole?>(null) }

    fun idFor(slot: FontSlot) = FontMix.idOf(slot, settings.fontMixLight, settings.fontMixRegular, settings.fontMixMedium)

    fun update(role: FontRole, id: String) {
        when (role) {
            FontRole.TEXT -> onChange(true, id, id, idFor(FontSlot.Medium))
            FontRole.TITLE -> onChange(true, idFor(FontSlot.Light), idFor(FontSlot.Regular), id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Fonts",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FontRole.entries.forEach { role ->
            val slot = when (role) {
                FontRole.TEXT -> FontSlot.Light
                FontRole.TITLE -> FontSlot.Medium
            }
            val id = idFor(slot)
            val effective = FontMix.effectiveId(id, settings.islandFont)
            val family = remember(effective) { islandFontFamily(context, effective) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = role }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = role.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = role.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // The sample is drawn with the role's own font at the weight the island asks for it.
                    Text(
                        text = fontName(id),
                        fontFamily = family,
                        fontWeight = FontWeight(slot.representativeWeight),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    editing?.let { role ->
        val slot = when (role) {
            FontRole.TEXT -> FontSlot.Light
            FontRole.TITLE -> FontSlot.Medium
        }
        val current = idFor(slot)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(role.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    IslandFontPicker(
                        selected = current,
                        onSelected = { update(role, it) },
                        title = role.title,
                        description = role.note,
                        inheritRow = "Same as the app font" to "Follows the island's main font setting"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = null }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun fontName(id: String): String {
    if (id.isEmpty()) return "Same as the app font"
    IslandFontOptions.firstOrNull { it.id == id }?.let { return it.label }
    return CustomFonts.fileNameOf(id)?.let { CustomFonts.labelOf(it) } ?: "Same as the app font"
}
