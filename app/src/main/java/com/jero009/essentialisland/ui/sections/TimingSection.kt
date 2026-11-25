/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jero009.essentialisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jero009.essentialisland.R
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.data.SmartIslandSettingsRepository
import com.jero009.essentialisland.ui.SliderSettingItem
import com.jero009.essentialisland.ui.bounceClick
import com.jero009.essentialisland.ui.components.CollapsibleCard
import com.jero009.essentialisland.ui.search.searchAnchor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** "7.5 s" style text for a value in milliseconds. Whole seconds drop the decimal. */
internal fun formatSeconds(ms: Float): String {
    val tenths = (ms / 100f).roundToInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) "$whole s" else "$whole.$frac s"
}

internal fun formatMinutes(minutes: Float): String {
    val m = minutes.roundToInt()
    return if (m <= 0) "Never" else "$m min"
}

/** Rounds [value] to the nearest multiple of [step] and keeps it inside [min]..[max]. */
internal fun snapToStep(value: Float, step: Float, min: Float, max: Float): Float =
    ((value / step).roundToInt() * step).coerceIn(min, max)

/** One named combination of all five timing sliders. */
internal data class TimingValues(
    val idleTimeoutMs: Int,
    val batteryWarningMs: Int,
    val popupMs: Int,
    val secondaryMergeMs: Int,
    val transferExpiryMinutes: Int
)

/**
 * The timing pace presets. Normal is the shipped default; a preset applies all five sliders at once and the
 * individual sliders stay available under "Individual timers".
 */
internal enum class TimingPreset(val values: TimingValues) {
    Snappy(TimingValues(3_000, 2_500, 1_200, 1_200, 10)),
    Normal(TimingValues(5_000, 4_000, 1_800, 1_800, 15)),
    Relaxed(TimingValues(8_000, 6_000, 2_600, 2_600, 30));

    companion object {
        fun from(settings: SmartIslandSettings): TimingValues = TimingValues(
            settings.idleTimeoutMs,
            settings.batteryWarningMs,
            settings.popupMs,
            settings.secondaryMergeMs,
            settings.transferExpiryMinutes
        )

        /** The preset whose five values all match, or null when the timings are custom. */
        fun match(values: TimingValues): TimingPreset? = entries.firstOrNull { it.values == values }
    }
}

/**
 * Settings > Timing: a pace preset, then the individual timers folded away. Sliders keep a local draft while
 * dragging and write to the settings once, when the finger lifts (or a +/- button is pressed).
 */
@Composable
fun TimingSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val neverText = stringResource(R.string.timing_never)
    val selectedPreset = TimingPreset.match(TimingPreset.from(settings))

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.timing_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        text = stringResource(R.string.timing_presets_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            TimingPreset.Snappy to R.string.anim_speed_snappy,
                            TimingPreset.Normal to R.string.anim_speed_normal,
                            TimingPreset.Relaxed to R.string.anim_speed_relaxed
                        ).forEach { (preset, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedPreset == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .bounceClick {
                                        scope.launch {
                                            repository.setIdleTimeoutMs(preset.values.idleTimeoutMs)
                                            repository.setBatteryWarningMs(preset.values.batteryWarningMs)
                                            repository.setPopupMs(preset.values.popupMs)
                                            repository.setSecondaryMergeMs(preset.values.secondaryMergeMs)
                                            repository.setTransferExpiryMinutes(preset.values.transferExpiryMinutes)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(label),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedPreset == preset) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedPreset == preset) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                CollapsibleCard(
                    title = stringResource(R.string.timing_advanced_title),
                    stateKey = "timing_advanced",
                    summary = stringResource(R.string.timing_advanced_summary),
                    framed = false,
                    searchKeyPrefix = "timing."
                ) {
                    TimingSlider(
                        searchKey = "timing.idle",
                        label = stringResource(R.string.timing_idle_label),
                        hint = stringResource(R.string.timing_idle_hint),
                        stored = settings.idleTimeoutMs,
                        min = SmartIslandSettings.MIN_IDLE_TIMEOUT_MS,
                        max = SmartIslandSettings.MAX_IDLE_TIMEOUT_MS,
                        step = 500,
                        onCommit = { scope.launch { repository.setIdleTimeoutMs(it) } },
                        valueText = { ms ->
                            if (ms.roundToInt() >= SmartIslandSettings.IDLE_TIMEOUT_NEVER_MS) neverText else formatSeconds(ms)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TimingSlider(
                        searchKey = "timing.battery",
                        label = stringResource(R.string.timing_battery_label),
                        hint = stringResource(R.string.timing_battery_hint),
                        stored = settings.batteryWarningMs,
                        min = SmartIslandSettings.MIN_BATTERY_WARNING_MS,
                        max = SmartIslandSettings.MAX_BATTERY_WARNING_MS,
                        step = 500,
                        onCommit = { scope.launch { repository.setBatteryWarningMs(it) } }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TimingSlider(
                        searchKey = "timing.popup",
                        label = stringResource(R.string.timing_popup_label),
                        hint = stringResource(R.string.timing_popup_hint),
                        stored = settings.popupMs,
                        min = SmartIslandSettings.MIN_POPUP_MS,
                        max = SmartIslandSettings.MAX_POPUP_MS,
                        step = 100,
                        onCommit = { scope.launch { repository.setPopupMs(it) } }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TimingSlider(
                        searchKey = "timing.merge",
                        label = stringResource(R.string.timing_merge_label),
                        hint = stringResource(R.string.timing_merge_hint),
                        stored = settings.secondaryMergeMs,
                        min = SmartIslandSettings.MIN_SECONDARY_MERGE_MS,
                        max = SmartIslandSettings.MAX_SECONDARY_MERGE_MS,
                        step = 100,
                        onCommit = { scope.launch { repository.setSecondaryMergeMs(it) } }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    TimingSlider(
                        searchKey = "timing.transfer",
                        label = stringResource(R.string.timing_transfer_label),
                        hint = stringResource(R.string.timing_transfer_hint),
                        stored = settings.transferExpiryMinutes,
                        min = 0,
                        max = SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES,
                        step = 5,
                        valueText = ::formatMinutes,
                        onCommit = { scope.launch { repository.setTransferExpiryMinutes(it) } }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { scope.launch { repository.resetTimings() } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.timing_reset))
        }
    }
}

@Composable
private fun TimingSlider(
    searchKey: String,
    label: String,
    hint: String,
    stored: Int,
    min: Int,
    max: Int,
    step: Int,
    onCommit: (Int) -> Unit,
    valueText: (Float) -> String = ::formatSeconds
) {
    // Local draft so dragging is smooth and only the final value is written. It follows the
    // stored value again whenever that changes (reset, restore from backup).
    var draft by remember(stored) { mutableFloatStateOf(stored.toFloat()) }
    Column(modifier = Modifier.searchAnchor(searchKey)) {
        SliderSettingItem(
            label = label,
            value = draft,
            range = min.toFloat()..max.toFloat(),
            step = step.toFloat(),
            valueText = valueText,
            onValueChange = { draft = snapToStep(it, step.toFloat(), min.toFloat(), max.toFloat()) },
            onValueChangeFinished = { onCommit(draft.roundToInt()) }
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
